package controllers;

import database.Database;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import util.*;

import java.io.IOException;
import java.sql.SQLOutput;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Objects;

public class TemporaryChatBoxController {

    private MainViewController MVCR;
    public boolean isGroupChat = false;

    @FXML
    TextField nameTextField;
    @FXML
    Button confirmButton, cancelButton;


    @FXML
    public void confirmButtonOnClick() {
        String tempName = this.getNameTextField();
        if (tempName.isEmpty()) return;

        if (!isGroupChat) addRegularChat(tempName);
        else addGroupChat(tempName);
    }

    @FXML
    public void cancelButtonOnClick() {
        removeLastElement();
    }

    private void addRegularChat(String otherUsername) {
        // Find the userId of the person we want to chat with
        String otherUserId = Database.getUserIdByUsername(otherUsername);
        if (otherUserId == null) {
            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "User not found", "Could not find a user with the name " + otherUsername + ", try a different one.");
            return; // user was not found, do nothing.
        }

        if (Objects.equals(otherUserId, MVCR.MyUser.getId())) {
            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Can't chat with yourself", "Can't create a chat with yourself, you can only chat with other users.");
            return; // user was not found, do nothing.
        }

        // Check if there is already a chat with that person
        Chat chatObject;
        for (Pane chatBoxPane: MVCR.MyUser.userChats.keySet()) {
            chatObject = MVCR.MyUser.userChats.get(chatBoxPane);
            if (chatObject instanceof RegularChat) {
                if (Objects.equals(otherUserId, ((RegularChat)chatObject).getReceiver())) {
                    MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Chat already exists", "Could not create chat Pane in the ui because it already exists, try again later.");
                    return;
                }
            }
        }
        chatObject = new RegularChat(new ArrayList<Message>(), MVCR.MyUser.getId(), otherUserId, IdGenerator.generateUniqueId(), 0);

        Pane chatBoxPane = MVCR.createChatBoxPaneComponent(otherUsername, otherUserId, chatObject.getChatId());
        if (chatBoxPane == null) {
            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error in creating chat Pane", "Could not create chat Pane in the ui, try again later.");
            return;
        }

        // Add the chat to the users database
        if (Database.addRegularChatToDatabase((RegularChat)chatObject)) {
            removeLastElement(); // Removing the temporary chat box
            MVCR.getHistoryVBox().getChildren().add(chatBoxPane);
            MVCR.MyUser.addChatToUser(chatBoxPane, chatObject);
            MVCR.webSocketClient.sendMessageToServer(new SystemMessage(
                    otherUserId + "//" + MVCR.MyUser.getName() + "//" + MVCR.MyUser.getUserImage(),
                    MVCR.MyUser.getId(),
                    Message.createTimestamp(),
                    chatObject.getChatId(),
                    SystemMessageType.REGULAR_CHAT_CREATED
            ));
        } else {
            removeLastElement();
            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR,"Error","Could not create the conversation.");
        }
    }

    private void addGroupChat(String groupName) {
        // Check if there is already a chat with that person
        if (!Database.isGroupNameUnique(groupName)) {
            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Group name is taken", "Could not create group because the name is taken, try a different group name.");
            return;
        }

        GroupChat chatObject = new GroupChat(new ArrayList<Message>(), MVCR.MyUser.getId(), new ArrayList<String>(), IdGenerator.generateUniqueId(), 0, groupName, MVCR.MyUser.getId());
        chatObject.getReceivers().add(MVCR.MyUser.getId());

        Pane chatBoxPane = MVCR.createChatBoxPaneComponent(groupName, null, chatObject.getChatId()); // 🔶 passing null will at the end cause the icon to be the default question-mark icon
        if (chatBoxPane == null) {
            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error in creating chat Pane", "Could not create chat Pane in the ui, try again later.");
            return;
        }

        // Add the chat to the users database
        if (Database.addGroupChatToDatabase(chatObject)) {
            removeLastElement(); // Removing the temporary chat box
            MVCR.getHistoryVBox().getChildren().add(chatBoxPane);
            MVCR.MyUser.addChatToUser(chatBoxPane, chatObject);


        } else {
            removeLastElement();
            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR,"Error","Could not create the conversation.");
        }
    }


    public void removeLastElement() {
        int tempSize = MVCR.getHistoryVBox().getChildren().size();
        MVCR.getHistoryVBox().getChildren().remove(tempSize-1);
        MVCR.setTemporaryChatBoxCounter(0); // Reducing the counter so that the user could add another chat later
        MVCR.getFocus();
    }

    // getters and setters

    public String getNameTextField() {
        return nameTextField.getText();
    }

    public void setMainViewControllerReference(MainViewController mainViewControllerReference) { MVCR = mainViewControllerReference; }

    public MainViewController getMainViewControllerReference() {
        return MVCR;
    }
}
