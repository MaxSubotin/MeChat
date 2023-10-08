package websockets;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class CustomWebSocketClient extends WebSocketClient {
    private final String username;

    public CustomWebSocketClient(URI serverURI, String _username) {
        super(serverURI);
        this.username = _username;
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
            alert.setContentText("Exception occurred ...\n" + ex + "\nClosing the app now, try again later.");
            ex.printStackTrace();
            alert.showAndWait();
        });
    }

}
