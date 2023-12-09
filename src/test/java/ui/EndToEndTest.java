package ui;

import controllers.LoginSignupViewController;
import javafx.fxml.FXMLLoader;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Scene;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    @Order(1)
    public void testLoginLogoutScenario() {
        clickOn("#loginUsernameField").write("Max");
        clickOn("#loginPasswordField").write("123");
        clickOn("#loginButton");

        // Wait for the main scene to be displayed using awaitility
        Awaitility.await().until(() -> primaryStage.getScene().lookup("#mainView") != null);
        assertFalse(verifyErrorPopUp());

        verifyThat("#mainView", isVisible());

        clickOn("#profileButton");
        verifyThat("#loginAndSignupPane", isVisible());

        clickOn("#logoutButton");
    }

    @Test
    @Order(2)
    public void testSignupLogoutScenario() {
        clickOn("#loginUsernameField").write("Max");
        clickOn("#loginPasswordField").write("123");
        clickOn("#loginButton");

        // Wait for the main scene to be displayed using awaitility
        Awaitility.await().until(() -> primaryStage.getScene().lookup("#mainView") != null);
        assertFalse(verifyErrorPopUp());

        verifyThat("#mainView", isVisible());

        clickOn("#profileButton");
        verifyThat("#loginAndSignupPane", isVisible());

        clickOn("#logoutButton");

    }

    @Override
    public void stop() throws Exception {
        FxToolkit.hideStage();
    }

    private boolean verifyErrorPopUp() {
        // Check if an error pop-up is visible
        if (lookup(".alert").tryQuery().isPresent()) {
            System.out.println("Error pop-up is visible.");
            return true;
        }
        return false;
    }


}
