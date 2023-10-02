module app.mechat {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.java_websocket;
    requires com.google.gson;
    requires java.sql;
    requires com.zaxxer.hikari;
    requires bcrypt;


    opens app.mechat to javafx.fxml;
    exports app.mechat;
}