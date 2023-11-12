package util;

import java.util.ArrayList;

public class User {
    private String name, id, userImage;

    public User(String _name, String _id, String _userImage) {
        this.name = _name;
        this.id = _id;
        this.userImage = _userImage + ".png"; // this will be male.png or female.png and will be decided be the database (male or female)
    }

    // getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public void setUserImage(String image) {
        this.userImage = image + ".png";
    }
}
