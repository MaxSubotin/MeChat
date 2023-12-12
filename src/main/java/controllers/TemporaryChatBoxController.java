package controllers;

import database.Database;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import util.Message;
import util.RegularChat;

import java.io.IOException;
import java.sql.SQLOutput;
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

    private void addRegularChat(String tempName) {
        // Find the userId of the person we want to chat with
        String tempNameId = Database.getUserIdByUsername(tempName);
        if (tempNameId == null) {
            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "User not found", "Could not find a user with the name " + tempName + ", try a different one.");
            return; // user was not found, do nothing.
        }

        // Check if there is already a chat with that person
        for (Pane chatBoxPane: MVCR.MyUser.userChats.keySet()) {
            RegularChat chat = MVCR.MyUser.userChats.get(chatBoxPane);
            if (Objects.equals(tempNameId, chat.getReceiver())) {
                MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Chat already exists", "Could not create chat Pane in the ui because it already exists, try again later.");
                return;
            }
        }

        Pane chatBoxPane = MVCR.createChatBoxPaneComponent(tempName, tempNameId);
        if (chatBoxPane == null) {
            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error in creating chat Pane", "Could not create chat Pane in the ui, try again later.");
            return;
        }

        // Removing the temporary chat box and adding the new one
        removeLastElement();
        MVCR.getHistoryVBox().getChildren().add(chatBoxPane);

        // Add the chat to the users database
        int id = Database.addConversationToDatabase(MVCR.MyUser.getId() ,tempNameId);
        if (id != -1) {

            MVCR.finalizeChatBoxComponentCreation(tempNameId, id, chatBoxPane);
            MVCR.webSocketClient.sendMessageToServer(MVCR.MyUser, "NEW//CHAT//CREATED//" + MVCR.MyUser.getName() + "//" + MVCR.MyUser.getId() + "//" + id);
        } else {
            removeLastElement();
            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR,"Error","Could not create the conversation.");
        }
    }

    private void addGroupChat(String tempName) {

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
