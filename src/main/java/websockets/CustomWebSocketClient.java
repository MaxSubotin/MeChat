package websockets;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import controllers.ChatBoxController;
import controllers.ChatBubbleController;
import controllers.LoadingViewController;
import controllers.MainViewController;
import database.Database;
import database.DatabaseConfig;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import util.*;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CustomWebSocketClient extends WebSocketClient {
    private final String username;
    private final Scene scene;
    private final MainViewController MVCR;

    //public final Gson gson = new GsonBuilder().registerTypeAdapter(Message.class, new MessageAdapter()).create();
    public final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Message.class, new MessageAdapter())
            .registerTypeAdapterFactory(new MessageTypeAdapterFactory())
            .create();

    public CustomWebSocketClient(URI serverURI, String _username, Scene _scene, MainViewController mvcr) {
        super(serverURI);
        this.username = _username;
        this.scene = _scene;
        this.MVCR = mvcr;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println(username + " has connected to the ChatServer.");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Platform.runLater(() -> {
            if (code == 1006) {
                MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error", MVCR.MyUser.getName() + " has disconnected from the ChatServer; Code: " + code + " " + reason + "\n");
                MVCR.closeChatAppWindow();
            }
            System.out.println("Close method of ClientWebSocket was called.");
        });
    }

    @Override
    public void onMessage(String text) {
        // Deserialize the received JSON string back to your custom object
        Type messageType = new TypeToken<Message>() {}.getType();
        Message receivedMessage = gson.fromJson(text, messageType);

        if (receivedMessage instanceof SystemMessage) {
            if (!Objects.equals(MVCR.MyUser.getId(), receivedMessage.getSender())) {

                switch (((SystemMessage)receivedMessage).getType()) {

                    case CONNECT: // 🟢
                        if (MVCR.currentChat instanceof RegularChat) {
                            if (Objects.equals(((RegularChat)(MVCR.currentChat)).getReceiver(), receivedMessage.getSender()))
                                Platform.runLater(MVCR::setConnectedLabelOn);
                        }
                        break;

                    case DISCONNECT: // 🟢
//                        if (Objects.equals(MVCR.currentChat.getSender(), receivedMessage.getSender()))
//                            Platform.runLater(MVCR::setConnectedLabelOff);
                        if (MVCR.currentChat instanceof RegularChat) {
                            if (Objects.equals(((RegularChat)(MVCR.currentChat)).getReceiver(), receivedMessage.getSender()))
                                Platform.runLater(MVCR::setConnectedLabelOff);
                        }
                        break;

                    case USERNAME_CHANGED: // 🟢
                        Platform.runLater(() -> {
                            String oldUsername = receivedMessage.getText().split("//")[0];
                            String newUsername = receivedMessage.getText().split("//")[1];

                            if (MVCR.currentChat instanceof RegularChat) {
                                if (Objects.equals(((RegularChat)(MVCR.currentChat)).getReceiver(), receivedMessage.getSender())) {
                                    // The person we are chatting with has changed their name, update the chat name label and the selected chat box name
                                    MVCR.setChatNameLabel(newUsername);
                                    ((Label) (MVCR.selectedChatBoxPane.getChildren()).get(0)).setText(newUsername);
                                } else {
                                    for (Pane chatPane : MVCR.MyUser.userChats.keySet()) { // Find the correct chat box pane (on the right side) and rename the name label
                                        if (Objects.equals(((Label) (chatPane.getChildren()).get(0)).getText(), oldUsername))
                                            ((Label) (chatPane.getChildren()).get(0)).setText(newUsername);
                                    }
                                }
                            }
                            else {
                                // handle the scenario where a group chat user changes his name. need to send a message in chat with the new name and change the name in the info tab (in the ListView)
                            }


                        });
                        break;

                    case REGULAR_CHAT_CREATED: // 🟢
                        Platform.runLater(() -> {
                            String receivedId = receivedMessage.getText().split("//")[0];
                            if (Objects.equals(receivedId, MVCR.MyUser.getId())) {

                                String receiverName = receivedMessage.getText().split("//")[1];
                                String receiverImage = receivedMessage.getText().split("//")[2];

                                Pane newChatBoxPane = MVCR.createChatBoxPaneComponent(receiverName, receiverImage, receivedMessage.getChatId());
                                if (newChatBoxPane == null) {
                                    MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error in creating chat Pane", "Could not create chat Pane in the ui, try again later.");
                                    return;
                                }
                                MVCR.getHistoryVBox().getChildren().add(newChatBoxPane);
                                MVCR.MyUser.addChatToUser(newChatBoxPane,
                                        new RegularChat(
                                                new ArrayList<Message>(),
                                                MVCR.MyUser.getId(),
                                                receivedMessage.getSender(),
                                                receivedMessage.getChatId(),
                                                0
                                        ));
                            }
                        });
                        break;


                    case INVITE_TO_GROUP_CHAT: // 🟢
                        Platform.runLater(() -> {
                            String newGroupUserId = receivedMessage.getText().split("//")[0];
                            String newGroupUserName = receivedMessage.getText().split("//")[1];
                            String groupName = receivedMessage.getText().split("//")[2];
                            String groupId = receivedMessage.getChatId();

                            if (Objects.equals(newGroupUserId, MVCR.MyUser.getId())) {
                                Pane newChatBoxPane = MVCR.createChatBoxPaneComponent(groupName, null, groupId); // 🔶 passing null will at the end cause the icon to be the default question-mark icon
                                if (newChatBoxPane == null) {
                                    MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error in creating chat Pane", "Could not create chat Pane in the ui, try again later.");
                                    return;
                                }

                                try (Connection conn = DatabaseConfig.getConnection()) {
                                    ArrayList<Message> groupMessages = Database.getChatMessagesFromDatabase(groupId);
                                    ArrayList<String> groupParticipants = Database.getGroupChatParticipants(conn, groupId);

                                    if (groupMessages != null && groupParticipants != null) {
                                        MVCR.getHistoryVBox().getChildren().add(newChatBoxPane);
                                        MVCR.MyUser.addChatToUser(newChatBoxPane,
                                                new GroupChat(
                                                        groupMessages,
                                                        MVCR.MyUser.getId(),
                                                        groupParticipants,
                                                        groupId,
                                                        groupMessages.size(),
                                                        groupName,
                                                        receivedMessage.getSender()));

                                    }
                                }
                                catch (SQLException e) { e.printStackTrace(); }
                            }
                            else {
                                Pane chatToUpdate = null;
                                for (Pane chatPane : MVCR.MyUser.userChats.keySet()) {
                                    if (Objects.equals(groupId, chatPane.getId())) {
                                        chatToUpdate = chatPane;
                                        break;
                                    }
                                }

                                if (chatToUpdate != null) {
                                    ((GroupChat) (MVCR.MyUser.userChats.get(chatToUpdate))).getReceivers().add(newGroupUserId);
                                    if (MVCR.currentChat != null)
                                        if (Objects.equals(MVCR.currentChat.getChatId(), chatToUpdate.getId())) {
                                            MVCR.getInfoGroupMembersList().getItems().add(newGroupUserName);
                                        }
                                }
                            }
                        });
                        break;

                    case ADMIN_QUIT_GROUP: // 🟢
                        Platform.runLater(() -> {
                            Pane chatToUpdate = null;
                            for (Pane chatPane : MVCR.MyUser.userChats.keySet()) {
                                if (Objects.equals(receivedMessage.getChatId(), chatPane.getId())) {
                                    chatToUpdate = chatPane;
                                    break;
                                }
                            }

                            if (chatToUpdate != null) {
                                String newAdminId = receivedMessage.getText().split("//")[0];

                                ((GroupChat)MVCR.MyUser.userChats.get(chatToUpdate)).getReceivers().remove(receivedMessage.getSender());
                                ((GroupChat)MVCR.MyUser.userChats.get(chatToUpdate)).setAdmin(newAdminId); // the text holds the id of the new admin
                            }
                        });
                        break;

                    case USER_QUIT_GROUP: // 🟢
                        Platform.runLater(() -> {
                            Pane chatToUpdate = null;
                            for (Pane chatPane : MVCR.MyUser.userChats.keySet()) {
                                if (Objects.equals(receivedMessage.getChatId(), chatPane.getId())) {
                                    chatToUpdate = chatPane;
                                    break;
                                }
                            }

                            if (chatToUpdate != null) {
                                ((GroupChat)MVCR.MyUser.userChats.get(chatToUpdate)).getReceivers().remove(receivedMessage.getSender());
                            }
                        });
                        break;

                    case ADMIN_DELETED_GROUP: // 🟡
                        Platform.runLater(() -> {
                            Pane chatToDelete = null;
                            for (Pane chatPane : MVCR.MyUser.userChats.keySet()) {
                                if (Objects.equals(receivedMessage.getChatId(), chatPane.getId())) {
                                    chatToDelete = chatPane;
                                    break;
                                }
                            }

                            if (chatToDelete != null) {
                                String deletedGroupName = receivedMessage.getText(); // 🟡
                                if (Objects.equals(MVCR.selectedChatBoxPane, chatToDelete)) {
                                    MVCR.cleanChatBubbles();
                                    MVCR.setConnectedLabelText("");
                                    MVCR.selectedChatBoxPane = null;
                                }
                                MVCR.MyUser.userChats.remove(chatToDelete);
                                MVCR.getHistoryVBox().getChildren().remove(chatToDelete);
                                MainViewController.showAlertWithMessage(Alert.AlertType.INFORMATION, "Group chat deleted by admin", "The group chat \"" + deletedGroupName + "\" was deleted by the admin, all records were deleted");
                            }
                        });
                        break;

                    case ADMIN_REMOVED_GROUP_USER:
                        Platform.runLater(() -> {
                            String userIdToRemove = receivedMessage.getText().split("//")[0];
                            String usernameToRemove = receivedMessage.getText().split("//")[1];


                            if (Objects.equals(userIdToRemove, MVCR.MyUser.getId())) {
                                if (MVCR.currentChat != null && Objects.equals(MVCR.currentChat.getChatId(), receivedMessage.getChatId())) {
                                    MVCR.getHistoryVBox().getChildren().remove(MVCR.selectedChatBoxPane);
                                    MVCR.MyUser.getUserChats().remove(MVCR.selectedChatBoxPane);
                                    MVCR.cleanChatBubbles();
                                    MVCR.setConnectedLabelText("");
                                    MVCR.selectedChatBoxPane = null;
                                }
                                else {
                                    Pane chatToRemove = null;
                                    for (Pane chatPane : MVCR.MyUser.userChats.keySet()) {
                                        if (Objects.equals(receivedMessage.getChatId(), chatPane.getId())) {
                                            chatToRemove = chatPane;
                                            break;
                                        }
                                    }

                                    if (chatToRemove != null) {
                                        ((GroupChat) MVCR.MyUser.userChats.get(chatToRemove)).getReceivers().remove(userIdToRemove);
                                        MVCR.getHistoryVBox().getChildren().remove(chatToRemove);
                                    }
                                }
                            }
                            else {
                                Pane chatToUpdate = null;
                                for (Pane chatPane : MVCR.MyUser.userChats.keySet()) {
                                    if (Objects.equals(receivedMessage.getChatId(), chatPane.getId())) {
                                        chatToUpdate = chatPane;
                                        break;
                                    }
                                }

                                if (chatToUpdate != null) {
                                    ((GroupChat) MVCR.MyUser.userChats.get(chatToUpdate)).getReceivers().remove(userIdToRemove);
                                    if (MVCR.currentChat != null)
                                        if (Objects.equals(MVCR.currentChat.getChatId(), chatToUpdate.getId())) {
                                            MVCR.getInfoGroupMembersList().getItems().remove(usernameToRemove);
                                        }
                                }
                            }
                        });
                        break;

                    case USER_DELETED: // 🔴
                        Platform.runLater(() -> {

                            try {
                                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/loadingView.fxml"));
                                Scene scene = new Scene(fxmlLoader.load(), 900, 600, Color.web("rgba(0, 0, 0, 0.75)"));
                                LoadingViewController controller = fxmlLoader.getController();
                                MVCR.MyUser.userChats.clear();
                                controller.MVCR = MVCR;
                                controller.addUsersChatsToScreen(MVCR.MyUser, Database.getUsersChatsFromDatabase(MVCR.MyUser.getId()));
                            } catch (IOException e){
                                e.printStackTrace();
                            }

                            // THIS CODE IS COMPLICATED, I THINK IT WILL BE FASTER TO JUST RELOAD THE CHATS FROM THE DATABASE

//                            // Find and save the chat we need to delete from the current user
//                            Pane chatToDelete = null;
//                            for (Pane chatPane : MVCR.MyUser.userChats.keySet()) {
//                                if (MVCR.MyUser.getUserChats().get(chatPane) instanceof RegularChat) {
//                                    if (Objects.equals(receivedMessage.getSender(), ((RegularChat) MVCR.MyUser.getUserChats().get(chatPane)).getReceiver()))
//                                        chatToDelete = chatPane; //🔴 add a call to a deleteChatBoxPane method here
//                                }
//                                else {
//                                    ((GroupChat) MVCR.MyUser.getUserChats().get(chatPane)).getReceivers().remove(receivedMessage.getSender());
//                                    // add a bubble to the screen saying that the user deleted his account
//                                    if (((GroupChat) MVCR.MyUser.getUserChats().get(chatPane)).getReceivers().size() == 1) // if after removing the deleted account i am the only person in the group chat
//                                        return; //🔴 add a call to a deleteChatBoxPane method here
//                                }
//
//
//
//                            }
//
//                            // Delete the chat if found
//                            if (chatToDelete != null) {
//                                MVCR.getHistoryVBox().getChildren().remove(chatToDelete);
//                                MVCR.MyUser.userChats.remove(chatToDelete);
//
//                                // delete the message bubbles if the deleted chat is open
//                                if (Objects.equals(MVCR.selectedChatBoxPane, chatToDelete)) {
//                                    MVCR.cleanChatBubbles();
//                                    MVCR.setConnectedLabelText("");
//                                    MVCR.selectedChatBoxPane = null;
//                                }
//                            }
//
//                            if (text.contains("USERNAME//DELETED//"))
//                                MainViewController.showAlertWithMessage(Alert.AlertType.INFORMATION, "A user deleted his account", receivedMessage.getText().split("//")[2] + " has deleted his account and is no longer available.");
//                            else
//                                MainViewController.showAlertWithMessage(Alert.AlertType.INFORMATION, "A user deleted his chat with you", Database.getUsernameById(receivedMessage.getSender()) + " has deleted his chat with you.");

                        });
                        break;

                    case REGULAR_CHAT_DELETED: // 🟢
                        Platform.runLater(() -> {
                            // Find and save the chat we need to delete from the current user
                            Pane chatToDelete = null;
                            for (Pane chatPane : MVCR.MyUser.userChats.keySet()) {
                                if (Objects.equals(receivedMessage.getChatId(), chatPane.getId())) {
                                    chatToDelete = chatPane;
                                    break;
                                }
                            }

                            // Delete the chat if found
                            if (chatToDelete != null) {
                                MVCR.getHistoryVBox().getChildren().remove(chatToDelete);
                                MVCR.MyUser.userChats.remove(chatToDelete);

                                // delete the message bubbles if the deleted chat is open
                                if (Objects.equals(MVCR.selectedChatBoxPane, chatToDelete)) {

                                    MVCR.cleanChatBubbles();
                                    MVCR.setConnectedLabelText("");
                                    MVCR.selectedChatBoxPane = null;

                                }

                                MainViewController.showAlertWithMessage(Alert.AlertType.INFORMATION, "A user deleted his chat with you", receivedMessage.getText() + " has deleted his chat with you.");
                            }
                        });
                        break;

                    case MESSAGE_DELETED: // 🟢
                        Platform.runLater(() -> {
                            Pane correctChat = null;
                            for (Pane chatPane : MVCR.MyUser.userChats.keySet()) {
                                if (Objects.equals(receivedMessage.getChatId(), chatPane.getId())) {
                                    correctChat = chatPane;
                                    break;
                                }
                            }

                            if (correctChat != null) {
                                //deconstruct the message that was deleted: message comes like so: message//timestamp
                                String receivedMessageText = receivedMessage.getText().split("//")[0];
                                String receivedMessageTimestamp = receivedMessage.getText().split("//")[1];

                                if (Objects.equals(MVCR.selectedChatBoxPane, correctChat)) {
                                    for (Node node : MVCR.getChatVBox().getChildren()) {
                                        if (node instanceof HBox) {
                                            List<Node> HBoxChildren = ((HBox) node).getChildren();
                                            Label messageText = (Label) HBoxChildren.get(1);
                                            Label leftMessageTimestamp = (Label) HBoxChildren.get(0);
                                            Label rightMessageTimestamp = (Label) HBoxChildren.get(2);

                                            if (Objects.equals(messageText.getText(), receivedMessageText) &&
                                                    (Objects.equals(leftMessageTimestamp.getText(), receivedMessageTimestamp) ||
                                                            Objects.equals(rightMessageTimestamp.getText(), receivedMessageTimestamp))) {

                                                messageText.setText("--< this message was deleted >--");
                                                node.setStyle("-fx-background-color: gray; -fx-background-radius: 10px; -fx-text-fill: whitesmoke;");
                                                break;
                                            }
                                        }
                                    }
                                }

                                if (!MVCR.MyUser.userChats.get(correctChat).deleteMessage(receivedMessageText, receivedMessageTimestamp))
                                    MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error in deleting a message", "Could not delete the message on the user side, please reload the application.");

                            }
                        });
                        break;

                    case GROUP_CHAT_RENAMED:
                        Platform.runLater(() -> {
                            Pane correctChat = null;
                            for (Pane chatPane : MVCR.MyUser.userChats.keySet()) {
                                if (Objects.equals(receivedMessage.getChatId(), chatPane.getId())) {
                                    correctChat = chatPane;
                                    break;
                                }
                            }

                            if (correctChat != null) {
                                if (Objects.equals(MVCR.selectedChatBoxPane.getId(), correctChat.getId()))
                                    MVCR.setChatNameLabel(receivedMessage.getText());

                                ((GroupChat)(MVCR.MyUser.userChats.get(correctChat))).setGroupName(receivedMessage.getText());
                                ((Label) ((HBox) (MVCR.selectedChatBoxPane.getChildren()).get(0)).getChildren().get(1)).setText(receivedMessage.getText());
                            }
                        });
                        break;

                    default:
                        MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error in CustomWebSocketClient", "Please reload the application.");

                }

            }
        }
        else {
            if (Objects.equals(MVCR.MyUser.getId(),receivedMessage.getSender()) ||
                    ( (receivedMessage instanceof RegularMessage) && Objects.equals(MVCR.MyUser.getId(), ((RegularMessage) receivedMessage).getReceiver()) ) ||
                    ( (receivedMessage instanceof GroupMessage) && ((GroupMessage) receivedMessage).getReceivers().contains(MVCR.MyUser.getId())))
            {
                Platform.runLater(() -> {
                    try {
                        // Creating a new message bubble on the screen and adding the users text into it
                        FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("/views/chatBubbleComponent.fxml"));
                        HBox chatBubblePane = fxmlLoader.load();
                        ChatBubbleController controller = fxmlLoader.getController();
                        ChatBoxController.messageBubbleLogic(receivedMessage, chatBubblePane, controller, MVCR);
                        if (MVCR.selectedChatBoxPane != null && Objects.equals(receivedMessage.getChatId(), MVCR.selectedChatBoxPane.getId()))
                            MVCR.MyUser.addMessageToChat(MVCR.selectedChatBoxPane, receivedMessage);
                        else {
                            for (Pane chat : MVCR.MyUser.getUserChats().keySet()) {
                                if (Objects.equals(chat.getId(), receivedMessage.getChatId())) {
                                    MVCR.MyUser.addMessageToChat(chat, receivedMessage);
                                    break;
                                }
                            }
                        }

                        if (MVCR.selectedChatBoxPane != null) {
                            if (Objects.equals(MVCR.MyUser.getId(), receivedMessage.getSender()) || Objects.equals(MVCR.selectedChatBoxPane.getId(), receivedMessage.getChatId())) {
                                MVCR.addChatBubbleToScreen(chatBubblePane);
                            }
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    @Override
    public void onError(Exception ex) {
        Platform.runLater(() -> {
            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error", "Exception occurred ...\nClosing the app now, try again later.");

            Stage stage = (Stage) scene.getWindow();
            stage.close();

            String userId = Database.getUserIdByUsername(this.username);
            if (userId != null) {
                if (!Database.removeUserSession(userId))
                    System.out.println("Could not complete the user session removal form the database. Error in onError Override function in CustomWebSocketClient.");
            }
        });
    }

    public void sendMessageToServer(SystemMessage systemMessage) {
        // Turn Message object into json format
        String jsonSysMessage = gson.toJson(systemMessage);

        // Send message to users that you have changed your name
        send(jsonSysMessage);
    }

}
