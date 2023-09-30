package app.mechat;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class CustomWebSocketClient extends WebSocketClient {
    private String username;

    public CustomWebSocketClient(URI serverURI, String _username) {
        super(serverURI);
        this.username = _username;
    }

    @Override
    public void onMessage(String text) {}

    @Override
    public void onOpen(ServerHandshake handshake) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Connected");
            alert.setHeaderText(null);
            alert.setContentText("You are connected to ChatServer: " + getURI() + "\n");
            alert.showAndWait();
        });
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Disconnected");
            alert.setHeaderText(null);
            alert.setContentText("You have been disconnected from: " + getURI() + "; Code: " + code + " " + reason + "\n");
            alert.showAndWait();
        });
    }

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

    public String getUsername() {
        return username;
    }
}
