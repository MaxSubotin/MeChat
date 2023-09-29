package app.mechat;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;

public class MainViewController {

    public User MyUser = null;
    private Pane selectedChatBoxPane;
    private String selectedChatBoxName;
    private int newChatBoxCounter = 0;

    @FXML
    ImageView profileButton, newChatButton, settingsButton, sendMessageButton, userPictureImageView;
    @FXML
    VBox historyVBox, chatVBox;
    @FXML
    TextField messageTextField, loginUsernameField, loginPasswordField, signupUsernameField, signupPhoneNumberField, signupPasswordField;
    @FXML
    Label chatNameLabel, userUsernameLabel, userPhoneNumberLabel;
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
        chatScrollPane.setVvalue(1.0); // scrolls down

        // Getting the text that the user inputted
        String text = messageTextField.getText();
        if (text == null || text.isEmpty()) return;

        // Creating a new message bubble on the screen and adding the users text into it
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("chatBubble.fxml"));
            HBox chatBubble = fxmlLoader.load();
            chatBubble.setAlignment(Pos.CENTER_RIGHT); // The users messages appear of the right side

            // Get current timestamp
            LocalDateTime currentTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTime = currentTime.format(formatter);

            // Create a message object and the controller to access methods
            Message message = new Message(text, MyUser.getName(), formattedTime);

            ChatBubbleController controller = fxmlLoader.getController();
            controller.setMessageBubbleLabel(text);
            controller.setMessageBubbleLabelColorBlue(); // The users messages are colored sky-blue
            controller.setMessage(message);

            // Add message bubble to the screen and to the database
            chatVBox.getChildren().add(chatBubble);
            String[] splitChatBoxName = this.getSelectedChatBoxPane().getId().split("_");
            Database.addMessageToDatabase(message, Integer.parseInt(splitChatBoxName[1]));

            messageTextField.clear(); // Removing the text from the textfield
            getFocus();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        // Get the text and see if it matches the database, if it does then login into the user
        User user = Database.getUserFromDatabase(loginUsernameField.getText(), loginPasswordField.getText());
        if (user != null) MyUser = user;
        else return;

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

}
