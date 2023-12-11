package websockets;

import com.google.gson.Gson;
import controllers.ChatBoxController;
import controllers.ChatBubbleController;
import controllers.MainViewController;
import database.Database;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import util.Message;
import util.MessageAdapter;
import util.User;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

public class CustomWebSocketClient extends WebSocketClient {
    private final String username;
    private final Scene scene;
    private final MainViewController MVCR;

    public final Gson gson = new GsonBuilder().registerTypeAdapter(Message.class, new MessageAdapter()).create();


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
        Message receivedMessage = gson.fromJson(text, Message.class);

        if (receivedMessage.getIsSystemMessage()) {
            if (!Objects.equals(MVCR.MyUser.getId(), receivedMessage.getSender())) {
                if (text.contains("DISCONNECT//")) {
                    if (Objects.equals(MVCR.selectedChatBoxUserId, receivedMessage.getSender()))
                        Platform.runLater(() -> {
                            MVCR.setConnectedLabelOff();
                        });
                } else if (text.contains("CONNECT//")) {
                    if (Objects.equals(MVCR.selectedChatBoxUserId, receivedMessage.getSender()))
                        Platform.runLater(() -> {
                            MVCR.setConnectedLabelOn();
                        });
                } else if (text.contains("USERNAME//CHANGED//")) {
                    Platform.runLater(() -> {
                        // Holding on to the old name and changing the title to the new name
                        String newName = receivedMessage.getText().split("//")[2];

                        if (Objects.equals(MVCR.selectedChatBoxUserId, receivedMessage.getSender())) {
                            // The person we are chatting with has changed their name, update the chat name label and the selected chat box name
                            MVCR.setChatNameLabel(newName);
                            ((Label) (MVCR.selectedChatBoxPane.getChildren()).get(0)).setText(newName);

                        } else {
                            // Find the correct chat box pane (on the right side) and rename the name label on it
                            String chatId = Database.compareStrings(receivedMessage.getSender(), MVCR.MyUser.getId());
                            for (Pane chatPane : MVCR.MyUser.userChats.keySet()) {
                                if (chatPane.getId().contains(chatId))
                                    ((Label) (chatPane.getChildren()).get(0)).setText(newName);
                            }
                        }
                    });
                } else if (text.contains("USERNAME//DELETED//") || text.contains("CHAT//DELETED//")) {
                    Platform.runLater(() -> {
                        // Find and save the chat we need to delete from the current user
                        Pane chatToDelete = null;
                        for (Pane chatPane : MVCR.MyUser.userChats.keySet()) {
                            if (chatPane.getId().contains(receivedMessage.getSender()))
                                chatToDelete = chatPane;
                        }

                        // Delete the chat if found
                        if (chatToDelete != null) {
                            MVCR.getHistoryVBox().getChildren().remove(chatToDelete);
                            MVCR.MyUser.userChats.remove(chatToDelete);

                            // delete the message bubbles if the deleted chat is open
                            if (Objects.equals(MVCR.selectedChatBoxUserId, receivedMessage.getSender())) {
                                MVCR.cleanChatBubbles();
                                MVCR.setConnectedLabelText("");
                                MVCR.selectedChatBoxUserId = null;
                                MVCR.selectedChatBoxPane = null;
                            }
                        }

                        if (text.contains("USERNAME//DELETED//"))
                            MainViewController.showAlertWithMessage(Alert.AlertType.INFORMATION, "A user deleted his account", receivedMessage.getText().split("//")[2] + " has deleted his account and is no longer available.");
                        else
                            MainViewController.showAlertWithMessage(Alert.AlertType.INFORMATION, "A user deleted his chat with you", Database.getUsernameById(receivedMessage.getSender()) + " has deleted his chat with you.");

                    });
                } else if (text.contains("MESSAGE//DELETED//")) {
                    if (Objects.equals(MVCR.selectedChatBoxUserId, receivedMessage.getSender())) {
                        Platform.runLater(() -> {
                            //deconstruct the message that was deleted: message comes like so: "MESSAGE//DELETED//this is the message//timestamp
                            String receivedMessageText = receivedMessage.getText().split("//")[2];
                            String receivedMessageTimestamp = receivedMessage.getText().split("//")[3];

                            for (Node node: MVCR.getChatVBox().getChildren()) {

                                if (node instanceof HBox) {
                                    List<Node> HBoxChildren = ((HBox)node).getChildren();
                                    Label messageText = (Label)HBoxChildren.get(1);
                                    Label leftMessageTimestamp = (Label)HBoxChildren.get(0);
                                    Label rightMessageTimestamp = (Label)HBoxChildren.get(2);

                                    if (Objects.equals(messageText.getText(), receivedMessageText) &&
                                        ( Objects.equals(leftMessageTimestamp.getText(), receivedMessageTimestamp) ||
                                            Objects.equals(rightMessageTimestamp.getText(), receivedMessageTimestamp) )) {
                                        System.out.println("4");

                                        messageText.setText("--< this message was deleted >--");
                                        node.setStyle("-fx-background-color: gray; -fx-background-radius: 10px; -fx-text-fill: whitesmoke;");

                                        if (!MVCR.MyUser.userChats.get(MVCR.selectedChatBoxPane).handleMessageDeleted(receivedMessageText, receivedMessageTimestamp))
                                            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error in deleting a message", "Could not delete the message on the user side, please reload the application.");
                                    }
                                }
                            }
                        });
                    }
                } else if (text.contains("NEW//CHAT//CREATED//")) {
                    Platform.runLater(() -> {
                        String receiverName = receivedMessage.getText().split("//")[3];
                        String receiverId = receivedMessage.getText().split("//")[4];
                        int receivedConversationId = Integer.parseInt(receivedMessage.getText().split("//")[5]);

                        Pane newChatBoxPane = MVCR.createChatBoxPaneComponent(receiverName, receiverId);
                        if (newChatBoxPane == null) {
                            MainViewController.showAlertWithMessage(Alert.AlertType.ERROR, "Error in creating chat Pane", "Could not create chat Pane in the ui, try again later.");
                            return;
                        }

                        MVCR.getHistoryVBox().getChildren().add(newChatBoxPane);

                        MVCR.finalizeChatBoxComponentCreation(receiverId, receivedConversationId, newChatBoxPane);
                    });
                }
            }
        }
        else {
            if (Objects.equals(MVCR.MyUser.getId(),receivedMessage.getSender()) || Objects.equals(MVCR.MyUser.getId(),receivedMessage.getReceiver())) {
                Platform.runLater(() -> {
                    try {
                        // Creating a new message bubble on the screen and adding the users text into it
                        FXMLLoader fxmlLoader = new FXMLLoader(ChatBoxController.class.getResource("/views/chatBubbleComponent.fxml"));
                        HBox chatBubblePane = fxmlLoader.load();
                        ChatBubbleController controller = fxmlLoader.getController();

                        ChatBoxController.messageBubbleLogic(receivedMessage, chatBubblePane, controller, MVCR);

                        if (Objects.equals(MVCR.MyUser.getId(),receivedMessage.getSender()) ||
                                (Objects.equals(MVCR.MyUser.getId(),receivedMessage.getReceiver()) && Objects.equals(MVCR.selectedChatBoxUserId, receivedMessage.getSender())))
                            MVCR.MyUser.addMessageToChat(MVCR.selectedChatBoxPane, receivedMessage);
                        else { // you are the receiver AND you are NOT looking at the chat (there for we cant use: MVCR.selectedChatBoxPane)
                            String correctChatBoxId = Database.compareStrings(receivedMessage.getSender(), receivedMessage.getReceiver()) + "_" + receivedMessage.getConversation_id();
                            for (Pane chat: MVCR.MyUser.getUserChats().keySet()) {
                                if (Objects.equals(chat.getId(), correctChatBoxId)) {
                                    MVCR.MyUser.addMessageToChat(chat, receivedMessage);
                                    break;
                                }
                            }
                        }

                        if (Objects.equals(MVCR.MyUser.getId(),receivedMessage.getSender()) || Objects.equals(MVCR.selectedChatBoxUserId, receivedMessage.getSender())) {
                            MVCR.addChatBubbleToScreen(chatBubblePane);
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

    public void sendMessageToServer(User user, String message) {
        // Create a system message object
        Message sysMessage = new Message(message, user.getId(), user.getId(), new Timestamp(System.currentTimeMillis()).toString() ,"-1");
        sysMessage.setIsSystemMessage(true);

        // Turn Message object into json format
        String jsonSysMessage = gson.toJson(sysMessage);

        // Send message to users that you have changed your name
        send(jsonSysMessage);
    }

}
