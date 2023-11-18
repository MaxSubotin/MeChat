package controllers;

import database.Database;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import at.favre.lib.crypto.bcrypt.BCrypt;

import util.*;
import websockets.CustomWebSocketClient;


public class MainViewController {

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    // - - - - - - - - - - Variable Declaration  - - - - - - - - - - //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //


    public User MyUser = null;
    public RegularChat currentRegularChat = null;
    private Pane selectedChatBoxPane;
    private HBox selectedAvatarHbox;
    private ImageView selectedUserImage = null;
    private String selectedChatBoxUserId;
    private int temporaryChatBoxCounter = 0;
    private CustomWebSocketClient webSocketClient;

    // Create a Gson object for JSON serialization/deserialization.
    // private final Gson gson = new GsonBuilder().registerTypeAdapter(Message.class, new MessageAdapter()).create();


    @FXML
    ImageView profileButton, newChatButton, settingsButton, sendMessageButton, userPictureImageView, male_AvatarImage, male_AvatarImageS, female_AvatarImage, female_AvatarImageS, settingsDeleteAccountButton;
    @FXML
    VBox historyVBox, chatVBox;
    @FXML
    HBox maleAvatarHBox, maleAvatarHBoxS, femaleAvatarHBox, femaleAvatarHBoxS;
    @FXML
    TextField messageTextField, loginUsernameField, loginPasswordField, signupUsernameField, signupPasswordField, settingsUsernameField, settingsPasswordField;
    @FXML
    Label chatNameLabel, userUsernameLabel, connectedLabel;
    @FXML
    Pane loginAndSignupPane, settingsPane;
    @FXML
    Button closeProfileButton, loginButton, signupButton, settingsUsernameSaveButton, settingPasswordSaveButton, settingsCloseButton, settingAvatarSaveButton;
    @FXML
    TitledPane theLoginTab,theSignupTab,theUserTab;
    @FXML
    ScrollPane chatScrollPane;

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    // - - - - - - - - - Buttons OnClick Functions - - - - - - - - - //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //

    @FXML
    public void newChatButtonOnClick() {
        // Counting the new chat boxes, only one on the screen at a time
        if (temporaryChatBoxCounter > 0) return;
        if (MyUser == null) return;

        // Adding a new chat box to the screen, waiting for the user to input the receiver's name/number
        try {
            // Add the chat box to the screen
            FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("/views/temporaryChatBoxComponent.fxml"));
            Pane newChatBoxPane = fxmlLoader.load();
            temporaryChatBoxCounter++;

            TemporaryChatBoxController controller = fxmlLoader.getController();
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

        if (!currentRegularChat.sendMessage(webSocketClient, text)) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not send the message","There was a problem sending the message, please try again later.");
            return;
        }

        messageTextField.clear(); // Removing the text from the textfield
        getFocus();
    }


    @FXML
    public void settingsButtonOnClick() {
        settingsPane.setVisible(true);

        if (MyUser != null) {
            if (Objects.equals(MyUser.getUserImage(), "male"))
                male_AvatarImageS.setStyle("-fx-border-color: skyblue; -fx-border-radius: 30px; -fx-border-width: 3px");
            else
                female_AvatarImageS.setStyle("-fx-border-color: skyblue; -fx-border-radius: 30px; -fx-border-width: 3px");
        }
    }

    @FXML
    public void closeSettingsButtonOnClick() { settingsPane.setVisible(false); }

    @FXML
    public void settingsUsernameSaveButtonOnClick() {
        String newUsername = settingsUsernameField.getText();

        // Check new username with regex
        if (!RegexChecker.isValidUsername(newUsername)) {
            // Show and error message
            showAlertWithMessage(Alert.AlertType.ERROR,"Error", "Your new username is not good enough.\nTry a different one.");
            return;
        }

        // Check newUsername in database and update the database
        if (MyUser.setName(newUsername)) {
            webSocketClient.sendMessageToServer(MyUser, "USERNAME//CHANGED//" + newUsername);
            settingsUsernameField.clear();
        } else {
            showAlertWithMessage(Alert.AlertType.ERROR,"Error", newUsername + " is already taken.\nTry a different username.");
        }
    }

    @FXML
    public void settingsPasswordSaveButtonOnClick() {
        // Check new password with regex
        if (RegexChecker.isValidPassword(signupPasswordField.getText())) {
            if (!MyUser.updatePassword(BCrypt.withDefaults().hashToString(12, signupPasswordField.getText().toCharArray())))
                return;
            signupPasswordField.clear();
        } else {
            // Show and error message
            showAlertWithMessage(Alert.AlertType.ERROR,"Error", "Your new password is not good enough.\nTry a different one.");
        }

    }

    @FXML
    public void settingsAvatarSaveButtonOnClick() {
        if (selectedAvatarHbox.getId().equals("maleAvatarHBox")) {
            MyUser.setUserImage("male");
        }
        else {
            MyUser.setUserImage("female");
        }
    }

    @FXML
    public void settingsDeleteAccountButtonOnClick() {
        if (MyUser != null) {
            if (Database.deleteUser(MyUser.getId()))
                webSocketClient.sendMessageToServer(MyUser, "USERNAME//DELETED//" + MyUser.getName());
            else
                System.out.println("Could not properly delete your user from the database, please try again later.");

            closeChatAppWindow();
        }
    }


    @FXML
    public void profileButtonOnClick() {
        loginAndSignupPane.setVisible(true);
        if (MyUser != null) {
            showUserTab();
        }
    }

    @FXML
    public void closeProfileButtonOnClick() {
        loginAndSignupPane.setVisible(false);
    }


    @FXML
    public void loginButtonOnClick() {
        // 1. Close any open connections (in case the user was already logged in to a different account)
        closeConnectionIfOpen();
        cleanChatBoxes();
        cleanChatBubbles();
        setConnectedLabelEmpty();

        // 2. Get user info
        String usernameInput = loginUsernameField.getText(), passwordInput = loginPasswordField.getText();
        if (usernameInput.isEmpty() || passwordInput.isEmpty()) {
            showAlertWithMessage(Alert.AlertType.WARNING, "Missing Info", "Please enter a username and a password.");
            return;
        }

        // 3. Fetch user data from the database
        MyUser = Database.getUserFromDatabase(usernameInput,passwordInput);
        if (MyUser == null) return;

        ArrayList<RegularChat> userChats = Database.getUsersChatsFromDatabase(MyUser.getId());
        if (userChats != null) {

            // 4. Establish a new websocket connection
            handleUserLogin();

            // 5. Show user info on the gui
            if (!addUsersChatsToScreen(userChats)) return;
            showUserTab();
            cleanLoginForm();
        }
    }

    @FXML
    public void signupButtonOnClick() {
        // 1. Getting the username, hashing the password and setting default user image name
        String username = signupUsernameField.getText();
        String HashedPassword = BCrypt.withDefaults().hashToString(12, signupPasswordField.getText().toCharArray());
        String userImageName = "male";

        // 2. Check that the username and password are in a correct format like length, special characters and the like
        if (!(RegexChecker.isValidUsername(username) && RegexChecker.isValidPassword(signupPasswordField.getText()))) {
            // Show and error message
            showAlertWithMessage(Alert.AlertType.ERROR,"Error", "The username or password are incorrect.\nUsername: 1 lower case letter, 1 upper case letter, 1 number, no special character, up-to 12 characters long. \nPassword: 1 lower case letter, 1 upper case letter, 1 number, can have special character, up-to 12 characters long.");
            return;
        }

        // Check that the user has selected an avatar
        if (selectedUserImage == null) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Error", "Please select an avatar.");
            return;
        }
        else
            userImageName = selectedUserImage.getId().split("_")[0]; // the id is "male_AvatarImageS"

        // Check that username is unique
        if (!Database.isUsernameUnique(username)) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Username Taken", "That username is already taken, try a different one.");
            return;
        }

        // 3. Create a new user and add it to the database
        String userId = IdGenerator.generateUniqueUserId(); // Generate a unique user id
        MyUser = new User(username, userId, userImageName);
        if (Database.addUserToDatabase(username, HashedPassword, userId, userImageName)) { // saving the hashed version of the password

            // 4. Log the user into his account and establish a new websocket connection
            if (handleUserLogin()) {
                showUserTab();
                cleanSignupForm();
                selectedUserImage = null; // cleaning the selected image
            }
        } else {
            MyUser = null;
            selectedUserImage = null;
        }
    }

    @FXML
    public void avatarImageOnClick(MouseEvent event) {
        // Removing old border
        if (selectedAvatarHbox != null) {
            selectedAvatarHbox.setStyle("-fx-border-radius: 30px");
        }

        // Creating and adding new border to the selected avatar and saving the selection
        ImageView newUserImage = (ImageView) event.getSource();
        HBox selectedAvatar = (HBox) newUserImage.getParent();
        selectedAvatar.setStyle("-fx-border-color: skyblue; -fx-border-radius: 30px; -fx-border-width: 3px");
        selectedAvatarHbox = selectedAvatar;
        selectedUserImage = newUserImage;
    }

    
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    // - - - - - - - - - - - User Login Logic  - - - - - - - - - - - //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //

    private boolean handleUserLogin() {
        try { // Initialize the client web socket
            String session = UUID.randomUUID().toString(); // Generate a unique session or token
            webSocketClient = new CustomWebSocketClient(new URI("ws://localhost:8888/socket?session=" + session), MyUser.getName(), getCurrentScene()) {

                @Override
                public void onMessage(String text) {
                    // Deserialize the received JSON string back to your custom object
                    Message receivedMessage = gson.fromJson(text, Message.class);

                    if (receivedMessage.getIsSystemMessage()) { // it is a system message
                        if (!Objects.equals(MyUser.getId(), receivedMessage.getSender())) {
                            if (text.contains("DISCONNECT//")) {
                                if (Objects.equals(selectedChatBoxUserId, receivedMessage.getSender()))
                                    Platform.runLater(() -> {
                                        setConnectedLabelOff();
                                    });
                            } else if (text.contains("CONNECT//")) {
                                if (Objects.equals(selectedChatBoxUserId, receivedMessage.getSender()))
                                    Platform.runLater(() -> {
                                        setConnectedLabelOn();
                                    });
                            } else if (text.contains("USERNAME//CHANGED//")) { //!@#
                                Platform.runLater(() -> {
                                    // Holding on to the old name and changing the title to the new name
                                    String newName = receivedMessage.getText().split("//")[2];

                                    if (Objects.equals(selectedChatBoxUserId, receivedMessage.getSender())) {
                                        // The person we are chatting with has changed their name, update the chat name label and the selected chat box name
                                        chatNameLabel.setText(newName);
                                        ((Label) (selectedChatBoxPane.getChildren()).get(0)).setText(newName);

                                    } else {
                                        // Find the correct chat box pane (on the right side) and rename the name label on it
                                        String chatId = Database.compareStrings(receivedMessage.getSender(), MyUser.getId());
                                        System.out.println("Looking for: " + chatId);
                                        for (Pane chatPane : MyUser.userChats.keySet()) {
                                            System.out.println("#: " + chatPane.getId());
                                            if (chatPane.getId().contains(chatId))
                                                ((Label) (chatPane.getChildren()).get(0)).setText(newName);
                                        }
                                    }
                                });
                            } else if (text.contains("NEW//CHAT//CREATED//")) { // This case is not yet implemented
                                Platform.runLater(() -> {
                                    String currentChat = selectedChatBoxPane.getId();
                                    if (addUsersChatsToScreen(Database.getUsersChatsFromDatabase(MyUser.getId()))) { // look at this part, its strange, note sure about it.
                                        for (Node child : historyVBox.getChildren()) {
                                            if (Objects.equals(child.getId(), currentChat)) {
                                                System.out.println("Inside the if statement");
                                                selectedChatBoxPane = (Pane) child;
                                                selectedChatBoxPane.setStyle("-fx-border-color: skyblue; -fx-border-radius: 15px; -fx-border-width: 0.5px");
                                            }
                                        }
                                    }
                                });
                            } else if (text.contains("USERNAME//DELETED//")) { //!@#
                                Platform.runLater(() -> {
                                    String currentChat = selectedChatBoxPane.getId();
                                    if (Objects.equals(selectedChatBoxUserId, receivedMessage.getSender())) {
                                        cleanChatBubbles();
                                        currentChat = null;
                                    }
                                    if (addUsersChatsToScreen(Database.getUsersChatsFromDatabase(MyUser.getId()))) { // This resets the selectedChatBoxPane
                                        if (currentChat != null) {
                                            for (Node child : historyVBox.getChildren()) {
                                                if (Objects.equals(child.getId(), currentChat)) {
                                                    System.out.println("Inside the if statement");
                                                    selectedChatBoxPane = (Pane) child;
                                                    selectedChatBoxPane.setStyle("-fx-border-color: skyblue; -fx-border-radius: 15px; -fx-border-width: 0.5px");
                                                }
                                            }
                                        } else {
                                            selectedChatBoxUserId = null;
                                            selectedChatBoxPane = null;
                                            connectedLabel.setText("");// this does not seem to take effect
                                        }
                                    }
                                });
                            }
                        }
                    }
                    else {
                        // Explanation: we will know that the current user is looking at the chat where the message was received if the sender of the message
                        // AND the chatNameLabel.getText() are equal, there for the current user is looking at the chat when the message is received

                        if (Objects.equals(MyUser.getId(),receivedMessage.getSender()) || Objects.equals(selectedChatBoxUserId, receivedMessage.getSender())) {
                            Platform.runLater(() -> {
                                try {
                                    // Creating a new message bubble on the screen and adding the users text into it
                                    FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("/views/chatBubbleComponent.fxml"));
                                    HBox chatBubble = fxmlLoader.load();

                                    ChatBubbleController controller = fxmlLoader.getController();
                                    controller.setMessageBubbleLabel(receivedMessage.getText());
                                    controller.setMessage(receivedMessage);


                                    if (Objects.equals(receivedMessage.getSender(), MyUser.getId())) {
                                        controller.setMessageBubbleLabelColorBlue();
                                        chatBubble.setAlignment(Pos.CENTER_RIGHT);
                                    }
                                    else
                                        chatBubble.setAlignment(Pos.CENTER_LEFT);

                                    // Add message bubble to the screen
                                    chatVBox.getChildren().add(chatBubble);

                                    // adding the new message to the list of messages for this user
                                    if (!Objects.equals(MyUser.getId(),receivedMessage.getSender()))
                                        MyUser.getUserChats().get(selectedChatBoxPane).getMessages().add(receivedMessage);

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
                            showAlertWithMessage(Alert.AlertType.ERROR, "Error", MyUser.getName() + " has disconnected from the ChatServer; Code: " + code + " " + reason + "\n");
                            closeChatAppWindow();
                        }
                        System.out.println("Close method of ClientWebSocket was called.");
                    });
                }
            };

            if (Database.getSessionByUserId(MyUser.getId()) == null) {
                if (Database.addSessionToDataBase(MyUser.getId(), session)) // adding the connecting to the database to keep track of connected users
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



    public void closeChatAppWindow() {
        Stage stage = (Stage) getCurrentScene().getWindow();
        stage.close();
        if (MyUser != null)
            if (!Database.removeUserSession(MyUser.getId()))
                System.out.println("Could not complete the user session removal form the database. Error in closeChatAppWindow function in MainViewController.");

        System.exit(0);
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

    public void cleanChatBoxes() {
        historyVBox.getChildren().clear();
    }

    public void cleanChatBubbles() {
        chatVBox.getChildren().clear();
        setChatNameLabel("");
        setConnectedLabelOff();
    }

    public void openLoginTab() {
        loginAndSignupPane.setVisible(true);
        theLoginTab.setExpanded(true);
    }

    private void showUserTab() {
        userUsernameLabel.setText(MyUser.getName());
        theUserTab.setVisible(true);
        theUserTab.setExpanded(true);
        try {
            userPictureImageView.setImage(new Image(getClass().getResource("/images/" + MyUser.getUserImage()).toExternalForm()));
        } catch (NullPointerException e) {
            userPictureImageView.setImage(new Image("/images/male.png"));
            showAlertWithMessage(Alert.AlertType.ERROR, "User Image Error", "There was an error when loading your user image.");
            e.printStackTrace();
        }
    }

    private boolean addUsersChatsToScreen(ArrayList<RegularChat> usersRegularChats) {
        if (usersRegularChats == null) return false;

        cleanChatBoxes(); // clean the chats of the left if there are any

        for (RegularChat regularChat : usersRegularChats) {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("/views/chatBoxComponent.fxml"));
                Pane chatBoxPane = fxmlLoader.load();
                chatBoxPane.setId(Database.compareStrings(regularChat.getSender(), regularChat.getReceiver()) + "_" + regularChat.getConversation_id());
                chatBoxPane.setCursor(Cursor.HAND);

                ChatBoxController controller = fxmlLoader.getController();
                controller.setMainViewControllerReference(this);
                controller.setNameLabel(Database.getUsernameById(regularChat.getReceiver()));

                regularChat.setMessages(Database.getChatMessagesFromDatabase(chatBoxPane.getId())); // loading the messages from the database
                if (!Objects.equals(regularChat.getMessages().get(0).getSender(), "ERROR")) { // based on the implementation of the catch block of "Database.getChatMessagesFromDatabase"
                    getHistoryVBox().getChildren().add(chatBoxPane);
                    MyUser.addChatToUser(chatBoxPane, regularChat); // adding the pane - chat reference to the hashmap for later use
                }

            } catch (IOException e) {
                showAlertWithMessage(Alert.AlertType.ERROR,"Error Loading Messages", "Could not load some messages from " + regularChat.getReceiver() + ". Please try again later.");
                return false;
            }
        }
        return true;
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

    public String getSelectedChatBoxUserId() { return selectedChatBoxUserId; }

    public void setSelectedChatBoxUserId(String selectedChatBoxName) { this.selectedChatBoxUserId = selectedChatBoxName; }

    public int getTemporaryChatBoxCounter() { return temporaryChatBoxCounter; }

    public void setTemporaryChatBoxCounter(int temporaryChatBoxCounter) { this.temporaryChatBoxCounter = temporaryChatBoxCounter; }

    public void setConnectedLabelOn() { connectedLabel.setText("connected"); }
    public void setConnectedLabelOff() { connectedLabel.setText("disconnected"); }
    public void setConnectedLabelEmpty() { connectedLabel.setText(""); }

    public String getConnectedLabel() { return connectedLabel.getText(); }

    public Scene getCurrentScene() { return profileButton.getScene();}

}
