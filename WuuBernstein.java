import java.io.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.*;

public class WuuBernstein {
	public static void main(String args[]) {
		Integer port = Integer.parseInt(args[0]);
		String username = args[1];
		HashMap<String, Integer> addressBook = new HashMap<String, Integer>();
		
		try (BufferedReader br = new BufferedReader(new FileReader("instances.config")) ) {
			WuuInstance wu = new WuuInstance(port, username, Integer.parseInt(br.readLine()));
			String line;
			Integer lineCount = 0;
			//Read the config file
			while ( (line = br.readLine()) != null ) {
				String[] tokens = line.split(" ");
				InetAddress clientIP = InetAddress.getByName(tokens[0]);
				Integer hostPort = Integer.parseInt(tokens[1]);
				//System.out.println("clientIP = " + clientIP);
				//System.out.println("Local Host " + InetAddress.getLocalHost());
				if ( !clientIP.equals(InetAddress.getLocalHost())) {
					wu.id = lineCount;
					//System.out.println("Will connect to " + clientIP);
					//Host name, host port
					wu.connectTo(clientIP.getHostAddress(), hostPort, lineCount);

				}
				//insert address into addressbook
				//System.out.println("Map In WuuBernstein: " + addressBook.toString());
				addressBook.put(clientIP.getHostAddress(), lineCount);

				lineCount++;
			}
			wu.setAddressBook(addressBook);
			wu.listen();
			//System.out.println("GOT HERE");
			wu.threadPool.shutdown();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
}