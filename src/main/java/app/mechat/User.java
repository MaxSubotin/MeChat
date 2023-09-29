package app.mechat;

import java.util.ArrayList;

public class User {
    private String name, phoneNumber;

    public User(String _name, String _phoneNumber) {
        this.name = _name;
        this.phoneNumber = _phoneNumber;
    }

    // getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() { return phoneNumber; }

    public void setPhoneNumber(String _phoneNumber) { this.phoneNumber = _phoneNumber; }

}
