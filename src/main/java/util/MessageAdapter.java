package util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class MessageAdapter extends TypeAdapter<Message> {
    @Override
    public void write(JsonWriter out, Message message) throws IOException {
        out.beginObject();
        out.name("text").value(message.getText());
        out.name("sender").value(message.getSender());
        out.name("receiver").value(message.getReceiver());
        out.name("timestamp").value(message.getTimestamp());
        out.name("conversation_id").value(message.getConversation_id());
        out.name("isSystemMessage").value(message.getIsSystemMessage());
        out.endObject();
    }

    @Override
    public Message read(JsonReader in) throws IOException {
        String text = null;
        String sender = null;
        String receiver = null;
        String timestamp = null;
        String conversationId = null;
        boolean isSystemMessage = false;

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "text":
                    text = in.nextString();
                    break;
                case "sender":
                    sender = in.nextString();
                    break;
                case "receiver":
                    receiver = in.nextString();
                    break;
                case "timestamp":
                    timestamp = in.nextString();
                    break;
                case "conversation_id":
                    conversationId = in.nextString();
                    break;
                case "isSystemMessage":
                    isSystemMessage = in.nextBoolean();
                    break;
                default:
                    in.skipValue(); // Ignore unknown fields
                    break;
            }
        }
        in.endObject();

        if (isSystemMessage) {
            Message message = new Message(text, sender, receiver, timestamp, conversationId);
            message.setIsSystemMessage(true);
            return message;
        }
        else
            return new Message(text, sender, receiver, timestamp, conversationId);
    }
}
