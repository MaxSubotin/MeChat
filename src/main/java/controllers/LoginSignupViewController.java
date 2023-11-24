package controllers;

import at.favre.lib.crypto.bcrypt.BCrypt;
import database.Database;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import util.*;

import java.io.IOException;

public class LoginSignupViewController {

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
    public void loginButtonOnClick() {
        // 1. Get user info
        String usernameInput = loginUsernameField.getText(), passwordInput = loginPasswordField.getText();
        if (usernameInput.isEmpty() || passwordInput.isEmpty()) {
            showAlertWithMessage(Alert.AlertType.WARNING, "Missing Info", "Please enter a username and a password.");
            return;
        }

        // 2. Fetch user data from the database
        User user = Database.getUserFromDatabase(usernameInput,passwordInput);
        if (user == null) return;


        // 3. Create the loading view scene
        Scene scene = loadLoadingView();
        if (scene == null) {
            showAlertWithMessage(Alert.AlertType.ERROR,"Error loading the loading scene", "Could not load the loading scene properly, try again later.");
            return;
        }

        cleanLoginForm();
        loadUserDataAndSwitchToMain(user, scene);
    }

    @FXML
    public void signupButtonOnClick() {
        // 1. Getting the username, hashing the password and setting default user image name
        String username = signupUsernameField.getText();
        String HashedPassword = BCrypt.withDefaults().hashToString(12, signupPasswordField.getText().toCharArray());
        String userImageName = "male";

        // 2. Check that the username and password are in a correct format like length, special characters and the like
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

        // 3. Create the loading view scene
        Scene scene = loadLoadingView();
        if (scene == null) {
            showAlertWithMessage(Alert.AlertType.ERROR,"Error loading the loading scene", "Could not load the loading scene properly, try again later.");
            return;
        }

        // 4. Create a new user
        String userId = IdGenerator.generateUniqueUserId(); // Generate a unique user id
        User user = new User(username, userId, userImageName);

        if (Database.addUserToDatabase(user.getName(), HashedPassword, user.getId(), user.getUserImageWithoutSuffix())) {

            // 5. Change to loading view to load the main app
            cleanSignupForm();
            loadUserDataAndSwitchToMain(user, scene);
        }
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


    private void loadUserDataAndSwitchToMain(User user, Scene scene) {
        // This method handles both Login and Signup, it shows the loading view followed by the main view. Also handles all the user data loading

        // 1. Show loading view
        Stage currentStage = (Stage) loginUsernameField.getScene().getWindow();
        currentStage.setScene(scene);

        // 2. Start working on loading user data and showing the main view
        Scene mainScene = loadMainView();
        if (mainScene == null) {
            showAlertWithMessage(Alert.AlertType.ERROR,"Error loading the main scene", "Could not load the main scene properly, try again later.");
            return;
        }

        // 3. Setting up code for the JavaFX Thread:
        Platform.runLater(() -> {
            // Create a Task obj that will perform the task of loading user data from the database and establish a connection to the server
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    try {
                        LVCR.user = user;
                        if (!LVCR.loadUserData()) { // loads all the user chats (if exists) and creating a new websocket connection
                            closeConnectionIfOpen(); // if something when wrong we close the connection if open
                            return null;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };

            task.setOnSucceeded(event -> {
                // Update the UI after the background task is complete
                Platform.runLater(() -> {
                    MVCR.MyUser = user;
                    MVCR.webSocketClient = LVCR.webSocketClient;
                    currentStage.setScene(mainScene);
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

    public void closeConnectionIfOpen() {
        if (LVCR.webSocketClient != null) {
            LVCR.webSocketClient.close();
        }
    }
}
