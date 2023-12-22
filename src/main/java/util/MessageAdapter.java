package util;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import javafx.scene.Group;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class MessageAdapter extends TypeAdapter<Message> {

    @Override
    public void write(JsonWriter out, Message message) throws IOException {
        out.beginObject();
        out.name("text").value(message.getText());
        out.name("sender").value(message.getSender());
        writeReceiver(out, message);
        out.name("timestamp").value(message.getTimestamp());
        out.name("chatId").value(message.getChatId());
        out.name("isSystemMessage").value(message instanceof SystemMessage);
        if (message instanceof SystemMessage) {
            out.name("type").value(((SystemMessage) message).getType().toString());
        }
        out.endObject();
    }

    private void writeReceiver(JsonWriter out, Message message) throws IOException {
        if (message instanceof RegularMessage) {
            out.name("receiver").value(((RegularMessage) message).getReceiver());
        } else if (message instanceof GroupMessage) {
            out.name("receivers").beginArray();
            for (String receiver : ((GroupMessage) message).getReceivers()) {
                out.value(receiver);
            }
            out.endArray();
        }
    }

    @Override
    public Message read(JsonReader in) throws IOException {
        String text = null;
        String sender = null;
        String timestamp = null;
        String chatId = null;
        boolean isSystemMessage = false;
        SystemMessageType systemMessageType = null;
        String receiver = null;
        ArrayList<String> receivers = null;

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
                case "timestamp":
                    timestamp = in.nextString();
                    break;
                case "chatId":
                    chatId = in.nextString();
                    break;
                case "isSystemMessage":
                    isSystemMessage = in.nextBoolean();
                    break;
                case "type":
                    systemMessageType = SystemMessageType.valueOf(in.nextString());
                    break;
                case "receiver":
                    receiver = in.nextString();
                    break;
                case "receivers":
                    receivers = new ArrayList<>();
                    in.beginArray();
                    while (in.hasNext()) {
                        receivers.add(in.nextString());
                    }
                    in.endArray();
                    break;
                default:
                    in.skipValue(); // Ignore unknown fields
                    break;
            }
        }
        in.endObject();

        if (isSystemMessage) {
            if (systemMessageType != null) {
                return new SystemMessage(text, sender, timestamp, chatId, systemMessageType);
            } else {
                throw new IOException("SystemMessageType is missing for SystemMessage");
            }
        } else if (receiver != null) {
            return new RegularMessage(text, sender, timestamp, chatId, receiver);
        } else if (receivers != null) {
            return new GroupMessage(text, sender, timestamp, chatId, receivers);
        } else {
            throw new IOException("Invalid message type");
        }
    }

}
