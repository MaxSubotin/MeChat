package util;

import database.Database;
import websockets.CustomWebSocketClient;

import java.util.ArrayList;

public class GroupChat extends Chat {

    // 🔸 maybe add a members counter to display the amount of members in the group chat
    private ArrayList<String> receivers;
    private String groupName, admin; // userId of the admin of the group, only he can add/remove users

    public GroupChat(ArrayList<Message> _messages, String _sender, ArrayList<String> _receivers, String _chatId, int _messageCount, String _groupName, String _admin) {
        super(_messages, _sender, _chatId, _messageCount);
        this.receivers = _receivers;
        this.groupName = _groupName;
        this.admin = _admin;
    }

    @Override
    public boolean sendMessage(CustomWebSocketClient client, String text) {
        // Create a message object and the controller to access methods
        GroupMessage message = new GroupMessage(text, getSender(), createTimestamp(), getChatId(), receivers);

        try {
            // Adding the new message to the array and the database
            if (Database.addGroupMessageToDatabase(message, getChatId())) {

                // Turn Message object into json format
                String jsonMessage = client.gson.toJson(message);

                // Send the message in json format to the chat server where it will be sent to the correct user
                client.send(jsonMessage);
            }
            else return false;
        }
        catch (Exception e) {
            return false;
        }

        return true;
    }

    public ArrayList<String> getReceivers() { return receivers; }
    public void setReceivers(ArrayList<String> receivers) { this.receivers = receivers; }
    public void addReceiver(String receiver) { this.receivers.add(receiver); }

    public String getAdmin() { return admin; }
    public void setAdmin(String _admin) { this.admin = _admin; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String _groupName) { this.groupName = _groupName; }

}
