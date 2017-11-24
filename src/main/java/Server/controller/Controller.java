package main.java.Server.controller;

import main.java.Server.model.Conversation;

public class Controller {
	private final Conversation conversation = new Conversation();

    /**
     * Appends the specified entry to the conversation.
     *
     * @param entry The entry to append.
     */
    public void appendEntry(String entry) {
        conversation.appendEntry(entry);
    }

    /**
     * @return All entries in the conversation, in the order they were entered.
     */
    public String[] getConversation() {
        return conversation.getConversation();
    }
}
