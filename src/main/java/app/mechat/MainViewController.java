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

import at.favre.lib.crypto.bcrypt.BCrypt;

import com.google.gson.Gson;


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
    TextField messageTextField, loginUsernameField, loginPasswordField, signupUsernameField, signupPasswordField, settingsUsernameField, settingsPasswordField;
    @FXML
    Label chatNameLabel, userUsernameLabel, connectedLabel;
    @FXML
    Pane loginAndSignupPane, settingsPane;
    @FXML
    Button closeProfileButton, loginButton, signupButton, settingsUsernameSaveButton, settingPasswordSaveButton, settingsCloseButton;
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

        // Get current conversation id
        String conversationId = selectedChatBoxPane.getId().split("_")[2];

        // Create a message object and the controller to access methods
        message = new Message(text, MyUser.getName(), chatNameLabel.getText(), formattedTime, conversationId);

        // Turn Message object into json format:
        // Create a Gson object for JSON serialization/deserialization.
        Gson gson = new Gson();

        // Serialize the message object to JSON.
        String jsonMessage = gson.toJson(message);

        // Send the message in json format to the chat server where it will be sent to the correct user
        webSocketClient.send(jsonMessage);

        // Add the message to the database
        String[] splitChatBoxName = this.getSelectedChatBoxPane().getId().split("_");
        Database.addMessageToDatabase(message, chatNameLabel.getText(), Integer.parseInt(splitChatBoxName[2]));

        messageTextField.clear(); // Removing the text from the textfield
        getFocus();
    }


    @FXML
    public void settingsButtonOnClick() { settingsPane.setVisible(true); }

    @FXML
    public void closeSettingsButtonOnClick() { settingsPane.setVisible(false); }

    @FXML
    public void settingsUsernameSaveButtonOnClick() {
        String newUsername = settingsUsernameField.getText();

        // Check new username with regex

        // Check newUsername in database and update the database
        if (Database.isUsernameUnique(newUsername)) {
            Database.updateUsernameInDatabase(newUsername, MyUser.getName());
            MyUser.setName(newUsername);
        } else {
            // Show and error message
            showAlertWithMessage(Alert.AlertType.ERROR,"Error", newUsername + " is already taken.\nTry a different username.");
        }
    }

    @FXML
    public void settingsPasswordSaveButtonOnClick() {
        // Check new password with regex
        // if it is ok then:
        if (true) {
            // Hashing the password
            String HashedPassword = BCrypt.withDefaults().hashToString(12, signupPasswordField.getText().toCharArray());

            // Update the database
            Database.updatePasswordInDatabase(HashedPassword, MyUser.getName());
        } else {
            // Show and error message
            showAlertWithMessage(Alert.AlertType.ERROR,"Error", "Your new password is not good enough.\nTry a different one.");
        }

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
        // Cleaning the UI
        closeConnectionIfOpen(); // close any open connections if there are any
        chatVBox.getChildren().clear(); // clean the chats of the left if there are any
        chatNameLabel.setText(" ");
        connectedLabel.setText(" ");

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
                    // Create a Gson object for JSON serialization/deserialization.
                    Gson gson = new Gson();

                    // Deserialize the received JSON string back to your custom object
                    Message receivedMessage = gson.fromJson(text, Message.class);

                    if (receivedMessage.getIsSystemMessage()) { // it is a system message
                        if (text.contains("CONNECT//")) {
                            if (Objects.equals(chatNameLabel.getText(), receivedMessage.getSender()))
                                Platform.runLater(() -> { setConnectedLabelOn(); });
                        }
                        else if (text.contains("DISCONNECT//")) {
                            if (Objects.equals(chatNameLabel.getText(), receivedMessage.getSender()))
                                Platform.runLater(() -> { setConnectedLabelOff(); });
                        }
                    }
                    else {
                        // Explanation: we will know that the current user is looking at the chat where the message was received if the sender of the message
                        // AND the chatNameLabel.getText() are equal, there for the current user is looking at the chat when the message is received

                        if (Objects.equals(MyUser.getName(),receivedMessage.getSender()) || Objects.equals(chatNameLabel.getText(), receivedMessage.getSender())) {
                            Platform.runLater(() -> {
                                try {
                                    // Creating a new message bubble on the screen and adding the users text into it
                                    FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("chatBubble.fxml"));
                                    HBox chatBubble = fxmlLoader.load();

                                    ChatBubbleController controller = fxmlLoader.getController();
                                    controller.setMessageBubbleLabel(receivedMessage.getText());
                                    controller.setMessage(receivedMessage);

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
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Platform.runLater(() -> {
                        if (code == 1006) {
                            showAlertWithMessage(Alert.AlertType.ERROR,"Error",MyUser.getName() + " has disconnected from the ChatServer; Code: " + code + " " + reason + "\n");

                            Stage stage = (Stage) profileButton.getScene().getWindow();
                            stage.close();
                        }

                    });
                }

            };

            Database.addSessionToDataBase(MyUser.getName(), session); // adding the connecting to the database to keep track of connected users
            webSocketClient.connect();

        } catch (URISyntaxException ex) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Error", "URISyntaxException, Not a valid WebSocket URI\n" + ex);
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

        // Check that the username and password are in a correct format like length, special characters and the like

        // Hashing the password
        String HashedPassword = BCrypt.withDefaults().hashToString(12, signupPasswordField.getText().toCharArray());

        // Check that username is unique
        if (Database.isUsernameUnique(username)) return;

        // Add user to database and Login
        MyUser = new User(username);
        Database.addUserToDatabase(username, HashedPassword); // saving the hashed versions

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
        this.setChatNameLabel("");
        this.setConnectedLabelOff();
    }

    public void openLoginTab() {
        loginAndSignupPane.setVisible(true);
        theLoginTab.setExpanded(true);
    }

    private void showUserTab() {
        userUsernameLabel.setText(MyUser.getName());
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
                chatBoxPane.setId(Database.compareStrings(chat.getSender(), chat.getReceiver()) + "_" + chat.getConversation_id());
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

    public void showAlertWithMessage(Alert.AlertType type, String title, String errorMessage) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(errorMessage);
        alert.showAndWait();
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
