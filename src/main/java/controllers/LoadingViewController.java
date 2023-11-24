package controllers;

import database.Database;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
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

    @FXML
    ProgressIndicator progressIndicator;


    public boolean loadUserData() {
        // Load user chats
        ArrayList<RegularChat> userChats = Database.getUsersChatsFromDatabase(user.getId());

        // Establish a new websocket connection, login the user and add chats to the screen
        if (handleUserLogin(user)) {
            if (userChats != null) {
                if (!addUsersChatsToScreen(user, userChats)) {
                    showAlertWithMessage(Alert.AlertType.ERROR, "Error loading chats", "Could not load your chats to the screen, please try again later.");
                    return false;
                }
            }

        } else {
            showAlertWithMessage(Alert.AlertType.ERROR, "Error in loging-in", "Could not log into you account after you signed-up, please try to log-in later.");
            return false;
        }
        return true;
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    // - - - - - - - - - - - User Login Logic  - - - - - - - - - - - //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //

    private boolean handleUserLogin(User user) {
        try { // Initialize the client web socket
            String session = UUID.randomUUID().toString(); // Generate a unique session or token
            webSocketClient = new CustomWebSocketClient(new URI("ws://localhost:8888/socket?session=" + session), user.getName(), MVCR.getCurrentScene()) {

                @Override
                public void onMessage(String text) {
                    // Deserialize the received JSON string back to your custom object
                    Message receivedMessage = gson.fromJson(text, Message.class);

                    if (receivedMessage.getIsSystemMessage()) { // it is a system message
                        if (!Objects.equals(user.getId(), receivedMessage.getSender())) {
                            if (text.contains("DISCONNECT//")) {
                                if (Objects.equals(MVCR.selectedChatBoxUserId, receivedMessage.getSender()))
                                    Platform.runLater(() -> {
                                        MVCR.setConnectedLabelOff();
                                    });
                            } else if (text.contains("CONNECT//")) {
                                if (Objects.equals(MVCR.selectedChatBoxUserId, receivedMessage.getSender()))
                                    Platform.runLater(() -> {
                                        MVCR.setConnectedLabelOn();
                                    });
                            } else if (text.contains("USERNAME//CHANGED//")) {
                                Platform.runLater(() -> {
                                    // Holding on to the old name and changing the title to the new name
                                    String newName = receivedMessage.getText().split("//")[2];

                                    if (Objects.equals(MVCR.selectedChatBoxUserId, receivedMessage.getSender())) {
                                        // The person we are chatting with has changed their name, update the chat name label and the selected chat box name
                                        MVCR.chatNameLabel.setText(newName);
                                        ((Label) (MVCR.selectedChatBoxPane.getChildren()).get(0)).setText(newName);

                                    } else {
                                        // Find the correct chat box pane (on the right side) and rename the name label on it
                                        String chatId = Database.compareStrings(receivedMessage.getSender(), user.getId());
                                        System.out.println("Looking for: " + chatId);
                                        for (Pane chatPane : user.userChats.keySet()) {
                                            System.out.println("#: " + chatPane.getId());
                                            if (chatPane.getId().contains(chatId))
                                                ((Label) (chatPane.getChildren()).get(0)).setText(newName);
                                        }
                                    }
                                });
                            } else if (text.contains("USERNAME//DELETED//")) {
                                Platform.runLater(() -> {
                                    // Find and save the chat we need to delete from the current user
                                    Pane chatToDelete = null;
                                    for (Pane chatPane : user.userChats.keySet()) {
                                        if (chatPane.getId().contains(receivedMessage.getSender()))
                                            chatToDelete = chatPane;
                                    }

                                    // Delete the chat if found
                                    if (chatToDelete != null) {
                                        MVCR.historyVBox.getChildren().remove(chatToDelete);
                                        user.userChats.remove(chatToDelete);

                                        // delete the message bubbles if the deleted chat is open
                                        if (Objects.equals(MVCR.selectedChatBoxUserId, receivedMessage.getSender())) {
                                            MVCR.cleanChatBubbles();
                                            MVCR.connectedLabel.setText("");
                                            MVCR.selectedChatBoxUserId = null;
                                            MVCR.selectedChatBoxPane = null;
                                        }
                                    }
                                });
                            } else if (text.contains("NEW//CHAT//CREATED//")) { // This case is not yet implemented
                                Platform.runLater(() -> {
                                    String currentChat = MVCR.selectedChatBoxPane.getId();
                                    if (addUsersChatsToScreen(user, Database.getUsersChatsFromDatabase(user.getId()))) { // look at this part, its strange, note sure about it.
                                        for (Node child : MVCR.historyVBox.getChildren()) {
                                            if (Objects.equals(child.getId(), currentChat)) {
                                                System.out.println("Inside the if statement");
                                                MVCR.selectedChatBoxPane = (Pane) child;
                                                MVCR.selectedChatBoxPane.setStyle("-fx-border-color: skyblue; -fx-border-radius: 15px; -fx-border-width: 0.5px");
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }
                    else {
                        // Explanation: we will know that the current user is looking at the chat where the message was received if the sender of the message
                        // AND the chatNameLabel.getText() are equal, there for the current user is looking at the chat when the message is received

                        if (Objects.equals(user.getId(),receivedMessage.getSender()) || Objects.equals(MVCR.selectedChatBoxUserId, receivedMessage.getSender())) {
                            Platform.runLater(() -> {
                                try {
                                    // Creating a new message bubble on the screen and adding the users text into it
                                    FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("/views/chatBubbleComponent.fxml"));
                                    HBox chatBubble = fxmlLoader.load();

                                    ChatBubbleController controller = fxmlLoader.getController();
                                    controller.setMessageBubbleLabel(receivedMessage.getText());
                                    controller.setMessage(receivedMessage);


                                    if (Objects.equals(receivedMessage.getSender(), user.getId())) {
                                        controller.setMessageBubbleLabelColorBlue();
                                        chatBubble.setAlignment(Pos.CENTER_RIGHT);
                                    }
                                    else
                                        chatBubble.setAlignment(Pos.CENTER_LEFT);

                                    // Add message bubble to the screen
                                    MVCR.chatVBox.getChildren().add(chatBubble);

                                    // adding the new message to the list of messages for this user
                                    if (!Objects.equals(user.getId(),receivedMessage.getSender()))
                                        user.getUserChats().get(MVCR.selectedChatBoxPane).getMessages().add(receivedMessage);

                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        }
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Platform.runLater(() -> {
                        if (code == 1006) {
                            showAlertWithMessage(Alert.AlertType.ERROR, "Error", user.getName() + " has disconnected from the ChatServer; Code: " + code + " " + reason + "\n");
                            MVCR.closeChatAppWindow();
                        }
                        System.out.println("Close method of ClientWebSocket was called.");
                    });
                }
            };

            if (Database.getSessionByUserId(user.getId()) == null) {
                if (Database.addSessionToDataBase(user.getId(), session)) // adding the connecting to the database to keep track of connected users
                    webSocketClient.connect();
                else {
                    webSocketClient.close();
                    return false;
                }
            }
            else
                webSocketClient.onError(new Exception());

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

                ChatBoxController controller = fxmlLoader.getController();
                controller.setMainViewControllerReference(MVCR);
                controller.setNameLabel(Database.getUsernameById(regularChat.getReceiver()));

                regularChat.setMessages(Database.getChatMessagesFromDatabase(chatBoxPane.getId())); // loading the messages from the database
                if (!Objects.equals(regularChat.getMessages().get(0).getSender(), "ERROR")) { // based on the implementation of the catch block of "Database.getChatMessagesFromDatabase"
                    MVCR.getHistoryVBox().getChildren().add(chatBoxPane);
                    user.addChatToUser(chatBoxPane, regularChat); // adding the pane - chat reference to the hashmap for later use
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
