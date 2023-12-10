package controllers;

import database.Database;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import util.Message;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import util.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class ChatBoxController {


    @FXML
    Label nameLabel;
    @FXML
    Pane chatBoxPane;
    @FXML
    ImageView userImage;

    private MainViewController MVCR;

    public void initChatBox(String name, String id, MainViewController mcvr) {
        setNameLabel(name);
        setUserImage(Database.getUserImageById(id));
        setMainViewControllerReference(mcvr);
        setContextMenu();
    }

    @FXML
    public void chatBoxOnClick(MouseEvent event) {
        // Removing old border
        Pane oldChatBox = MVCR.getSelectedChatBoxPane();
        if (oldChatBox != null) {
            oldChatBox.getStyleClass().remove("chatPaneClicked");
            MVCR.cleanChatBubbles();
        }

        // Creating and adding new border to the selected chat
        Pane newChatBox = (Pane) event.getSource();
        newChatBox.getStyleClass().add("chatPaneClicked");
        MVCR.setSelectedChatBoxPane(newChatBox);

        // Getting the Chat object based on the selected Pane
        MVCR.currentRegularChat = MVCR.MyUser.getUserChats().get(newChatBox);

        // Set receiver name label
        MVCR.setSelectedChatBoxUserId(MVCR.currentRegularChat.getReceiver());

        // Check if the other person is connected or disconnected
        if (Database.isUserConnected(MVCR.currentRegularChat.getReceiver())) {
            MVCR.setConnectedLabelOn();
        } else
            MVCR.setConnectedLabelOff();

        // Adding a listener to scroll down whenever there is a new message or messages are loaded (or just any change to the vbox)
        MVCR.chatVBox.heightProperty().addListener(
            (ChangeListener) (observable, oldvalue, newValue) -> MVCR.chatScrollPane.setVvalue(1.0));

        // Adding the chat name to the screen and making it visible
        MVCR.setChatNameLabel(nameLabel.getText());
        MVCR.turnChatVisible();

        // Loading the messages
        addMessageBubbles();

    }

    public void addMessageBubbles() {
        // The id of the chat box pane on the left side is the same as the name of the table that the messages are stored in the database: sender_receiver_conversationId
        //ArrayList<Message> listOfMessages = Database.getChatMessagesFromDatabase(MVCR.getSelectedChatBoxPane().getId());
        ArrayList<Message> listOfMessages = MVCR.currentRegularChat.getMessages();

        try {
            // For each message in the chat we make a new bubble and add the text into it
            for (Message message: listOfMessages) {
                FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("/views/chatBubbleComponent.fxml"));
                HBox chatBubblePane = fxmlLoader.load();
                ChatBubbleController controller = fxmlLoader.getController();


                // If the message was from the current user then we make it blue and positioned on the right side
                if (Objects.equals(message.getSender(), MVCR.MyUser.getId())) {
                    controller.initMessageBubble(message, MVCR, true);
                    controller.setMessageBubbleLabelColorBlue();
                    chatBubblePane.setAlignment(Pos.CENTER_RIGHT);
                } else
                    controller.initMessageBubble(message, MVCR, false);


                if (Objects.equals(message.getText(), "--< this message was deleted >--")) {
                    controller.setMessageBubbleLabelDeleted();
                }

                MVCR.getChatVBox().getChildren().add(chatBubblePane);
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
        this.MVCR = mainViewControllerReference;
    }


    public void setUserImage(String image) {
        Platform.runLater(() -> {
            try {
                userImage.setImage(new Image(getClass().getResource("/images/" + image).toExternalForm()));
            } catch (NullPointerException e) {
                userImage.setImage(new Image("/images/questionmark.png"));
            }
        });
    }

    public void setContextMenu() {
        Platform.runLater(() -> {
            // Create a context menu with a "delete" menu item
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setStyle("-fx-text-fill: whitesmoke;");
            contextMenu.getItems().add(deleteItem);

            // Set the context menu to the root pane
            chatBoxPane.setOnContextMenuRequested(event -> {
                contextMenu.show(chatBoxPane, event.getScreenX(), event.getScreenY());
            });

            // Add an action event handler for the "Delete" menu item
            deleteItem.setOnAction(event -> {
                System.out.println("Delete button clicked! Deleting conversation #" + MVCR.currentRegularChat.getConversation_id());

                if (Database.deleteRegularChat(MVCR.currentRegularChat)) {
                    try {
                        MVCR.MyUser.getUserChats().remove(chatBoxPane);
                        MVCR.getHistoryVBox().getChildren().remove(chatBoxPane);

                        MVCR.cleanChatBubbles();
                        MVCR.connectedLabel.setText("");

                        MVCR.webSocketClient.sendMessageToServer(MVCR.MyUser, "CHAT//DELETED//");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        });
    }
}
