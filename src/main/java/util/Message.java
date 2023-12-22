package util;

import java.sql.Timestamp;

public abstract class Message {
    public String text, sender, timestamp, chatId;

    public Message(String _text, String _sender, String _timestamp, String _chatId) {
        this.text = _text;
        this.sender = _sender;
        this.timestamp = _timestamp;
        this.chatId = _chatId;
    }

    // getters and setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public static String createTimestamp() { return new Timestamp(System.currentTimeMillis()).toString(); }

    public String getChatId() { return chatId; }

    public void setChatId(String chatId) { this.chatId = chatId; }
}
