package controllers;

import at.favre.lib.crypto.bcrypt.BCrypt;
import database.Database;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import util.*;

import java.io.IOException;

public class LoginSignupViewController {

    private Stage primaryStage;
    private ImageView selectedUserImage = null;
    private HBox selectedAvatarHbox;
    private MainViewController MVCR = null; // short for mainViewControllerReference
    private LoadingViewController LVCR = null; // short for loadingViewControllerReference


    @FXML
    TextField loginUsernameField, loginPasswordField, signupUsernameField, signupPasswordField;
    @FXML
    HBox maleAvatarHBox, femaleAvatarHBox;
    @FXML
    ImageView male_AvatarImage, female_AvatarImage;
    @FXML
    AnchorPane rootNode;


    @FXML
    public void loginButtonOnClick() {
        // Get user info
        if (loginUsernameField.getText().isEmpty() || loginPasswordField.getText().isEmpty()) {
            showAlertWithMessage(Alert.AlertType.WARNING, "Missing Info", "Please enter a username and a password.");
            return;
        }

        if (showLoadingView())
            handleUserLogin();
    }

    @FXML
    public void signupButtonOnClick() {
        // Getting the username, hashing the password and setting default user image name
        String username = signupUsernameField.getText();
        String hashedPassword = BCrypt.withDefaults().hashToString(12, signupPasswordField.getText().toCharArray());
        String userImageName = "male";

        // Check that the username and password are in a correct format like length, special characters and the like
        if (!(RegexChecker.isValidUsername(username) && RegexChecker.isValidPassword(signupPasswordField.getText()))) {
            // Show and error message
            showAlertWithMessage(Alert.AlertType.ERROR,"Error", "The username or password are incorrect.\nUsername: 1 lower case letter, 1 upper case letter, 1 number, no special character, up-to 12 characters long. \nPassword: 1 lower case letter, 1 upper case letter, 1 number, can have special character, up-to 12 characters long.");
            return;
        }

        // Check that the user has selected an avatar
        if (selectedUserImage == null) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Error", "Please select an avatar.");
            return;
        }
        else {
            userImageName = selectedUserImage.getId().split("_")[0]; // the id is "male_AvatarImage"
            selectedUserImage = null; // cleaning the selected image
        }

        // Check that username is unique
        if (!Database.isUsernameUnique(username)) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Username Taken", "That username is already taken, try a different one.");
            return;
        }

        if (showLoadingView())
            handleUserSignup(hashedPassword, userImageName);

    }


    @FXML
    public void avatarImageOnClick(MouseEvent event) {
        // Removing old border
        if (selectedAvatarHbox != null) {
            selectedAvatarHbox.setStyle("-fx-border-radius: 30px");
        }

        // Creating and adding new border to the selected avatar and saving the selection
        ImageView newUserImage = (ImageView) event.getSource();
        HBox selectedAvatar = (HBox) newUserImage.getParent();
        selectedAvatar.setStyle("-fx-border-color: skyblue; -fx-border-radius: 30px; -fx-border-width: 3px");
        selectedAvatarHbox = selectedAvatar;
        selectedUserImage = newUserImage;
    }


    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    // - - - - - - - - - - - Helper Functions  - - - - - - - - - - - //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //


    private Scene loadLoadingView() {
        Scene scene = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/loadingView.fxml"));
            scene = new Scene(fxmlLoader.load(), 900, 600, Color.web("rgba(0, 0, 0, 0.75)"));
            LoadingViewController controller = fxmlLoader.getController();
            LVCR = controller;
        } catch (IOException e){
            e.printStackTrace();
        }
        return scene;
    }

    private Scene loadMainView() {
        Scene scene = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/mainView.fxml"));
            scene = new Scene(fxmlLoader.load(), 900, 600, Color.web("rgba(0, 0, 0, 0.75)"));
            MainViewController controller = fxmlLoader.getController();
            MVCR = controller;
            MVCR.getFocus();
            MVCR.turnChatInvisible();

            LVCR.MVCR = controller; // later we call the loadUserData method that will call the addUsersChatsToScreen that needs a reference to the MVCR controller to work.
        } catch (IOException e){
            e.printStackTrace();
        }
        return scene;
    }

    private Scene loadLoginAndSignupView() {
        Scene scene = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/loginSignupView.fxml"));
            scene = new Scene(fxmlLoader.load(), 900, 600, Color.web("rgba(0, 0, 0, 0.75)"));
            LoginSignupViewController controller = fxmlLoader.getController();
            controller.setPrimaryStage(primaryStage);
        } catch (IOException e){
            e.printStackTrace();
        }
        return scene;
    }

    private void handleUserLogin() {
        Scene mainScene = loadMainView();

        // Setting up code for the JavaFX Thread:
        Platform.runLater(() -> {
            // Create a Task obj that will perform the task of loading user data from the database and establish a connection to the server
            Task<User> task = new Task<>() {
                @Override
                protected User call() {
                    try {
                        // Fetch user data from the database
                        User user = Database.getUserFromDatabase(loginUsernameField.getText(), loginPasswordField.getText());
                        if (user == null) return null;
                        LVCR.user = user;
                        if (!LVCR.loadUserData()) { // loads all the user chats (if exists) and creating a new websocket connection
                            closeConnectionIfOpen(); // if something went wrong we close the connection if open
                            return null;
                        }

                        return user;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            };

            task.setOnSucceeded(event -> {
                // Update the UI after the background task is complete
                Platform.runLater(() -> {
                    User myUser = task.getValue(); // Retrieve the result from the task

                    if (myUser != null) {
                        MVCR.MyUser = myUser;
                        MVCR.webSocketClient = LVCR.webSocketClient;

                        cleanLoginForm();
                        showMainView(mainScene);
                    } else {
                        primaryStage.setScene(loadLoginAndSignupView());
                        showAlertWithMessage(Alert.AlertType.ERROR,"Login Error","Login failed. User credentials are incorrect.");
                    }
                });
            });

            // Start the background task
            new Thread(task).start();
        });
    }


    private void handleUserSignup(String hashedPassword, String userImageName) {

        Scene mainScene = loadMainView();

        // Setting up code for the JavaFX Thread:
        Platform.runLater(() -> {
            // Create a Task obj that will perform the task of loading user data from the database and establish a connection to the server
            Task<User> task = new Task<>() {
                @Override
                protected User call() {
                    try {
                        // Create a new user
                        String userId = IdGenerator.generateUniqueUserId(); // Generate a unique user id
                        User user = new User(signupUsernameField.getText(), userId, userImageName);

                        if (Database.addUserToDatabase(user.getName(), hashedPassword, user.getId(), user.getUserImageWithoutSuffix())) {
                            LVCR.user = user;
                            if (!LVCR.loadUserData()) { // loads all the user chats (if exists) and creating a new websocket connection
                                closeConnectionIfOpen(); // if something when wrong we close the connection if open
                                return null;
                            }
                        }
                        return user;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };

            task.setOnSucceeded(event -> {
                // Update the UI after the background task is complete
                Platform.runLater(() -> {
                    User myUser = task.getValue(); // Retrieve the result from the task

                    if (myUser != null) {
                        MVCR.MyUser = myUser;
                        MVCR.webSocketClient = LVCR.webSocketClient;

                        cleanSignupForm();
                        showMainView(mainScene);
                    } else {
                        primaryStage.setScene(loadLoginAndSignupView());
                        showAlertWithMessage(Alert.AlertType.ERROR,"Signup Error","Signup failed. Unable to generate a new user.");
                    }
                });
            });

            // Start the background task
            new Thread(task).start();
        });
    }

    private void cleanSignupForm() {
        signupUsernameField.clear();
        signupPasswordField.clear();
    }

    private void cleanLoginForm() {
        loginUsernameField.clear();
        loginPasswordField.clear();
    }

    public void showAlertWithMessage(Alert.AlertType type, String title, String errorMessage) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(errorMessage);
        alert.showAndWait();
    }

    private boolean showLoadingView() {
        Scene scene = loadLoadingView();
        // Create the loading view scene
        if (scene == null) {
            showAlertWithMessage(Alert.AlertType.ERROR,"Error loading the loading scene", "Could not load the loading scene properly, try again later.");
            return false;
        }

        // Show loading view
        primaryStage.setScene(scene);
        return true;
    }

    private void showMainView(Scene mainScene) {
        if (mainScene == null) {
            showAlertWithMessage(Alert.AlertType.ERROR,"Error loading the main scene", "Could not load the main scene properly, try again later.");
            return;
        }

        primaryStage.setScene(mainScene);
        primaryStage.setOnCloseRequest((WindowEvent e) -> {
            this.closeConnectionIfOpen();
        });
    }

    public void closeConnectionIfOpen() {
        if (LVCR.webSocketClient != null) {
            LVCR.webSocketClient.close();
        }
    }

    public void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

}
