package util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import database.Database;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class User {
    private String name, id, userImage;
    public HashMap<Pane, RegularChat> userChats = new HashMap<>();

    public User(String _name, String _id, String _userImage) {
        this.name = _name;
        this.id = _id;
        this.userImage = _userImage + ".png"; // this will be male.png or female.png and will be decided be the database (male or female)
    }



    // getters and setters
    public String getName() {
        return name;
    }

    public boolean setName(String newUsername) {
        // Check newUsername in database and update the database
        if (Database.isUsernameUnique(newUsername)) {
            try {
                if (!Database.updateUsernameInDatabase(newUsername, getName())) {
                    System.out.println("Could not update the username.");
                    return false;
                }
            }
            catch (Exception e) { return false; }

            this.name = newUsername;
            return true;
        }
        else return false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserImage() {
        return userImage;
    }

    public String getUserImageWithoutSuffix() {
        return userImage.split(".png")[0];
    }

    public void setUserImage(String image) {
        this.userImage = image + ".png";
        if (Objects.equals(image, "male"))
            Database.updateAvatarInDatabase("male", getName());
        else
            Database.updateAvatarInDatabase("female", getName());

    }

    public HashMap<Pane, RegularChat> getUserChats() { return userChats; }

    public void addChatToUser(Pane pane, RegularChat chat) {
        getUserChats().put(pane, chat);
    }

    public boolean updatePassword(String hashedPassword) {

        // Update the database
        if (!Database.updatePasswordInDatabase(hashedPassword, getName()))
            return false;
        return true;
    }
}
