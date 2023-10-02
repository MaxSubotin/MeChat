package app.mechat;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import java.util.Objects;
import java.util.UUID;

public class MainViewController {

    public User MyUser = null;
    private Pane selectedChatBoxPane;
    private String selectedChatBoxName;
    private int newChatBoxCounter = 0;
    private CustomWebSocketClient webSocketClient;
    private Message message = null;


    @FXML
    ImageView profileButton, newChatButton, settingsButton, sendMessageButton, userPictureImageView;
    @FXML
    VBox historyVBox, chatVBox;
    @FXML
    TextField messageTextField, loginUsernameField, loginPasswordField, signupUsernameField, signupPhoneNumberField, signupPasswordField;
    @FXML
    Label chatNameLabel, userUsernameLabel, userPhoneNumberLabel, connectedLabel;
    @FXML
    Pane loginAndSignupPane;
    @FXML
    Button closeProfileButton, loginButton, signupButton;
    @FXML
    TitledPane theLoginTab,theSignupTab,theUserTab;
    @FXML
    ScrollPane chatScrollPane;


    @FXML
    public void newChatButtonOnClick() {
        // Counting the new chat boxes, only one on the screen at a time
        if (newChatBoxCounter > 0) return;
        if (MyUser == null) return;

        // Adding a new chat box to the screen, waiting for the user to input the receiver's name/number
        try {
            // Add the chat box to the screen
            FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("newChatBox.fxml"));
            Pane newChatBoxPane = fxmlLoader.load();
            newChatBoxCounter++;

            NewChatBoxController controller = fxmlLoader.getController();
            controller.setMainViewControllerReference(this); // Giving a reference to the main controller for communication
            historyVBox.getChildren().add(newChatBoxPane);
            getFocus();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void sendMessageButtonOnClick() {
        chatScrollPane.setVvalue(1.0); // Scrolls down

        // Check message
        String text = messageTextField.getText();
        if (text == null || text.isEmpty()) return;

        // Get current timestamp
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedTime = currentTime.format(formatter);

        // Create a message object and the controller to access methods
        message = new Message(text, MyUser.getName(), formattedTime);

        // Send the message text to all users of the chat
        webSocketClient.send(message.getText());

        // Add the message to the database
        String[] splitChatBoxName = this.getSelectedChatBoxPane().getId().split("_");
        Database.addMessageToDatabase(message, Integer.parseInt(splitChatBoxName[1]));

        messageTextField.clear(); // Removing the text from the textfield
        getFocus();
    }

    @FXML
    public void profileButtonOnClick() {
        loginAndSignupPane.setVisible(true);
    }

    @FXML
    public void closeProfileButtonOnClick() {
        loginAndSignupPane.setVisible(false);
    }

    @FXML
    public void loginButtonOnClick() {
        closeConnectionIfOpen();

        // Get the text and see if it matches the database, if it does then login into the user
        User user = Database.getUserFromDatabase(loginUsernameField.getText(), loginPasswordField.getText());
        if (user != null) MyUser = user;
        else return;

        // Initialize the client web socket
        try {
            String session = UUID.randomUUID().toString(); // Generate a unique session or token
            webSocketClient = new CustomWebSocketClient(new URI("ws://localhost:8887/socket?session=" + session), MyUser.getName()) {

                @Override
                public void onMessage(String text) {
                    if (text.contains("SYSTEM//CONNECT//")) {
                        String[] sysMessage = text.split("//");
                        if (Objects.equals(chatNameLabel.getText(), sysMessage[2]))
                            Platform.runLater(() -> { setConnectedLabelOn(); });

                    }
                    else if (text.contains("SYSTEM//DISCONNECT//")) {
                        String[] sysMessage = text.split("//");
                        if (Objects.equals(chatNameLabel.getText(), sysMessage[2]))
                            Platform.runLater(() -> { setConnectedLabelOff(); });

                    }
                    else {
                        Platform.runLater(() -> {
                            try {
                                // Creating a new message bubble on the screen and adding the users text into it
                                FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("chatBubble.fxml"));
                                HBox chatBubble = fxmlLoader.load();

                                ChatBubbleController controller = fxmlLoader.getController();
                                controller.setMessageBubbleLabel(text);
                                controller.setMessage(message);

                                if (message != null) {
                                    chatBubble.setAlignment(Pos.CENTER_RIGHT); // The users messages appear of the right side
                                    controller.setMessageBubbleLabelColorBlue(); // The users messages are colored sky-blue
                                    message = null; // nullify the current message after it is sent to get the app ready for the next message broadcast
                                } else
                                    chatBubble.setAlignment(Pos.CENTER_LEFT);

                                // Add message bubble to the screen
                                chatVBox.getChildren().add(chatBubble);

                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Platform.runLater(() -> {
                        if (code == 1006) {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText(null);
                            alert.setContentText(MyUser.getName() + " has disconnected from the ChatServer; Code: " + code + " " + reason + "\n");
                            alert.showAndWait();
                        }

                        Stage stage = (Stage) profileButton.getScene().getWindow();
                        stage.close();
                    });
                }

            };

            webSocketClient.connect();
            Database.addSessionToDataBase(MyUser.getName(), session); // adding the connecting to the database to keep track of connected users

        } catch (URISyntaxException ex) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Not a valid WebSocket URI\n" + ex);
                alert.showAndWait();
            });
            return;
        }

        // Show the USER tab and show users chats
        showUserTab();
        addUsersChatsToScreen();
        cleanLoginForm();

    }

    @FXML
    public void signupButtonOnClick() {
        String username = signupUsernameField.getText();
        String phoneNumber = signupPhoneNumberField.getText();

        // Check that username AND phoneNumber are unique
        if (Database.isUsernameUnique(username) || Database.isPhoneNumberUnique(phoneNumber)) return;

        // Add user to database and Login
        MyUser = new User(username, phoneNumber);
        Database.addUserToDatabase(username, signupPasswordField.getText(), phoneNumber);

        // Show the USER tab and show users chats
        showUserTab();
        addUsersChatsToScreen();
        cleanSignupForm();
    }


    // Helper Functions

    public void turnChatInvisible() {
        // Turing the chat and textfield invisible
        messageTextField.setVisible(false);
        getFocus();
    }

    public void turnChatVisible() {
        // Turing the chat and textfield visible
        messageTextField.setVisible(true);
        getFocus();
    }

    public void getFocus() {
        // Setting the focus on a random button so that the text field will not be selected and focused
        profileButton.requestFocus();
    }

    public void cleanChatBubbles() {
        this.chatVBox.getChildren().clear();
    }

    public void openLoginTab() {
        loginAndSignupPane.setVisible(true);
        theLoginTab.setExpanded(true);
    }

    private void showUserTab() {
        userUsernameLabel.setText(MyUser.getName());
        userPhoneNumberLabel.setText(MyUser.getPhoneNumber());
        theUserTab.setVisible(true);
        theUserTab.setExpanded(true);
    }

    private void addUsersChatsToScreen() {
        historyVBox.getChildren().clear(); // clean the chats of the left if there are any

        // maybe do this operation in a separate thread in case a user has a lot of chats...
        ArrayList<Chat> usersChats = Database.getUsersChatsFromDatabase(MyUser.getName());
        for (Chat chat: usersChats) {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("chatBox.fxml"));
                Pane chatBoxPane = fxmlLoader.load();
                chatBoxPane.setId("chatbox_" + chat.getConversation_id());
                chatBoxPane.setCursor(Cursor.HAND);
                ChatBoxController controller = fxmlLoader.getController();

                controller.setMainViewControllerReference(this);
                controller.setNameLabel(chat.getReceiver());
                getHistoryVBox().getChildren().add(chatBoxPane);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void cleanSignupForm() {
        signupUsernameField.clear();
        signupPasswordField.clear();
        signupPhoneNumberField.clear();
    }

    private void cleanLoginForm() {
        loginUsernameField.clear();
        loginPasswordField.clear();
    }

    public void closeConnectionIfOpen() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    // getters and setters

    public VBox getHistoryVBox() { return historyVBox; }

    public VBox getChatVBox() { return chatVBox; }

    public ImageView getProfileButton() { return profileButton; }

    public void setChatNameLabel(String name) { this.chatNameLabel.setText(name); }

    public Pane getSelectedChatBoxPane() { return selectedChatBoxPane; }

    public void setSelectedChatBoxPane(Pane chatBoxPane) { this.selectedChatBoxPane = chatBoxPane; }

    public String getSelectedChatBoxName() { return selectedChatBoxName; }

    public void setSelectedChatBoxName(String selectedChatBoxName) { this.selectedChatBoxName = selectedChatBoxName; }

    public int getNewChatBoxCounter() { return newChatBoxCounter; }

    public void setNewChatBoxCounter(int newChatBoxCounter) { this.newChatBoxCounter = newChatBoxCounter; }

    public void setConnectedLabelOn() { connectedLabel.setText("connected"); }
    public void setConnectedLabelOff() { connectedLabel.setText("disconnected"); }
    public String getConnectedLabel() { return connectedLabel.getText(); }

}
