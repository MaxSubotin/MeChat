package controllers;

import database.Database;
import database.DatabaseConfig;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
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
    public Chat currentChat = null;
    public Pane selectedChatBoxPane;

    private HBox selectedAvatarHbox;

    @FXML
    AnchorPane mainView;
    @FXML
    ImageView profileButton, newChatButton, newGroupChatButton, settingsButton, sendMessageButton, userPictureImageView, male_AvatarImage, female_AvatarImage, male_GroupImage, female_GroupImage;
    @FXML
    VBox historyVBox, chatVBox;
    @FXML
    HBox settingsDeleteAccountButton, infoDeleteGroupButton, infoEditGroupNameHBox, infoChangeGroupIconHBox, infoAddGroupMemberHBox, infoDeleteGroupHBox;
    @FXML
    TextField messageTextField, settingsUsernameField, settingsPasswordField, infoNewGroupNameField, infoNewGroupMemberNameField;
    @FXML
    Label chatNameLabel, userUsernameLabel, connectedLabel, infoGroupNameLabel, infoAdminNameLabel, infoAdminPanelLabel;
    @FXML
    Pane loginAndSignupPane, settingsPane, groupInfoPane;
    @FXML
    Button settingsUsernameSaveButton, settingPasswordSaveButton, settingsCloseButton, settingAvatarSaveButton, infoNewGroupNameSaveButton, infoGroupIconSaveButton, infoGroupPaneCloseButton, infoNewGroupMemberAddButton;
    @FXML
    ScrollPane chatScrollPane, adminPane;
    @FXML
    ListView infoGroupMembersList;

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    // - - - - - - - - - Button OnClick Functions - - - - - - - - - //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //

    @FXML
    public void newChatButtonOnClick(MouseEvent event) {
        // Counting the new chat boxes, only one on the screen at a time
        if (temporaryChatBoxCounter > 0) return;
        if (MyUser == null) return;

        // Adding a new chat box to the screen, waiting for the user to input the receiver's name/number
        try {
            // Add the chat box to the screen
            FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("/views/temporaryChatBoxComponent.fxml"));
            Pane temporaryChatBoxComponent = fxmlLoader.load();
            temporaryChatBoxCounter++;

            TemporaryChatBoxController controller = fxmlLoader.getController();
            controller.setMainViewControllerReference(this); // Giving a reference to the main controller for communication

            String buttonId = ((ImageView) event.getSource()).getId();
            if (Objects.equals(buttonId, "newGroupChatButton")) {
                controller.isGroupChat = true;
                controller.nameTextField.setPromptText("enter a group name");
            }

            historyVBox.getChildren().add(temporaryChatBoxComponent);
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

        String message = (currentChat instanceof GroupChat) ? MyUser.getName() + ":\n" + text : text;

        if (!currentChat.sendMessage(webSocketClient, message)) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not send the message", "There was a problem sending the message, please try again later.");
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
        String oldUsername = MyUser.getName();
        String newUsername = settingsUsernameField.getText();

        // Check new username with regex
        if (!RegexChecker.isValidUsername(newUsername)) {
            // Show and error message
            showAlertWithMessage(Alert.AlertType.ERROR,"Error", "Your new username, " + newUsername + " is not good enough.\nTry a different one.");
            settingsUsernameField.clear();
            return;
        }

        // Check newUsername in database and update the database
        if (MyUser.setName(newUsername)) {
            webSocketClient.sendMessageToServer(new SystemMessage(
                    oldUsername + "//" + newUsername,
                    MyUser.getId(),
                    Message.createTimestamp(),
                    "-1",
                    SystemMessageType.USERNAME_CHANGED
            ));
        } else {
            showAlertWithMessage(Alert.AlertType.ERROR,"Error", newUsername + " is already taken.\nTry a different username.");
        }

        settingsUsernameField.clear();
    }

    @FXML
    public void settingsPasswordSaveButtonOnClick() {
        // Check new password with regex
        if (RegexChecker.isValidPassword(settingsPasswordField.getText())) {
            if (!MyUser.updatePassword(BCrypt.withDefaults().hashToString(12, settingsPasswordField.getText().toCharArray()))) {
                settingsPasswordField.clear();
                return;
            }
        } else {
            // Show and error message
            showAlertWithMessage(Alert.AlertType.ERROR,"Error", "Your new password is not good enough.\nTry a different one.");
        }
        settingsPasswordField.clear();
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
                webSocketClient.sendMessageToServer(new SystemMessage(
                        MyUser.getName(),
                        MyUser.getId(),
                        Message.createTimestamp(),
                        "-1",
                        SystemMessageType.USER_DELETED
                ));
            else
                System.out.println("Could not properly delete your user from the database, please try again later.");

            logoutButtonOnClick();
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
    public void logoutButtonOnClick() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/loginSignupView.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 900, 600, Color.web("rgba(0, 0, 0, 0.75)"));
            LoginSignupViewController controller = fxmlLoader.getController();

            Stage currentStage = (Stage) mainView.getScene().getWindow();
            controller.setPrimaryStage(currentStage);
            currentStage.setScene(scene);

            webSocketClient.close();

        } catch (IOException e) {
            e.printStackTrace();
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
    }


    @FXML
    public void infoNewGroupNameSaveButtonOnClick() {
        String newGroupName = infoNewGroupNameField.getText();
        if (newGroupName.isEmpty()) return;

        if (Database.updateGroupName(currentChat.getChatId(), newGroupName)) {
            infoGroupNameLabel.setText(newGroupName);
            setChatNameLabel(newGroupName);

            ((GroupChat)currentChat).setGroupName(newGroupName);
            ((Label) ((HBox) (selectedChatBoxPane.getChildren()).get(0)).getChildren().get(1)).setText(newGroupName);

            infoNewGroupNameField.clear();

            if (!currentChat.sendMessage(webSocketClient, "--< The group name was changed to: " + newGroupName + " >--")) {
                showAlertWithMessage(Alert.AlertType.ERROR, "Error in renaming group", "There was a problem sending a message about the renaming of the group, please try again later.");
                return;
            }

            webSocketClient.sendMessageToServer(new SystemMessage(
                    newGroupName,
                    MyUser.getId(),
                    Message.createTimestamp(),
                    currentChat.getChatId(),
                    SystemMessageType.GROUP_CHAT_RENAMED
            ));
        }
    }

    @FXML
    public void infoGroupIconSaveButtonOnClick() {}

    @FXML
    public void infoDeleteGroupOnClick() {
        if (Database.deleteGroupChat(currentChat.getChatId())) {
            String groupName = chatNameLabel.getText();
            MyUser.userChats.remove(selectedChatBoxPane);
            historyVBox.getChildren().remove(selectedChatBoxPane);
            cleanChatBubbles();
            setConnectedLabelText("");

            webSocketClient.sendMessageToServer(new SystemMessage(
                    groupName,
                    MyUser.getId(),
                    Message.createTimestamp(),
                    currentChat.getChatId(),
                    SystemMessageType.ADMIN_DELETED_GROUP
            ));

            currentChat = null;
            selectedChatBoxPane = null;
            closeInfoGroupPaneButtonOnClick();
        }
    }

    @FXML
    public void openInfoGroupPaneButtonOnClick() {
        if (currentChat instanceof GroupChat) {
            if (Objects.equals(MyUser.getId(), ((GroupChat) currentChat).getAdmin()))
                enableAdminInfoPane();

            loadGroupChatParticipants(currentChat.getChatId());
            infoGroupNameLabel.setText(Database.getGroupNameByGroupId(currentChat.getChatId()));
            infoAdminNameLabel.setText(Database.getUsernameById(((GroupChat) currentChat).getAdmin()));
            groupInfoPane.setVisible(true);
        }
    }

    @FXML
    public void closeInfoGroupPaneButtonOnClick() {
        groupInfoPane.setVisible(false);
        disableAdminInfoPane();
        adminPane.setVvalue(0);
    }

    @FXML
    public void infoNewGroupMemberAddButtonOnClick() {
        String newUserName = infoNewGroupMemberNameField.getText();
        String newUserId = Database.getUserIdByUsername(newUserName);

        ObservableList<String> currentItems = infoGroupMembersList.getItems();
        if (currentItems.contains(newUserName) ||
                Objects.equals(newUserId, ((GroupChat) currentChat).getAdmin())) {
            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "User already in group", "The user you are tring to add is already in the group.");
            return;
        }

        if (Database.addGroupChatParticipant(currentChat.getChatId(), newUserId)) {
            currentItems.add(newUserName);
            infoGroupMembersList.setItems(currentItems);
            ((GroupChat)(currentChat)).getReceivers().add(newUserId);

            if (!currentChat.sendMessage(webSocketClient, "--< " + newUserName + " has been added to the group chat! >--")) {
                showAlertWithMessage(Alert.AlertType.ERROR, "Error in adding a member to group", "There was a problem sending a message about the new member of the group, please try again later.");
                return;
            }

            webSocketClient.sendMessageToServer(new SystemMessage(
                    newUserId + "//" + newUserName + "//" + chatNameLabel.getText(),
                    MyUser.getId(),
                    Message.createTimestamp(),
                    currentChat.getChatId(),
                    SystemMessageType.INVITE_TO_GROUP_CHAT
            ));

        }

        infoNewGroupMemberNameField.clear();
    }


    // Helper Functions

    public void loadGroupChatParticipants(String groupId) {
        infoGroupMembersList.getItems().clear();
        try (Connection conn = DatabaseConfig.getConnection()) {
            ArrayList<String> groupChatParticipants = Database.getGroupChatParticipants(conn, groupId);
            if (groupChatParticipants != null && !groupChatParticipants.isEmpty()) {
                ObservableList<String> currentItems = infoGroupMembersList.getItems();

                for (String participant: groupChatParticipants) {
                    String participantUsername = Database.getUsernameById(participant);
                    currentItems.add(Objects.equals(participant, ((GroupChat) currentChat).getAdmin()) ?
                            participantUsername + " - admin" :
                            participantUsername);
                }
                infoGroupMembersList.setItems(currentItems);

                if (Objects.equals(((GroupChat)currentChat).getAdmin(), MyUser.getId()))
                    setContextMenu();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setContextMenu() {
        Platform.runLater(() -> {
            // Create a context menu with a "delete" menu item
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Remove user");
            deleteItem.setStyle("-fx-text-fill: whitesmoke;");
            deleteItem.setText("Remove user");
            contextMenu.getItems().add(deleteItem);
            contextMenu.setId("listItemContextMenu");

            // Set the context menu for the list view
            infoGroupMembersList.setCellFactory(param -> {
                ListCell<String> cell = new ListCell<>();
                cell.textProperty().bind(cell.itemProperty());
                cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                    if (isNowEmpty || cell.getText() == null) {
                        cell.setContextMenu(null);
                    } else {
                        if (cell.getText().endsWith(" - admin")) {
                            cell.setContextMenu(null); // Do not add context menu for items ending with " - admin"
                        } else {
                            cell.setContextMenu(contextMenu);
                        }
                    }
                });
                return cell;
            });


            // Add an action event handler for the "Delete" menu item
            deleteItem.setOnAction(event -> {
                String usernameToRemove = (String) infoGroupMembersList.getSelectionModel().getSelectedItem();
                String userIdToRemove = Database.getUserIdByUsername(usernameToRemove);

                if (userIdToRemove != null) {
                    if (Database.removeGroupChatParticipant(currentChat.getChatId(), userIdToRemove)) {
                        ((GroupChat) currentChat).getReceivers().remove(userIdToRemove);

                        if (!currentChat.sendMessage(webSocketClient, "--< " + usernameToRemove + " has been removed from the group >--")) {
                            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error removing a user", "Could not send a message about removing a user form the group, try again later.");
                            return;
                        }

                        webSocketClient.sendMessageToServer(new SystemMessage(
                                userIdToRemove + "//" + usernameToRemove,
                                MyUser.getId(),
                                Message.createTimestamp(),
                                currentChat.getChatId(),
                                SystemMessageType.ADMIN_REMOVED_GROUP_USER
                        ));

                        infoGroupMembersList.getItems().remove(usernameToRemove);
                    }
                }
            });



        });
    }

    public void closeChatAppWindow() {
        Stage stage = (Stage) getCurrentScene().getWindow();
        stage.close();
        if (MyUser != null)
            if (!Database.removeUserSession(MyUser.getId()))
                System.out.println("Could not complete the user session removal form the database. Error in closeChatAppWindow function in MainViewController.");

        System.exit(0);
    }

    public void disableAdminInfoPane() {
        infoAdminPanelLabel.setDisable(true);
        infoEditGroupNameHBox.setDisable(true);
        infoChangeGroupIconHBox.setDisable(true);
        infoAddGroupMemberHBox.setDisable(true);
        infoDeleteGroupHBox.setDisable(true);
    }

    public void enableAdminInfoPane() {
        infoAdminPanelLabel.setDisable(false);
        infoEditGroupNameHBox.setDisable(false);
        infoChangeGroupIconHBox.setDisable(false);
        infoAddGroupMemberHBox.setDisable(false);
        infoDeleteGroupHBox.setDisable(false);
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

    public void addChatBubbleToScreen(Pane chatBubblePane) {
        chatVBox.getChildren().add(chatBubblePane);
    }

    public Pane createChatBoxPaneComponent(String receiverName, String receiverImage, String chatId) {
        try {
            // Create the conversation chat box
            FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("/views/chatBoxComponent.fxml"));
            Pane chatBoxPane = fxmlLoader.load();

            // Setting the name of the contact and an on-click event
            ChatBoxController controller = fxmlLoader.getController();
            controller.initChatBox(receiverName, receiverImage, this);

            chatBoxPane.setOnMouseClicked(controller::chatBoxOnClick);
            chatBoxPane.setCursor(Cursor.HAND);
            chatBoxPane.setId(chatId);
            return chatBoxPane;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void showAlertWithMessage(Alert.AlertType type, String title, String errorMessage) {
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

    //public void setSelectedChatBoxUserId(String selectedChatBoxName) { this.selectedChatBoxUserId = selectedChatBoxName; }

    public void setTemporaryChatBoxCounter(int temporaryChatBoxCounter) { this.temporaryChatBoxCounter = temporaryChatBoxCounter; }

    public void setConnectedLabelOn() { connectedLabel.setText("connected"); }

    public void setConnectedLabelOff() { connectedLabel.setText("disconnected"); }
    public void setConnectedLabelText(String text) { connectedLabel.setText(text); }

    public Scene getCurrentScene() { return profileButton.getScene();}

    public ListView getInfoGroupMembersList() { return infoGroupMembersList; }

}
