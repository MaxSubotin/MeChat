package controllers;

import database.Database;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import util.Message;
import util.RegularChat;
import util.User;
import websockets.CustomWebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public class LoadingViewController {

    public User user;
    public CustomWebSocketClient webSocketClient;
    public MainViewController MVCR = null; // short for mainViewControllerReference
    private final String URI_Address = "ws://localhost:8888/socket?session=";

    @FXML
    ProgressIndicator progressIndicator;


    public boolean loadUserData() {
        // Load user chats
        ArrayList<RegularChat> userChats = Database.getUsersChatsFromDatabase(user.getId());

        // Establish a new websocket connection, login the user and add chats to the screen
        if (createWebsocketConnection(user)) {
            if (userChats != null) {
                if (!addUsersChatsToScreen(user, userChats)) {
                    showAlertWithMessage(Alert.AlertType.ERROR, "Error loading chats", "Could not load your chats to the screen, please try again later.");
                    return false;
                }
            }

        } else {
            showAlertWithMessage(Alert.AlertType.ERROR, "Error in logging-in", "Could not log into you account after you signed-up, please try to log-in later.");
            return false;
        }
        return true;
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    // - - - - - - - - - - - User Login Logic  - - - - - - - - - - - //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //

    private boolean createWebsocketConnection(User user) {
        try { // Initialize the client web socket
            String session = UUID.randomUUID().toString(); // Generate a unique session or token
            webSocketClient = new CustomWebSocketClient(new URI(URI_Address + session), user.getName(), MVCR.getCurrentScene(), MVCR);

            if (Database.getSessionByUserId(user.getId()) == null) {
                if (Database.addSessionToDataBase(user.getId(), session)) // adding the connecting to the database to keep track of connected users
                    webSocketClient.connect();
                else {
                    webSocketClient.close();
                    return false;
                }
            }
            else webSocketClient.onError(new Exception());

        } catch (URISyntaxException ex) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Error", "URISyntaxException, Not a valid WebSocket URI\n" + ex);
            return false;
        }

        return true;
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    // - - - - - - - - - - - Helper Functions  - - - - - - - - - - - //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //

    private boolean addUsersChatsToScreen(User user, ArrayList<RegularChat> usersRegularChats) { // this method requires there to be a reference to the MainViewController (MVCR)
        if (usersRegularChats == null) return false;

        MVCR.cleanChatBoxes(); // clean the chats of the left if there are any

        for (RegularChat regularChat : usersRegularChats) {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("/views/chatBoxComponent.fxml"));
                Pane chatBoxPane = fxmlLoader.load();
                chatBoxPane.setId(Database.compareStrings(regularChat.getSender(), regularChat.getReceiver()) + "_" + regularChat.getConversation_id());
                chatBoxPane.setCursor(Cursor.HAND);



                User receiver = Database.getUserByUserId(regularChat.getReceiver());
                if (receiver != null) {
                    ChatBoxController controller = fxmlLoader.getController();
                    controller.setMainViewControllerReference(MVCR);
                    controller.setNameLabel(receiver.getName());
                    controller.setUserImage(receiver.getUserImage());
                    controller.setContextMenu();

                    regularChat.setMessages(Database.getChatMessagesFromDatabase(chatBoxPane.getId())); // loading the messages from the database
                    if (regularChat.getMessages() != null) {
                        MVCR.getHistoryVBox().getChildren().add(chatBoxPane);
                        user.addChatToUser(chatBoxPane, regularChat); // adding the pane - chat reference to the hashmap for later use
                    }
                }
            } catch (IOException e) {
                showAlertWithMessage(Alert.AlertType.ERROR,"Error Loading Messages", "Could not load some messages from " + regularChat.getReceiver() + ". Please try again later.");
                return false;
            }
        }
        return true;
    }


    public void showAlertWithMessage(Alert.AlertType type, String title, String errorMessage) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(errorMessage);
        alert.showAndWait();
    }

}
