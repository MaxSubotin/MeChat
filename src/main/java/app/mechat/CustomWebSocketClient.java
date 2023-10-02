package app.mechat;
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
    public void onMessage(String text) {}

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println(username + " has connected to the ChatServer.");
//        Platform.runLater(() -> {
//            Alert alert = new Alert(Alert.AlertType.INFORMATION);
//            alert.setTitle("Connected");
//            alert.setHeaderText(null);
//            alert.setContentText("You are connected to ChatServer: " + getURI() + "\n");
//            alert.showAndWait();
//        });
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {}
//    @Override
//    public void onClose(int code, String reason, boolean remote) {
//        System.out.println(username + " has disconnected from the ChatServer; Code: " + code + " " + reason + "\n");
////        Platform.runLater(() -> {
////            Alert alert = new Alert(Alert.AlertType.INFORMATION);
////            alert.setTitle("Disconnected");
////            alert.setHeaderText(null);
////            alert.setContentText("You have been disconnected from: " + getURI() + "; Code: " + code + " " + reason + "\n");
////            alert.showAndWait();
////        });
//    }

    @Override
    public void onError(Exception ex) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Exception occurred ...\n" + ex + "\n");
            ex.printStackTrace();
            alert.showAndWait();
        });
    }

}
