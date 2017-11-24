package main.java.Server.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
 * Holds the entire conversation, including all messages from all clients. All methods are thread
 * safe.
 */
public class Conversation {
	
	
	private final List<Object> entries = Collections.synchronizedList(new ArrayList<>());
	 /**
     * Appends the specified entry to the conversation.
     *
     * @param entry The entry to append.
     */
	public void appendEntry(String entry) {
		entries.add(entry);
	}
	/**
     * @return All entries in the conversation, in the order they were entered.
     */
	public String[] getConversation() {
		return entries.toArray(new String[0]);
	}
}
