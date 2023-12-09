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

public class TemporaryChatBoxController {

    private MainViewController MVCR;

    @FXML
    TextField nameTextField;
    @FXML
    Button confirmButton, cancelButton;


    @FXML
    public void confirmButtonOnClick() {
        // Creating the actual chat box of the left side
        try {
            // Get the username of who we want to contact
            String tempName = this.getNameTextField();
            if (tempName.isEmpty()) return;

            // Find the userId of the person we want to chat with
            String tempNameId = Database.getUserIdByUsername(tempName);
            if (tempNameId == null) {
                MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "User not found", "Could not find a user with the name " + tempName + ", try a different one.");
                return; // user was not found, do nothing.
            }

            // Create the conversation chat box
            FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("/views/chatBoxComponent.fxml"));
            Pane chatBoxPane = fxmlLoader.load();

            // Setting the name of the contact and an on-click event
            ChatBoxController controller = fxmlLoader.getController();
            controller.initChatBox(tempName, tempNameId, MVCR);

            chatBoxPane.setOnMouseClicked(controller::chatBoxOnClick);
            chatBoxPane.setCursor(Cursor.HAND);

            // Removing the temporary chat box and adding the new one
            removeLastElement();
            MVCR.getHistoryVBox().getChildren().add(chatBoxPane);

            // Add the chat to the users database
            int id = Database.addConversationToDatabase(MVCR.MyUser.getId() ,tempNameId);
            if (id != -1) {
                String conversationName = Database.compareStrings(MVCR.MyUser.getId(), tempNameId) + "_" + id;
                chatBoxPane.setId(conversationName);

                String otherParticipant = conversationName.split("_")[0].equals(MVCR.MyUser.getId())
                        ? conversationName.split("_")[1]
                        : conversationName.split("_")[0];

                MVCR.MyUser.addChatToUser(
                        chatBoxPane,
                        new RegularChat(
                                new ArrayList<Message>(),
                                MVCR.MyUser.getId(),
                                otherParticipant,
                                id
                        )
                );
            } else {
                removeLastElement();
                MainViewController.showAlertWithMessage(Alert.AlertType.ERROR,"Error","Could not create the conversation.");
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void cancelButtonOnClick() {
        removeLastElement();
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
