package ui;

import controllers.LoginSignupViewController;
import controllers.MainViewController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Scene;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.util.NodeQueryUtils.isVisible;

@SuppressWarnings("exports")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EndToEndTest extends ApplicationTest {

    Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/loginSignupView.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 900, 600, Color.web("rgba(0, 0, 0, 0.75)"));
            LoginSignupViewController controller = fxmlLoader.getController();
            controller.setPrimaryStage(primaryStage);

            primaryStage.setTitle("MeChat!");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            this.primaryStage = primaryStage;
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        FxToolkit.hideStage();
    }

    // 🟢 Tests 🟢 //

    @Test
    @Order(1)
    @DisplayName("Test Login Logout:")
    public void testLoginLogoutScenario() {
        loginProcess("Max", "123");

        // Wait for the main scene to be displayed using awaitility
        Awaitility.await().until(() -> primaryStage.getScene().lookup("#mainView") != null);

        assertFalse(verifyErrorPopUp());
        verifyThat("#mainView", isVisible());

        logoutProcess();
    }

    @Test
    @Order(2)
    @DisplayName("Test Signup Logout:")
    public void testSignupLogoutScenario() {
        clickOn("#signupTab");

        clickOn("#signupUsernameField").write("Test1");
        clickOn("#signupPasswordField").write("Test1");
        clickOn("#male_AvatarImage");
        clickOn("#signupButton");

        // Wait for the main scene to be displayed using awaitility
        Awaitility.await().until(() -> primaryStage.getScene().lookup("#mainView") != null);
        assertFalse(verifyErrorPopUp());

        verifyThat("#mainView", isVisible());

        logoutProcess();
    }

    @Test
    @Order(3)
    @DisplayName("Test Create New Chat:")
    public void testCreateNewChatScenario() {
        loginProcess("Test1", "Test1");

        // Wait for the main scene to be displayed using awaitility
        Awaitility.await().until(() -> primaryStage.getScene().lookup("#mainView") != null);

        assertFalse(verifyErrorPopUp());
        verifyThat("#mainView", isVisible());

        clickOn("#newChatButton");
        clickOn("#nameTextField").write("Max");
        clickOn("#confirmButton");
        assertFalse(verifyErrorPopUp());

        // Lookup the VBox with id "HistoryVBox"
        VBox historyVBox = lookup("#historyVBox").query();
        assertNotNull(historyVBox, "Could not find the historyVBox.");

        Node firstChat = historyVBox.getChildren().get(0);
        clickOn(firstChat);
        clickOn("#messageTextField").write("This is a test message by testCreateNewChatScenario.");
        clickOn("#sendMessageButton");
        assertFalse(verifyErrorPopUp());

        logoutProcess();
    }

    @Test
    @Order(4)
    @DisplayName("Test View Received Message:")
    public void testViewReceivedMessageScenario() {
        loginProcess("Max", "123");

        // Wait for the main scene to be displayed using awaitility
        Awaitility.await().until(() -> primaryStage.getScene().lookup("#mainView") != null);

        assertFalse(verifyErrorPopUp());
        verifyThat("#mainView", isVisible());

        // Find the correct chat box
        VBox historyVBox = lookup("#historyVBox").query();
        assertNotNull(historyVBox, "Could not find the historyVBox.");

        for (Node node : historyVBox.getChildren()) {
            if (node instanceof Pane) {
                Pane chatPane = (Pane) node;
                if (chatPane.getChildren().size() == 1 && chatPane.getChildren().get(0) instanceof HBox) {
                    HBox chatHBox = (HBox) chatPane.getChildren().get(0);
                    List<Node> chatChildren = chatHBox.getChildren();

                    // Assuming the ImageView is the first child and Label is the second child
                    if (chatChildren.size() == 2 && chatChildren.get(1) instanceof Label) {
                        Label chatLabel = (Label) chatChildren.get(1);
                        String labelText = chatLabel.getText();

                        if ("Test1".equals(labelText)) {
                            // Found the correct node, click on and check to see if the message made it
                            clickOn(chatLabel);

                            VBox chatVBox = lookup("#chatVBox").query();
                            assertNotNull(chatVBox, "Could not find the historyVBox.");

                            if (!chatVBox.getChildren().isEmpty()) {
                                Node lastChild = chatVBox.getChildren().get(chatVBox.getChildren().size() - 1);

                                if (lastChild instanceof HBox) {
                                    assertEquals("This is a test message by testCreateNewChatScenario.",
                                            ((Label) ((HBox) lastChild).getChildren().get(1)).getText()
                                    );
                                }
                            }

                            break; // Assuming you want to stop after finding the correct node
                        }
                    }
                }
            }
        }

        logoutProcess();
    }

    @Test
    @Order(5)
    @DisplayName("Test Change Username:")
    public void testChangeUsernameScenario() {
        loginProcess("Test1", "Test1");

        // Wait for the main scene to be displayed using awaitility
        Awaitility.await().until(() -> primaryStage.getScene().lookup("#mainView") != null);

        assertFalse(verifyErrorPopUp());
        verifyThat("#mainView", isVisible());

        clickOn("#settingsButton");
        verifyThat("#settingsPane", isVisible());

        clickOn("#settingsUsernameField").write("TEST1");
        clickOn("#settingsUsernameSaveButton");
        assertTrue(verifyErrorPopUp());

        clickOn("#settingsUsernameField").write("tEST1");
        clickOn("#settingsUsernameSaveButton");
        clickOn("#settingsCloseButton");

        clickOn("#profileButton");
        verifyThat("#loginAndSignupPane", isVisible());
        verifyThat("#userUsernameLabel", LabeledMatchers.hasText("tEST1"));

        clickOn("#logoutButton");
    }

    @Test
    @Order(6)
    @DisplayName("Test Login Delete Account:")
    public void testLoginDeleteAccScenario() {
        loginProcess("tEST1", "Test1");

        // Wait for the main scene to be displayed using awaitility
        Awaitility.await().until(() -> primaryStage.getScene().lookup("#mainView") != null);

        assertFalse(verifyErrorPopUp());
        verifyThat("#mainView", isVisible());

        deleteAccountProcess();
    }

    @Test
    @Order(7)
    @DisplayName("Test Login Unsuccessful:")
    public void testLoginUnsuccessfulScenario() { // trying to log into a deleted account
        loginProcess("tEST1", "Test1");

        // Wait for the main scene to be displayed using awaitility
        Awaitility.await().until(() -> primaryStage.getScene().lookup("#loginAndSignupView") != null);

        assertTrue(verifyErrorPopUp());
        verifyThat("#loginAndSignupView", isVisible());
    }


    private void loginProcess(String username, String password) {
        clickOn("#loginUsernameField").write(username);
        clickOn("#loginPasswordField").write(password);
        clickOn("#loginButton");
    }

    private void logoutProcess() {
        clickOn("#profileButton");
        verifyThat("#loginAndSignupPane", isVisible());
        clickOn("#logoutButton");
    }

    private void deleteAccountProcess() {
        clickOn("#settingsButton");
        verifyThat("#settingsPane", isVisible());

        clickOn("#settingsDeleteAccountButton");
        verifyThat("#loginAndSignupView", isVisible());
    }

    private boolean verifyErrorPopUp() {
        // Check if an error pop-up is visible
        if (lookup(".alert").tryQuery().isPresent()) {
            System.out.println("Error pop-up is visible. Closing the popup...");
            clickOn(".alert .button-bar .button:contains(Close)");
            return true;
        }
        return false;
    }


}
