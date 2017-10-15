import java.io.Serializable;

public class EventRecord implements Serializable {
	public enum Operation { TWEET, BLOCK, UNBLOCK };

	int timestamp;
	Operation operation;
	String username; //user that does the thing
	String content; //either: user that is blocked, or tweet text
	long realtime;
	Integer id;
}