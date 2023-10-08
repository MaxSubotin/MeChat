package util;

import java.util.ArrayList;

public class User {
    private String name, id;

    public User(String _name, String _id) {
        this.name = _name;
        this.id = _id;
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
}
