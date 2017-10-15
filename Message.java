import java.io.*;
import java.util.ArrayList;

public class Message implements Serializable{
	ArrayList<EventRecord> log;
	ArrayList<ArrayList<Integer>> tsMatrix;
	Integer id;

	//does not deep copy
	public Message(ArrayList<EventRecord> l, ArrayList<ArrayList<Integer>> ts, Integer senderID) {
		log = l;
		tsMatrix = ts;
		id = senderID;
	}

	public byte[] toBytes() {
		byte[] bytes = {};
		try{
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bao);
			oos.writeObject(log);
			oos.close();
			bytes = bao.toByteArray();
			//String message = new String(bytes, "ISO-8859-1");
		}
		catch (IOException e) {
			System.out.println(e.getMessage());
		}
		return bytes;
	}

	public static Message fromBytes(byte[] bytes) {
		ByteArrayInputStream binStream = new ByteArrayInputStream(bytes);
		ObjectInput inStream = null;
		Message message = null;
		try {
			inStream = new ObjectInputStream(binStream);
			message = (Message)inStream.readObject();
		}
		catch (ClassNotFoundException e) {
			System.out.println(e.getMessage());
		}
		catch(IOException e) {
			System.out.println(e.getMessage());
		}
		return message;
	}

	public void printMessage() {
		if (tsMatrix.size() <= 0) {
			return;
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
}