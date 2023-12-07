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
import java.util.Arrays;
import java.util.function.Supplier;

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

    private void handleUserAction(Supplier<User> actionSupplier, boolean isLogin, String errorMessage) {
        Scene mainScene = loadMainView();

        Platform.runLater(() -> {
            Task<User> task = new Task<>() {
                @Override
                protected User call() {
                    try {
                        // actionSupplier is basically a way to pass a function into another function in Java.
                        // the actionSupplier in my case holds a function of the code that needs to run in the task, and that code is modular
                        // and different for login and signup, therefore user the supplier in this case is the right thing to do because it allows
                        // the task to perform any function I pass it.
                        return actionSupplier.get();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            };

            task.setOnSucceeded(event -> Platform.runLater(() -> {
                User myUser = task.getValue();

                if (myUser != null) {
                    MVCR.MyUser = myUser;
                    MVCR.webSocketClient = LVCR.webSocketClient;

                    if (isLogin) {
                        cleanLoginForm();
                    } else {
                        cleanSignupForm();
                    }

                    showMainView(mainScene);
                } else {
                    primaryStage.setScene(loadLoginAndSignupView());
                    showAlertWithMessage(Alert.AlertType.ERROR, errorMessage, errorMessage);
                }
            }));

            new Thread(task).start();
        });
    }

    private void handleUserLogin() {
        handleUserAction(() -> {
            char[] userPassword = loginPasswordField.getText().toCharArray();
            User user = Database.getUserFromDatabase(loginUsernameField.getText(), userPassword);
            Arrays.fill(userPassword, '\0'); // Zeroing out the password in memory for user safety
            if (user == null) return null;

            LVCR.user = user;
            if (!LVCR.loadUserData()) {
                closeConnectionIfOpen();
                return null;
            }

            return user;
        }, true, "Login failed. User credentials are incorrect.");
    }

    private void handleUserSignup(String hashedPassword, String userImageName) {
        handleUserAction(() -> {
            String userId = IdGenerator.generateUniqueUserId();
            User user = new User(signupUsernameField.getText(), userId, userImageName);

            if (Database.addUserToDatabase(user.getName(), hashedPassword, user.getId(), user.getUserImageWithoutSuffix())) {
                LVCR.user = user;
                if (!LVCR.loadUserData()) {
                    closeConnectionIfOpen();
                    return null;
                }
            }
            return user;
        }, false, "Signup failed. Unable to generate a new user.");
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
