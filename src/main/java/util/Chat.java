package util;

import java.util.ArrayList;

public class Chat {

    private ArrayList<Message> messages;
    private String sender, receiver;
    private boolean active;
    private int conversation_id;

    public Chat(ArrayList<Message> _messages, String _sender, String _receiver, int _conversation_id) {
        this.messages = _messages;
        this.sender = _sender;
        this.receiver = _receiver;
        this.conversation_id = _conversation_id;
        this.active = false;
    }

    // getters and setters
    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String _sender) {
        sender = _sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String _receiver) {
        receiver = _receiver;
    }

    public boolean isActive() { return active; }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getConversation_id() { return conversation_id; }

    public void setConversation_id(int id) { this.conversation_id = id; }
}
