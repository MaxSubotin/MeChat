package websockets;
import com.google.gson.Gson;
import database.Database;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import util.Message;
import util.MessageAdapter;
import util.User;
import com.google.gson.GsonBuilder;

import java.net.URI;
import java.sql.Timestamp;

public class CustomWebSocketClient extends WebSocketClient {
    private final String username;
    private final Scene scene;

    public final Gson gson = new GsonBuilder().registerTypeAdapter(Message.class, new MessageAdapter()).create();


    public CustomWebSocketClient(URI serverURI, String _username, Scene _scene) {
        super(serverURI);
        this.username = _username;
        this.scene = _scene;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println(username + " has connected to the ChatServer.");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {}

    @Override
    public void onMessage(String text) {}

    @Override
    public void onError(Exception ex) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Exception occurred ...\nClosing the app now, try again later.");
            alert.showAndWait();

            Stage stage = (Stage) scene.getWindow();
            stage.close();

            String userId = Database.getUserIdByUsername(this.username);
            if (userId != null) {
                if (!Database.removeUserSession(userId))
                    System.out.println("Could not complete the user session removal form the database. Error in onError Override function in CustomWebSocketClient.");
            }
        });
    }

    public void sendMessageToServer(User user, String message) {
        // Create a system message object
        Message sysMessage = new Message(message, user.getId(), user.getId(), new Timestamp(System.currentTimeMillis()).toString() ,"-1");
        sysMessage.setIsSystemMessage(true);

        // Turn Message object into json format
        String jsonSysMessage = gson.toJson(sysMessage);

        // Send message to users that you have changed your name
        send(jsonSysMessage);
    }

}
