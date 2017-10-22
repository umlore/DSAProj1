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

	Socket[] hosts; //receives

	Integer id;
	String username; 
 
	ArrayList<ArrayList<Integer>> tsMatrix;
	ArrayList<EventRecord> log;

	Dictionary<String, ArrayList<String>> blockList;

	Thread acceptThread;
	Boolean acceptThreadActive;
	
	ExecutorService threadPool;
	
	public WuuInstance(Integer portNumber, String name, Integer n) {
		username = name;
		port = portNumber;	

		addressBook = new HashMap<String, Integer>();	

		hosts = new Socket[n];

		acceptThread = null;
		acceptThreadActive = false;

		log = new ArrayList<EventRecord>();

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
		return (eR.timestamp <= tsMatrix.get(k).get(eR.id));
	}

	public ArrayList<EventRecord> getLogDiff(int proc) { //TODO
		ArrayList<EventRecord> partialLog =  new ArrayList<EventRecord>();
		for (int i = 0; i < log.size(); i++) {
			if (!hasRecord(log.get(i), proc)) {
				partialLog.add(log.get(i));
			}
		}
		return partialLog;
	}

	//called whenever a new event is created by the user, will trigger
	//send message in the case that eventRecord is of type tweet
	public void createEvent(EventRecord eventRecord) {
		tsMatrix.get(id).set(id, tsMatrix.get(id).get(id) + 1);
		log.add(eventRecord);

		if (eventRecord.operation == EventRecord.Operation.TWEET) {
			sendMessage();
		}
	}
	
	public void listen() {
		System.out.println("Listening on port " + port);
		try (
			ServerSocket serverSocket = new ServerSocket(port);
		) {					
			socket = serverSocket;
			
			while (true) {

				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (Exception e) { System.out.println(e.getMessage()); }
				acceptConnect();
			}
		} catch (IOException e) {
			System.out.println("Exception caught listening for a connection to server on port " + port);
			System.out.println(e.getMessage());
		}
	}

	public void view() {
		for (int i = 0; i < log.size(); i++) {
			EventRecord tweet = log.get(i);
			if (tweet.operation != EventRecord.Operation.TWEET) { continue; }
			if (blockList.get(tweet.username).contains(username)) {
				continue;
			}
			System.out.println("@" + tweet.username + "\t" + tweet.realtime);
			System.out.println(tweet.content + "\n");
			//TODO: Don't view if blocked!

		}
	}
	
	public void acceptConnect() throws IOException {


        // create an open ended thread-pool
        // wait for a client to connect
		try { 
			
			if (acceptThread == null) {
				Socket clientSocket = socket.accept();
				
				//Integer clientID = wu.addressBook.get(clientSocket.getInetAddress().getHostAddress());
				//System.out.println("clientID: " + clientID);
				
				
				acceptThread = new AcceptClients(clientSocket);
				acceptThread.start();
				
				
				
			}
			if (acceptThread != null) {
				if (!acceptThreadActive) {
					acceptThread = null;
					acceptConnect();
				}
			}
		}
		catch (Exception e) {

		}
	}

	//sends a message to all unblocked sites consisting of the partial log
	//local to this instance which this doesn't know the receiving
	//instance knows about
	public void sendMessage() { 
		for (int i = 0; i < hosts.length; i++) {
			if (hosts[i] == null) { continue; }
			Message message = new Message(getLogDiff(i), tsMatrix, id);
			
			System.out.println("Message: ");
			message.printMessage();
			//byte[] byteMessage = message.toBytes();
			try {
				//s.writeInt(byteMessage.length);
				//s.write(byteMessage, 0, byteMessage.length);
				ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(hosts[i].getOutputStream()));
				out.writeObject(message);
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
	}

	public void updateBlocklist(Message message) {
		for (int i = 0; i < message.log.size(); i++) {
			EventRecord eRMessage = message.log.get(i);
			if (eRMessage.operation == EventRecord.Operation.BLOCK) {
				ArrayList<String> blockedUsers = blockList.get(eRMessage.username);
				blockedUsers.add(eRMessage.content);
			} else if (eRMessage.operation == EventRecord.Operation.UNBLOCK) {
				ArrayList<String> blockedUsers = blockList.get(eRMessage.username);
				blockedUsers.remove(eRMessage.content);
			}
		}
	}

	public void updateLog(Message message) {
		for (int i = 0; i < message.log.size(); i++) {
			EventRecord eRMessage = message.log.get(i);
			if (!hasRecord(eRMessage, id)) {
				log.add(eRMessage);
			}
		}
	}

	public void truncateLog() {
		for (int i = log.size() - 1; i >= 0; i--) {
			Boolean allHasRec = true;
			EventRecord eRToTrunc = log.get(i);
			if (eRToTrunc.operation == EventRecord.Operation.TWEET) { continue; }
			for (int j = 0; j < tsMatrix.size(); j++) {
				if (!hasRecord(eRToTrunc, j)) {
					allHasRec = false;
				}
			}
			if (allHasRec) {
				log.remove(i);
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
		System.out.println("Hostport: " + hostPort + "\thostID: " + hostID + "\tHostaddress: " + host);
		try (
			Socket hostSocket = new Socket(host, hostPort);
			Socket clientSocket = new Socket();
		) {
			System.out.println("\t\t\tHostSocket: " + hostSocket);
			if (hostSocket != null) {
				//PrintWriter sendToHost = new PrintWriter(hostSocket.getOutputStream(), true);
				//BufferedReader receiveFromHost = new BufferedReader(new InputStreamReader(hostSocket.getInputStream()));
				//sendToHost.println("Client port " + port + " connected to host " + hostPort + ".");
				hosts[hostID] = hostSocket;
				hosts[hostID].setKeepAlive(true);
				sendMessage();
				sendMessage();
				System.out.println("Connected self to host " + host);
			}
			else {
				System.out.println("Host is null");
			}
			
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + host);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static class AcceptClients extends Thread {
		ServerSocket server;
		Socket client;

		public AcceptClients(Socket clientSocket) {
			client = clientSocket;
		}
		
	    public void run() {
				
				try {
					//System.out.println("Waiting to receive: ");
					
					//System.out.println("Received Connection");

					//todo: fix this
					/*if (wu.hosts[clientID] == null) {
						System.out.println("\t\t\t\t\t\t\t\tGOT HERE");
						TimeUnit.SECONDS.sleep(1);
						wu.connectTo(clientSocket.getInetAddress().getHostAddress() , clientID + 7000, clientID);
					}*/
						ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(client.getInputStream()));
						System.out.println("Receiving...");
						String line = "";
						while(true) {
							Message m = (Message) in.readObject();
						    m.printMessage();
						}
						
							
					//wu.acceptThreadActive = false;
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
	    }
	}
}