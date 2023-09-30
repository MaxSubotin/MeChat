package app.mechat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {
    private static String dbHost = "trumpet.db.elephantsql.com";
    private static String dbName = "pjsovuwl";
    private static String user = "pjsovuwl";
    private static String password = "jziRZfERM9zpFwsoIYay5aD7-cKnaWcy";
    private static String dbUrl = null;
    private static HikariDataSource dataSource;


    static {
        HikariConfig config = new HikariConfig();
        try {
            Class.forName("org.postgresql.Driver");
            String encodedPassword = null;
            encodedPassword = URLEncoder.encode(password, "UTF-8");
            dbUrl = String.format("jdbc:postgresql://%s/%s?user=%s&password=%s",
                dbHost, dbName, user, encodedPassword);

            config.setJdbcUrl(dbUrl);
            config.setUsername(user);
            config.setPassword(password);

            config.setMaximumPoolSize(1); // Adjust this as per your requirement
            config.setConnectionTimeout(10000);
            dataSource = new HikariDataSource(config);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public DatabaseConfig() {}

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}

