package app.mechat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

public class MeChatMain extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MeChatMain.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 602, Color.web("rgba(0, 0, 0, 0.75)"));

        MainViewController mainViewController = fxmlLoader.getController();
        mainViewController.getFocus();
        mainViewController.turnChatInvisible();

        stage.setTitle("MeChat!");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        stage.setOnCloseRequest((WindowEvent event) -> {
            mainViewController.closeConnectionIfOpen();
        });

        mainViewController.openLoginTab();
    }

    public static void main(String[] args) {
        launch();
    }

}
