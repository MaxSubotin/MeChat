package util;

import database.Database;
import websockets.CustomWebSocketClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;

public class RegularChat extends Chat {

    private String receiver; // these are user id, not username

    public RegularChat(ArrayList<Message> _messages, String _sender, String _receiver, String _chatId, int _messageCount) {
        super(_messages, _sender, _chatId, _messageCount);
        this.receiver = _receiver;
    }

    @Override
    public boolean sendMessage(CustomWebSocketClient client, String text) {
        // Create a message object and the controller to access methods
        RegularMessage message = new RegularMessage(text, getSender(), createTimestamp(), getChatId(), receiver);

        try {
            // Adding the new message to the array and the database
            if (Database.addRegularMessageToDatabase(message, getChatId())) {

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


    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
}
