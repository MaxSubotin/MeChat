package app.mechat;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class ChatBoxController {


    @FXML
    Label nameLabel;
    @FXML
    Pane chatBoxPane;

    private MainViewController mainViewControllerReference;

    @FXML
    public void chatBoxOnClick(MouseEvent event) {
        // Removing old border
        Pane oldChatBox = mainViewControllerReference.getSelectedChatBoxPane();
        if (oldChatBox != null) {
            oldChatBox.setStyle("-fx-border-color: black; -fx-border-radius: 15px; -fx-border-width: 0.5px");
            mainViewControllerReference.cleanChatBubbles();
        }

        // Creating and adding new border to the selected chat
        Pane newChatBox = (Pane) event.getSource();
        newChatBox.setStyle("-fx-border-color: skyblue; -fx-border-radius: 15px; -fx-border-width: 0.5px");
        mainViewControllerReference.setSelectedChatBoxPane(newChatBox);
        mainViewControllerReference.setSelectedChatBoxName(nameLabel.getText());

        // Adding a listener to scroll down whenever there is a new message or messages are loaded (or just any change to the vbox)
        mainViewControllerReference.chatVBox.heightProperty().addListener(
                (ChangeListener) (observable, oldvalue, newValue) ->
                        mainViewControllerReference.chatScrollPane.setVvalue(1.0));

        // Adding the chat name to the screen and making it visible
        mainViewControllerReference.setChatNameLabel(nameLabel.getText());
        mainViewControllerReference.turnChatVisible();

        // Loading the messages
        addMessageBubbles();

    }

    public void addMessageBubbles() {
        String[] splitChatBoxName = mainViewControllerReference.getSelectedChatBoxPane().getId().split("_");
        ArrayList<Message> listOfMessages = Database.getChatMessagesFromDatabase(Integer.parseInt(splitChatBoxName[1]));

        try {
            // For each message in the chat we make a new bubble and add the text into it
            for (Message message: listOfMessages) {
                FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("chatBubble.fxml"));
                HBox chatBubblePane = fxmlLoader.load();
                ChatBubbleController controller = fxmlLoader.getController();
                controller.setMessageBubbleLabel(message.getText());
                controller.setMessage(message);

                // If the message was from the current user then we make it blue and positioned on the right side
                if (Objects.equals(message.getSender(), mainViewControllerReference.MyUser.getName())) {
                    controller.setMessageBubbleLabelColorBlue();
                    chatBubblePane.setAlignment(Pos.CENTER_RIGHT);
                }
                mainViewControllerReference.getChatVBox().getChildren().add(chatBubblePane);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @FXML
    public void setNameLabel(String name) {
        nameLabel.setText(name);
    }


    public void setMainViewControllerReference(MainViewController mainViewControllerReference) {
        this.mainViewControllerReference = mainViewControllerReference;
    }

    public MainViewController getMainViewControllerReference() {
        return mainViewControllerReference;
    }
}
