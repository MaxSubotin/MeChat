package controllers;

import database.Database;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

import at.favre.lib.crypto.bcrypt.BCrypt;

import util.*;
import websockets.CustomWebSocketClient;


public class MainViewController {

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    // - - - - - - - - - - Variable Declaration  - - - - - - - - - - //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //

    public User MyUser = null;
    public CustomWebSocketClient webSocketClient;

    private int temporaryChatBoxCounter = 0;
    public RegularChat currentRegularChat = null;
    public Pane selectedChatBoxPane;
    public String selectedChatBoxUserId;

    private HBox selectedAvatarHbox;

    @FXML
    AnchorPane root;
    @FXML
    ImageView profileButton, newChatButton, settingsButton, sendMessageButton, userPictureImageView, male_AvatarImage, female_AvatarImage;
    @FXML
    VBox historyVBox, chatVBox;
    @FXML
    HBox settingsDeleteAccountButton;
    @FXML
    TextField messageTextField, settingsUsernameField, settingsPasswordField;
    @FXML
    Label chatNameLabel, userUsernameLabel, connectedLabel;
    @FXML
    Pane loginAndSignupPane, settingsPane;
    @FXML
    Button settingsUsernameSaveButton, settingPasswordSaveButton, settingsCloseButton, settingAvatarSaveButton;
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
                male_AvatarImage.setStyle("-fx-border-color: skyblue; -fx-border-radius: 30px; -fx-border-width: 3px");
            else
                female_AvatarImage.setStyle("-fx-border-color: skyblue; -fx-border-radius: 30px; -fx-border-width: 3px");
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
        if (RegexChecker.isValidPassword(settingsPasswordField.getText())) {
            if (!MyUser.updatePassword(BCrypt.withDefaults().hashToString(12, settingsPasswordField.getText().toCharArray())))
                return;
            settingsPasswordField.clear();
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
            userUsernameLabel.setText(MyUser.getName());
            try {
                userPictureImageView.setImage(new Image(getClass().getResource("/images/" + MyUser.getUserImage()).toExternalForm()));
            } catch (NullPointerException e) {
                userPictureImageView.setImage(new Image("/images/male.png"));
                showAlertWithMessage(Alert.AlertType.ERROR, "User Image Error", "There was an error when loading your user image.");
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void closeProfileButtonOnClick() {
        loginAndSignupPane.setVisible(false);
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
    }


    // Helper Functions

    public void closeChatAppWindow() {
        Stage stage = (Stage) getCurrentScene().getWindow();
        stage.close();
        if (MyUser != null)
            if (!Database.removeUserSession(MyUser.getId()))
                System.out.println("Could not complete the user session removal form the database. Error in closeChatAppWindow function in MainViewController.");

        System.exit(0);
    }

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

    public void setChatNameLabel(String name) { this.chatNameLabel.setText(name); }

    public Pane getSelectedChatBoxPane() { return selectedChatBoxPane; }

    public void setSelectedChatBoxPane(Pane chatBoxPane) { this.selectedChatBoxPane = chatBoxPane; }

    public void setSelectedChatBoxUserId(String selectedChatBoxName) { this.selectedChatBoxUserId = selectedChatBoxName; }

    public void setTemporaryChatBoxCounter(int temporaryChatBoxCounter) { this.temporaryChatBoxCounter = temporaryChatBoxCounter; }

    public void setConnectedLabelOn() { connectedLabel.setText("connected"); }

    public void setConnectedLabelOff() { connectedLabel.setText("disconnected"); }

    public Scene getCurrentScene() { return profileButton.getScene();}

}
