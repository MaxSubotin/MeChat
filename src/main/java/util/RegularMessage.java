package util;

public class RegularMessage extends Message {

    private String receiver;

    public RegularMessage(String _text, String _sender, String _timestamp, String _chatId, String _receiver) {
        super(_text, _sender, _timestamp, _chatId);
        receiver = _receiver;
    }

    public String getReceiver() {
        return receiver;
    }
    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }
}
