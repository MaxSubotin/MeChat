package app.mechat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

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

        mainViewController.openLoginTab();
    }

    public static void main(String[] args) {
//        String location;
//        if (args.length != 0) {
//            location = args[0];
//            System.out.println("Default server url specified: \'" + location + "\'");
//        } else {
//            location = "ws://localhost:8887";
//            System.out.println("Default server url not specified: defaulting to \'" + location + "\'");
//        }
//        new ChatClient(location); // cant be here

        launch();
        // learn how to open 2 gui apps with 2 threads here to test if it works maybe? or just user visual studio code
    }

}
