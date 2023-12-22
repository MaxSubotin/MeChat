package controllers;

import database.Database;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import util.Message;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import util.SystemMessage;
import util.SystemMessageType;

import java.util.Objects;

public class ChatBubbleController {

    @FXML
    Label messageBubbleLabel, leftTimestampLabel, rightTimestampLabel;
    @FXML
    HBox bubbleHBox;

    private Message message;
    public MainViewController MVCR;
    public boolean myMessageFlag = true;


    public void initMessageBubble(Message message, MainViewController mvcr) {
        setMessage(message);
        setMessageBubbleLabel(message.getText());

        if (!(message instanceof SystemMessage) && Objects.equals(message.getSender(), mvcr.MyUser.getId()))
            setContextMenu();
        setTimestamps(message.getTimestamp());
        MVCR = mvcr;
    }


    @FXML
    public void showTimeStampOnBubbleHover() {
        if (bubbleHBox.getAlignment() == Pos.CENTER_RIGHT) {
            leftTimestampLabel.setVisible(true);
            leftTimestampLabel.setPadding(new Insets(0,5,0,0));
        }
        else {
            rightTimestampLabel.setVisible(true);
            rightTimestampLabel.setPadding(new Insets(0,0,0,5));
        }
    }

    @FXML
    public void hideTimeStampOnBubbleHover() {
        if (bubbleHBox.getAlignment() == Pos.CENTER_RIGHT) {
            leftTimestampLabel.setVisible(false);
            leftTimestampLabel.setPadding(new Insets(0,0,0,0));
        }
        else {
            rightTimestampLabel.setVisible(false);
            rightTimestampLabel.setPadding(new Insets(0,0,0,0));
        }
    }

    // getters and setters

    public Label getMessageBubbleLabel() {
        return messageBubbleLabel;
    }

    public void setMessageBubbleLabel(String message) {
        this.messageBubbleLabel.setText(message);
    }

    public void setMessageBubbleLabelColorBlue() {
        String messageStyle = "-fx-background-color: skyblue; -fx-background-radius: 10px;";
        messageBubbleLabel.setStyle(messageStyle);
    }

    public void setMessageBubbleLabelDeleted() {
        String messageStyle = "-fx-background-color: gray; -fx-background-radius: 10px; -fx-text-fill: whitesmoke;";
        messageBubbleLabel.setStyle(messageStyle);
        messageBubbleLabel.setText("--< this message was deleted >--");
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public void setTimestamps(String timestamp) {
        if (myMessageFlag) {
            leftTimestampLabel.setText(timestamp);
            leftTimestampLabel.setPadding(new Insets(0,5,0,0));
            rightTimestampLabel.setText("");
        } else {
            rightTimestampLabel.setText(timestamp);
            rightTimestampLabel.setPadding(new Insets(0,0,0,5));
            leftTimestampLabel.setText("");
        }
        leftTimestampLabel.setVisible(false);
        rightTimestampLabel.setVisible(false);
    }

    public void setContextMenu() {
        Platform.runLater(() -> {
            // Create a context menu with a "delete" menu item
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setId("contextMenuDeleteButton");
            deleteItem.setStyle("-fx-text-fill: whitesmoke;");
            contextMenu.getItems().add(deleteItem);

            // Set the context menu to the root pane
            bubbleHBox.setOnContextMenuRequested(event -> {
                contextMenu.show(bubbleHBox, event.getScreenX(), event.getScreenY());
            });

            // Add an action event handler for the "Delete" menu item
            deleteItem.setOnAction(event -> {
                System.out.println("Delete button clicked! Deleting message: " + messageBubbleLabel.getText());

                if (Database.deleteMessageBubble(message)) {
                    MVCR.webSocketClient.sendMessageToServer(new SystemMessage(
                            messageBubbleLabel.getText() + "//" + message.getTimestamp(),
                            MVCR.MyUser.getId(),
                            Message.createTimestamp(),
                            message.getChatId(),
                            SystemMessageType.MESSAGE_DELETED
                    ));
                    setMessageBubbleLabelDeleted();

                    if (!MVCR.MyUser.userChats.get(MVCR.selectedChatBoxPane).deleteMessage(message.getText(), message.getTimestamp()))
                        MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error in deleting a message", "Could not delete the message on the user side, please reload the application.");

                    bubbleHBox.setOnContextMenuRequested(null); // Remove the context menu event handler

                } else {
                    System.out.println("Failed to delete the message. Try again later.");
                }
            });
        });
    }
}
