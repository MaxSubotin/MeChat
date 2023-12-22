package util;

;

public class SystemMessage extends Message {

    public SystemMessageType type;

    public SystemMessage(String _text, String _sender, String _timestamp, String _chatId, SystemMessageType type) {
        super(_text, _sender, _timestamp, _chatId);
        setType(type);
    }

    public void setType(SystemMessageType type) { this.type = type; }
    public SystemMessageType getType() { return type; }

}
