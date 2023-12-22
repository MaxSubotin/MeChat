package util;

import database.Database;
import websockets.CustomWebSocketClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;

public abstract class Chat  {
    protected int messageCount;
    protected String sender, chatId; // userId of the sender
    protected ArrayList<Message> messages;

    public Chat(ArrayList<Message> _messages, String _sender, String _chatId, int _messageCount) {
        if (_messages == null)
            this.messages = new ArrayList<>();
        else
            this.messages = _messages;
        this.sender = _sender;
        this.chatId = _chatId;
        this.messageCount = _messageCount;
    }

    public abstract boolean sendMessage(CustomWebSocketClient client, String text);

    public boolean deleteMessage(String text, String timestamp) {
        for (Message message: messages) {
            if (Objects.equals(message.getText(), text) &&
                    Objects.equals(message.getTimestamp(), timestamp)) {
                message.setText("--< this message was deleted >--");
                return true;
            }
        }
        return false;
    }

    protected String createTimestamp() {
        // Get current timestamp
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return currentTime.format(formatter);
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

    public String getChatId() { return chatId; }

    public void setChatId(String id) { this.chatId = id; }

    public int getMessageCount() { return messageCount; }

    public void setMessageCount(int count) { this.messageCount = count; }

    public void incrementMessageCount() { this.messageCount++; }
}
