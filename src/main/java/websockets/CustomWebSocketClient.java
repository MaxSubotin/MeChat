package websockets;
import database.Database;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class CustomWebSocketClient extends WebSocketClient {
    private final String username;
    private final Scene scene;

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
            Database.removeUserSession(userId);
        });
    }

}
