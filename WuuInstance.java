import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;
import java.util.concurrent.*;

public class WuuInstance {
	ServerSocket socket;
	Integer port;
	Boolean listening;
	Boolean newCommand;

	HashMap<String, Integer> addressBook;

	Socket[] clients; //sends
	DataInputStream[] inStreams;
	Socket[] hosts; //receives
	DataOutputStream[] outStreams;

	Integer id;
	String username; 
 
	ArrayList<ArrayList<Integer>> tsMatrix;
	ArrayList<EventRecord> log;

	Dictionary<Integer, ArrayList<Integer>> blockList;

	Thread acceptThread;
	Boolean acceptThreadActive;
	
	ExecutorService threadPool;
	
	public WuuInstance(Integer portNumber, String name, Integer n) {
		username = name;
		port = portNumber;	

		addressBook = new HashMap<String, Integer>();	

		clients = new Socket[n];
		inStreams = new DataInputStream[n];
		hosts = new Socket[n];
		outStreams = new DataOutputStream[n];

		acceptThread = null;
		acceptThreadActive = false;

		threadPool = Executors.newCachedThreadPool();
		listening = false;
		tsMatrix = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < n; i++) {
			ArrayList<Integer> timevec = new ArrayList<Integer>();
			for (int j = 0; j < n; j++) {
				timevec.add(0);
			}
			tsMatrix.add(timevec);
		}
	}

	public void setAddressBook(HashMap<String, Integer> adb) {
		addressBook = adb;
	}
	
	public String getHostName() {
		return socket.getInetAddress().getHostName();
	}

	public Boolean hasRecord(EventRecord eR, int k) {
		return false;
	}
	
	public void listen() {
		System.out.println("Listening on port " + port);
		try (
			ServerSocket serverSocket = new ServerSocket(port);
		) {					
			socket = serverSocket;
			
			while (true) {
				//TODO: Only call when necessary
				sendMessage();
				receiveMessages();
				acceptConnect();
			}
		} catch (IOException e) {
			System.out.println("Exception caught listening for a connection to server on port " + port);
			System.out.println(e.getMessage());
		}
	}
	
	public void acceptConnect() throws IOException {


        // create an open ended thread-pool
        // wait for a client to connect
        System.out.println("AcceptThread: " + acceptThread);
		try { 
			
			if (acceptThread == null) {
				acceptThread = new AcceptClients(this);
				acceptThreadActive = true;
				threadPool.submit(acceptThread);
			}
			if (acceptThread != null) {
				if (!acceptThreadActive) {
					acceptThread = null;
				}
			}
		}
		catch (Exception e) {

		}
	}

	public ArrayList<EventRecord> getLogDiff(int proc) { //TODO
		return new ArrayList<EventRecord>();
	}

	public void printHostsAndClients() {
		System.out.println("Hosts and Clients: ");
		for (int i = 0; i < hosts.length; i++) {
			System.out.println("\t  Hosts["+i+"]: " + hosts[i]);
			System.out.println("\tClients["+i+"]: " + clients[i]);
		}
	}

	public void sendMessage() { //TODO:ESU
		//printHostsAndClients();
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (Exception e) { System.out.println(e.getMessage()); }
		

		for (int i = 0; i < hosts.length; i++) {
			if (hosts[i] == null) { continue; }
			Message message = new Message(getLogDiff(i), tsMatrix, id);
			byte[] byteMessage = message.toBytes();
			try {
				outStreams[i].writeInt(byteMessage.length);
				outStreams[i].write(byteMessage);
			}
			catch (IOException e) {
				//TODO: SOCKET IS CLOSED, CANNOT SEND
				System.out.println(e.getMessage());
			}
		}
	}
	
	public void receiveMessages() {
		for (int i = 0; i < clients.length; i++) {
			try {
				if (clients[i] != null) {
					if (inStreams[i].available() != 0) {
						int length = inStreams[i].readInt();
						byte[] byteMessage = new byte[length];
						inStreams[i].readFully(byteMessage, 0, byteMessage.length);

						Message message = Message.fromBytes(byteMessage);
						message.printMessage();
						updateMatrix(message.log, message.id, message.tsMatrix);
						//TODO: Do something with message, now that it's received
					}

				}
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	public void updateMatrix(ArrayList<EventRecord> clientLog, Integer clientid, ArrayList<ArrayList<Integer>> clientMatrix) {
		for (int i = 0; i < tsMatrix.size(); i++) {
			ArrayList<Integer> row = tsMatrix.get(id);
			row.set(i, Math.max(tsMatrix.get(id).get(i), clientMatrix.get(clientid).get(i) ) );
			tsMatrix.set(id, row);
		}
		for (int i = 0; i < tsMatrix.size(); i++) {
			for (int j = 0; j < tsMatrix.size(); j++) {
				ArrayList<Integer> row = tsMatrix.get(i);
				row.set(j, Math.max(tsMatrix.get(i).get(j), clientMatrix.get(i).get(j) ) );
				tsMatrix.set(id, row);
			}
		}
		
		for (int i = 0; i < tsMatrix.size(); i++) {
			System.out.print("[\t");
			for (int j = 0; j < tsMatrix.get(i).size(); j++) {
				System.out.print(tsMatrix.get(i).get(j) + "\t");
			}
			System.out.print("]\n");
		}
		System.out.println();
		for (int i = 0; i < log.size(); i++) {
			System.out.println("Event");
		}
		
	}
	
	public void connectTo(String host, Integer hostPort, Integer hostID) {
		System.out.println("Connecting to host " + host);
		try (
			Socket hostSocket = new Socket(host, hostPort);
			Socket clientSocket = new Socket();
		) {
			if (hostSocket != null) {
				hostSocket.setKeepAlive(true);
				//PrintWriter sendToHost = new PrintWriter(hostSocket.getOutputStream(), true);
				//BufferedReader receiveFromHost = new BufferedReader(new InputStreamReader(hostSocket.getInputStream()));
				//sendToHost.println("Client port " + port + " connected to host " + hostPort + ".");
				hosts[hostID] = hostSocket;
				outStreams[hostID] = new DataOutputStream(hosts[hostID].getOutputStream());
				System.out.println("Connected self to host " + host);
			}
			else {
				System.out.println("Message fell on deaf ears....");
			}
			
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + host);
		} catch (IOException e) {
			System.out.println("Message fell on deaf ears....");
		}
	}
	
	public static class AcceptClients extends Thread {
		ServerSocket server;
		WuuInstance wu;

		public AcceptClients(WuuInstance w) {
			wu = w;
			server = w.socket;
		}
		
	    public void run() {
				try {
					System.out.println("Waiting to receive: ");
					Socket clientSocket = server.accept();
					Integer clientID = wu.addressBook.get(clientSocket.getInetAddress().getHostAddress());
					//System.out.println("clientID: " + clientID);
					wu.clients[clientID] = clientSocket;
					wu.inStreams[clientID] = new DataInputStream(wu.clients[clientID].getInputStream());
					System.out.println("Received Connection");
					wu.acceptThreadActive = false;
				} catch (IOException e) {
					System.out.println(e.getMessage());
				}
	    }
	}
}