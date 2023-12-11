package util;

import database.Database;
import websockets.CustomWebSocketClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;

public class RegularChat implements ChatMethods {

    private ArrayList<Message> messages;
    private String sender, receiver; // these are user id, not username
    private boolean active; // delete this?
    private int conversation_id;

    public RegularChat(ArrayList<Message> _messages, String _sender, String _receiver, int _conversation_id) {
        if (_messages == null)
            this.messages = new ArrayList<>();
        else
            this.messages = _messages;
        this.sender = _sender;
        this.receiver = _receiver;
        this.conversation_id = _conversation_id;
        this.active = false;
    }

    @Override
    public boolean sendMessage(CustomWebSocketClient sender, String text) {
        // Get current timestamp
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedTime = currentTime.format(formatter);

        // Create a message object and the controller to access methods
        Message message = new Message(text, getSender(), getReceiver(), formattedTime, Integer.toString(getConversation_id()));

        try {
            // Adding the new message to the array and the database
            if (Database.addMessageToDatabase(message, getConversation_id())) {

                // Turn Message object into json format
                String jsonMessage = sender.gson.toJson(message);

                // Send the message in json format to the chat server where it will be sent to the correct user
                sender.send(jsonMessage);
            }
            else return false;
        }
        catch (Exception e) {
            return false;
        }

        return true;
    }

    public boolean handleMessageDeleted(String text, String timestamp) {
        for (Message message: messages) {
            if (Objects.equals(message.getText(), text) &&
                    Objects.equals(message.getTimestamp(), timestamp)) {
                message.setText("--< this message was deleted >--");
                return true;
            }
        }
        return false;
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
