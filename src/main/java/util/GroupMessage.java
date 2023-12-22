package util;

import java.util.ArrayList;

public class GroupMessage extends Message {

    private ArrayList<String> receivers;

    public GroupMessage(String _text, String _sender, String _timestamp, String _chatId, ArrayList<String> _receivers) {
        super(_text, _sender, _timestamp, _chatId);
        receivers = (_receivers != null) ? _receivers : new ArrayList<>();
    }

    public ArrayList<String> getReceivers() {
        return receivers;
    }
    public void setReceivers(ArrayList<String> receivers) {
        this.receivers = receivers;
    }
    public void addReceiver(String receiver) { this.receivers.add(receiver); }
}
