module app.mechat {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.java_websocket;
    requires com.google.gson;
    requires java.sql;
    requires com.zaxxer.hikari;
    requires bcrypt;

    exports controllers;
    opens controllers to javafx.fxml;
    exports database;
    opens database to javafx.fxml;
    exports websockets;
    opens websockets to javafx.fxml;
    exports util;
    opens util to javafx.fxml;
    exports main;
    opens main to javafx.fxml;
}