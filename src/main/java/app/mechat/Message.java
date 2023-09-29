package app.mechat;

public class Message {
    private String text, sender, timestamp;

    Message(String _text, String _sender, String _timestamp) {
        this.text = _text;
        this.sender = _sender;
        this.timestamp = _timestamp;
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

}
