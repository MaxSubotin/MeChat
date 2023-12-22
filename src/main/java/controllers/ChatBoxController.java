package controllers;

import database.Database;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import util.*;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
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
    @FXML
    ImageView chatImage;

    private MainViewController MVCR;

    public void initChatBox(String name, String imageName, MainViewController mcvr) {
        setNameLabel(name);

        setMainViewControllerReference(mcvr);
        if (imageName == null) {
            setContextMenu("GroupChat");
        } else {
            setContextMenu("RegularChat");
        }
        setUserImage(imageName);
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
        MVCR.currentChat = MVCR.MyUser.getUserChats().get(newChatBox);

        if (MVCR.currentChat instanceof RegularChat) {
            // Check if the other person is connected or disconnected
            if (Database.isUserConnected(((RegularChat)MVCR.currentChat).getReceiver())) {
                MVCR.setConnectedLabelOn();
            } else
                MVCR.setConnectedLabelOff();
        } else {
            MVCR.setConnectedLabelText("");
        }

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
        ArrayList<Message> listOfMessages = MVCR.currentChat.getMessages();

        try {
            // For each message in the chat we make a new bubble and add the text into it
            for (Message message: listOfMessages) {
                FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("/views/chatBubbleComponent.fxml"));
                HBox chatBubblePane = fxmlLoader.load();
                ChatBubbleController controller = fxmlLoader.getController();

                messageBubbleLogic(message, chatBubblePane, controller, MVCR);

                if (Objects.equals(message.getText(), "--< this message was deleted >--")) {
                    controller.setMessageBubbleLabelDeleted();
                }

                MVCR.addChatBubbleToScreen(chatBubblePane);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method handles the placement of the message bubble on screen. (place it on the left with green outline OR on the right with blue outline).
     */
    public static void messageBubbleLogic(Message message, HBox chatBubblePane, ChatBubbleController controller, MainViewController MVCR) {
        // If the message was from the current user then we make it blue and positioned on the right side
        if (Objects.equals(message.getSender(), MVCR.MyUser.getId())) {
            controller.setMessageBubbleLabelColorBlue();
            chatBubblePane.setAlignment(Pos.CENTER_RIGHT);
        } else {
            chatBubblePane.setAlignment(Pos.CENTER_LEFT);
            controller.myMessageFlag = false;
        }

        controller.initMessageBubble(message, MVCR);
    }

    public static void systemMessageBubbleLogic(Message message, HBox chatBubblePane, ChatBubbleController controller, MainViewController MVCR) {
        String messageStyle = "-fx-background-color: gray; -fx-background-radius: 10px; -fx-text-fill: whitesmoke;";
        controller.messageBubbleLabel.setStyle(messageStyle);
        controller.initMessageBubble(message, MVCR);

        if (Objects.equals(message.getSender(), MVCR.MyUser.getId())) {
            chatBubblePane.setAlignment(Pos.CENTER_RIGHT);
        } else {
            chatBubblePane.setAlignment(Pos.CENTER_LEFT);
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
//            try {
//                chatImage.setImage(new Image(getClass().getResource("/images/" + image).toExternalForm()));
//            } catch (NullPointerException e) {
//                chatImage.setImage(new Image("/images/questionmark.png"));
//            }
        });
    }

    public void setContextMenu(String type) {
        Platform.runLater(() -> {
            // Create a context menu with a "delete" menu item
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete chat");
            deleteItem.setStyle("-fx-text-fill: whitesmoke;");
            contextMenu.getItems().add(deleteItem);

            // Set the context menu to the root pane
            chatBoxPane.setOnContextMenuRequested(event -> {
                contextMenu.show(chatBoxPane, event.getScreenX(), event.getScreenY());
            });

            // Add an action event handler for the "Delete" menu item
            switch (type) {
                case "RegularChat":
                    deleteItem.setOnAction(event -> deleteRegularChat());
                    break;

                case "GroupChat":
                    deleteItem.setText("Leave group");
                    deleteItem.setOnAction(event -> leaveGroupChat());
                    break;

                default:
                    MainViewController.showAlertWithMessage(Alert.AlertType.ERROR,"Error adding context menu", "Error");
            }
        });
    }

    private void deleteRegularChat() {
        System.out.println("Delete button clicked! Deleting regular chat: " + MVCR.currentChat.getChatId());

        if (Database.deleteRegularChat(MVCR.currentChat.getChatId())) {
            try {
                MVCR.MyUser.getUserChats().remove(chatBoxPane);
                MVCR.getHistoryVBox().getChildren().remove(chatBoxPane);

                if (Objects.equals(MVCR.selectedChatBoxPane, chatBoxPane)) {
                    MVCR.cleanChatBubbles();
                    MVCR.connectedLabel.setText("");
                    MVCR.selectedChatBoxPane = null;
                }

                MVCR.webSocketClient.sendMessageToServer(new SystemMessage(
                        MVCR.MyUser.getName(),
                        MVCR.MyUser.getId(),
                        Message.createTimestamp(),
                        chatBoxPane.getId(),
                        SystemMessageType.REGULAR_CHAT_DELETED
                ));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void leaveGroupChat() {
        System.out.println("Leave button clicked! Leaving group chat: " + MVCR.currentChat.getChatId());
        GroupChat chat = ((GroupChat)(MVCR.MyUser.userChats.get(chatBoxPane)));
        String oldAdminId = chat.getAdmin();
        String newAdminId = "";

        if (chat.getReceivers().size() == 1) {
            if (Database.deleteGroupChat(chat.getChatId())) {
                MVCR.MyUser.userChats.remove(chatBoxPane);
                MVCR.getHistoryVBox().getChildren().remove(chatBoxPane);

                if (Objects.equals(MVCR.selectedChatBoxPane, chatBoxPane)) {
                    MVCR.cleanChatBubbles();
                    MVCR.connectedLabel.setText("");
                }
            } else {
                MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error deleting group", "Error deleting group chat with 1 person.");
            }
            return;
        }

        boolean flag = false;

        if (Objects.equals(MVCR.MyUser.getId(), chat.getAdmin())) {
            chat.getReceivers().remove(chat.getAdmin());
            newAdminId = chat.getReceivers().get(0);
            if (Database.updateGroupChatAdmin(chatBoxPane.getId(), oldAdminId, newAdminId)) {
                chat.setAdmin(newAdminId);
                flag = true;
            } else {
                chat.getReceivers().add(MVCR.MyUser.getId()); // adding the old admin back if failed to delete
                MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error updating admin", "Could not update the group admin.");
                return;
            }
        }

        if (Database.removeGroupChatParticipant(chatBoxPane.getId(), MVCR.MyUser.getId())) {

            MVCR.MyUser.getUserChats().remove(chatBoxPane);
            MVCR.getHistoryVBox().getChildren().remove(chatBoxPane);

            if (Objects.equals(MVCR.selectedChatBoxPane, chatBoxPane)) {
                MVCR.cleanChatBubbles();
                MVCR.connectedLabel.setText("");
            }

            String txt;
            if (flag) {
                String newAdminName = Database.getUsernameById(chat.getAdmin());
                txt = "--< " + MVCR.MyUser.getName() + " has quit the group, the new admin is " + newAdminName + " >--";
            } else {
                txt = "--< " + MVCR.MyUser.getName() + " has been removed from the group chat! >--";
            }

            if (!MVCR.currentChat.sendMessage(MVCR.webSocketClient, txt)) {
                MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error in leaving group", "There was a problem sending a message about the leaving the group, please try again later.");
                return;
            }

            MVCR.webSocketClient.sendMessageToServer(new SystemMessage(
                    flag ? chat.getAdmin() : "",
                    MVCR.MyUser.getId(),
                    Message.createTimestamp(),
                    chat.getChatId(),
                    flag ? SystemMessageType.ADMIN_QUIT_GROUP : SystemMessageType.USER_QUIT_GROUP
            ));

        } else {
            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error removing group chat participant", "Could not remove group chat participant.");
            if (flag) { // reverting the changes back
                Database.updateGroupChatAdmin(chatBoxPane.getId(), newAdminId, oldAdminId);
                chat.setAdmin(MVCR.MyUser.getId());
                chat.getReceivers().add(MVCR.MyUser.getId());
            }
        }

    }

}
