package controllers;

import database.Database;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import util.Chat;
import util.RegularChat;
import util.RegularMessage;
import util.User;
import websockets.CustomWebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.UUID;

public class LoadingViewController {

    public User user;
    public MainViewController MVCR = null; // short for mainViewControllerReference
    private final String URI_Address = "ws://localhost:8888/socket?session=";

    @FXML
    ProgressIndicator progressIndicator;


    public boolean loadUserData() {
        // Load user chats
        ArrayList<Chat> userChats = Database.getUsersChatsFromDatabase(user.getId());

        // Establish a new websocket connection, login the user and add chats to the screen
        if (createWebsocketConnection(user)) {
            if (userChats != null) {
                if (!addUsersChatsToScreen(user, userChats)) {
                    MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error loading chats", "Could not load your chats to the screen, please try again later.");
                    return false;
                }
            }

        } else {
            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error in logging-in", "Could not log into you account after you signed-up, please try to log-in later.");
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
            MVCR.webSocketClient = new CustomWebSocketClient(new URI(URI_Address + session), user.getName(), MVCR.getCurrentScene(), MVCR);

            if (Database.getSessionByUserId(user.getId()) == null) {
                if (Database.addSessionToDataBase(user.getId(), session)) {// adding the connecting to the database to keep track of connected users
                    MVCR.webSocketClient.connect();
                }
                else {
                    MVCR.webSocketClient.close();
                    return false;
                }
            }
            else MVCR.webSocketClient.onError(new Exception());

        } catch (URISyntaxException ex) {
            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error", "URISyntaxException, Not a valid WebSocket URI\n" + ex);
            return false;
        }

        return true;
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    // - - - - - - - - - - - Helper Functions  - - - - - - - - - - - //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //

    public boolean addUsersChatsToScreen(User user, ArrayList<Chat> usersChats) { // this method requires there to be a reference to the MainViewController (MVCR)
        if (usersChats == null) return false;

        MVCR.cleanChatBoxes(); // clean the chats of the left if there are any

        for (Chat chat : usersChats) {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("/views/chatBoxComponent.fxml"));
                Pane chatBoxPane = fxmlLoader.load();
                chatBoxPane.setId(chat.getChatId());
                chatBoxPane.setCursor(Cursor.HAND);

                ChatBoxController controller = fxmlLoader.getController();
                controller.setMainViewControllerReference(MVCR);

                if (chat instanceof RegularChat) {
                    User receiver = Database.getUserByUserId(((RegularChat)chat).getReceiver());
                    if (receiver != null) {
                        controller.setNameLabel(receiver.getName());
                        controller.setUserImage(receiver.getUserImage());
                        controller.setContextMenu("RegularChat");
                    }
                }
                else { // need to get the group name, set the name, image and context menu, and controller reference
                    controller.setNameLabel(Database.getGroupNameByGroupId(chat.getChatId()));
                    controller.setUserImage(null); // this will set the default image
                    controller.setContextMenu("GroupChat");
                }

                chat.setMessages(Database.getChatMessagesFromDatabase(chatBoxPane.getId())); // loading the messages from the database

                if (chat.getMessages() != null) {
                    MVCR.getHistoryVBox().getChildren().add(chatBoxPane);
                    user.addChatToUser(chatBoxPane, chat); // adding the pane - chat reference to the hashmap for later use
                }

            } catch (IOException e) {
                MainViewController.showAlertWithMessage(Alert.AlertType.ERROR,"Error Loading Messages", "Could not load some messages from chat: " + chat.getChatId() + ", Please try again later.");
                return false;
            }
        }
        return true;
    }
}
