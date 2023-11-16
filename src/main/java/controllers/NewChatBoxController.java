package controllers;

import database.Database;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import util.Message;
import util.RegularChat;

import java.io.IOException;
import java.util.ArrayList;

public class NewChatBoxController {

    @FXML
    TextField nameTextField;
    @FXML
    Button confirmButton, cancelButton;

    private MainViewController mainViewControllerReference;

    public String getNameTextField() {
        return nameTextField.getText();
    }

    @FXML
    public void confirmButtonOnClick() {
        // Creating the actual chat box of the left side
        try {
            // Get the username of who we want to contact
            String tempName = this.getNameTextField();

            // Find the userId of the person we want to chat with
            String tempNameId = Database.getUserIdByUsername(tempName);
            if (tempNameId == null) return; // user was not found, do nothing. // TODO: add error message

            // Create the conversation chat box
            FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("/views/chatBoxComponent.fxml"));
            Pane chatBoxPane = fxmlLoader.load();

            // Setting the name of the contact and an on-click event
            ChatBoxController controller = fxmlLoader.getController();
            controller.setNameLabel(tempName);
            chatBoxPane.setOnMouseClicked(controller::chatBoxOnClick);
            chatBoxPane.setCursor(Cursor.HAND);

            // Removing the temporary chat box and adding the new one
            int tempSize = mainViewControllerReference.getHistoryVBox().getChildren().size();
            mainViewControllerReference.getHistoryVBox().getChildren().remove(tempSize-1);
            mainViewControllerReference.getHistoryVBox().getChildren().add(chatBoxPane);
            mainViewControllerReference.setNewChatBoxCounter(0); // Reducing the counter so that the user could add another chat
            mainViewControllerReference.getFocus();

            // Giving a reference to the main controller for communication
            controller.setMainViewControllerReference(mainViewControllerReference);

            // Add the chat to the users database
            int id = Database.addConversationToDatabase(mainViewControllerReference.MyUser.getId() ,tempNameId);
            if (id != -1) {
                String conversationName = Database.compareStrings(mainViewControllerReference.MyUser.getId(), tempNameId) + "_" + id;
                chatBoxPane.setId(conversationName);
                mainViewControllerReference.userChats.put(chatBoxPane, new RegularChat(new ArrayList<Message>(),conversationName.split("_")[0], conversationName.split("_")[1],id));
            } else {
                tempSize = mainViewControllerReference.getHistoryVBox().getChildren().size();
                mainViewControllerReference.getHistoryVBox().getChildren().remove(tempSize-1);
                mainViewControllerReference.showAlertWithMessage(Alert.AlertType.ERROR,"Error","Could not create the conversation.");
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void cancelButtonOnClick() {
        int tempSize = mainViewControllerReference.getHistoryVBox().getChildren().size();
        mainViewControllerReference.getHistoryVBox().getChildren().remove(tempSize-1);
        mainViewControllerReference.getFocus();

    }


    // getters and setters

    public void setMainViewControllerReference(MainViewController mainViewControllerReference) { this.mainViewControllerReference = mainViewControllerReference; }

    public MainViewController getMainViewControllerReference() {
        return mainViewControllerReference;
    }
}
