package app.mechat;

public class Message {
    public String text, sender, receiver, timestamp, conversation_id;
    public boolean isSystemMessage;

    Message(String _text, String _sender, String _receiver, String _timestamp, String _conversation_id) {
        this.text = _text;
        this.sender = _sender;
        this.receiver = _receiver;
        this.timestamp = _timestamp;
        this.conversation_id = _conversation_id;
        this.isSystemMessage = false;
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

    public void setIsSystemMessage(boolean systemMessage) {
        isSystemMessage = systemMessage;
    }

    public boolean getIsSystemMessage() {
        return isSystemMessage;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }
}
