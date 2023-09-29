package app.mechat;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class ChatBubbleController {

    @FXML
    Label messageBubbleLabel, leftTimestampLabel, rightTimestampLabel;
    @FXML
    HBox bubbleFlowPane;

    private Message message;


    private String myMessageStyle = "-fx-background-color: skyblue; -fx-background-radius: 10px;";
    private String othersMessageStyle = "-fx-background-color: lightgreen; -fx-background-radius: 10px;";


    @FXML
    public void showTimeStampOnBubbleHover() {
        if (bubbleFlowPane.getAlignment() == Pos.CENTER_RIGHT) {
            leftTimestampLabel.setText(message.getTimestamp());
            leftTimestampLabel.setPadding(new Insets(0,5,0,0));
        }
        else {
            rightTimestampLabel.setText(message.getTimestamp());
            rightTimestampLabel.setPadding(new Insets(0,0,0,5));
        }
    }

    @FXML
    public void hideTimeStampOnBubbleHover() {
        if (bubbleFlowPane.getAlignment() == Pos.CENTER_RIGHT) {
            leftTimestampLabel.setText("");
            leftTimestampLabel.setPadding(new Insets(0,0,0,0));
        }
        else {
            rightTimestampLabel.setText("");
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
        messageBubbleLabel.setStyle(myMessageStyle);
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
