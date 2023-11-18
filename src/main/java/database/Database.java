package database;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import util.RegularChat;
import util.Message;
import util.User;
import at.favre.lib.crypto.bcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Database {


    public static User getUserFromDatabase(String userUsername, String userPassword) {
        String queryForUser = "SELECT * FROM users WHERE username = ?";
        User userFromDatabase = null;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(queryForUser)) {

            // Find the user in the database and create a User object
            pst1.setString(1, userUsername);
            ResultSet rs = pst1.executeQuery();

            if (rs.next()) {
                BCrypt.Result result = BCrypt.verifyer().verify(userPassword.toCharArray(), rs.getString("password"));
                if (result.verified)
                    userFromDatabase = new User(rs.getString("username"), rs.getString("id"), rs.getString("image"));
            }
        }
        catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "User Not Found", "User not found, please try again later.\nERROR #1");
            catchBlockCode(e); // User not found error is handled in the MainViewController file
        }

        return userFromDatabase;
    }



    public static ArrayList<RegularChat> getUsersChatsFromDatabase(String userId) {
        String queryForConversations = "SELECT * FROM conversations WHERE participant1 = ? OR participant2 = ?";
        ArrayList<RegularChat> usersRegularChats = new ArrayList<RegularChat>();

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(queryForConversations)) {

            // Find all the conversations this user has in the database and for each one we will create a chat box
            pst1.setString(1, userId);
            pst1.setString(2, userId);
            ResultSet rs = pst1.executeQuery();

            while (rs.next()) {
                if (Objects.equals(rs.getString("participant1"), userId))
                    usersRegularChats.add(new RegularChat(
                            null,
                            userId,
                            rs.getString("participant2"),
                            rs.getInt("conversation_id"))
                    );
                else
                    usersRegularChats.add(new RegularChat(
                            null,
                            userId,
                            rs.getString("participant1"),
                            rs.getInt("conversation_id"))
                    );
            }

        } catch (SQLException e) {
            catchBlockCode(e);
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not load user chats", "Could not load the user chats for user: " + userId + "\nERROR #2");
            usersRegularChats = null;
        }

        return usersRegularChats;
    }

    public static ArrayList<Message> getChatMessagesFromDatabase(String tableName) {
        String queryForConversations = "SELECT * FROM " + tableName;
        ArrayList<Message> chatMessages = new ArrayList<>();

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(queryForConversations)) {

            // Find all the conversations this user has in the database and for each one we will create a chat box
            ResultSet rs = pst1.executeQuery();

            while (rs.next()) {
                chatMessages.add(new Message(rs.getString("message_text"), rs.getString("sender_id"), rs.getString("receiver_id"),rs.getString("timestamp"), tableName.split("_")[2]));
            }

        } catch (SQLException e) {
            catchBlockCode(e);
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not load table", "Could not load the table, table name: " + tableName + "\nERROR #3");
            chatMessages.add(0,new Message("ERROR","ERROR","ERROR","ERROR","ERROR"));
        }

        return chatMessages;
    }

    public static String getUserIdBySession(String session) {
        String queryForUser = "SELECT * FROM session_tokens WHERE session_token = ?";
        String userId = null;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(queryForUser)) {

            // Find the user in the database and create a User object
            pst1.setString(1, session);
            ResultSet rs = pst1.executeQuery();

            if (rs.next()) {
                userId = rs.getString("id");
            }
        }
        catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not find user", "Error finding the user by session id, try again later.\nERROR #4");
            catchBlockCode(e);
        }

        return userId;
    }

    public static String getSessionByUserId(String id) {
        String queryForUser = "SELECT * FROM session_tokens WHERE id = ?";
        String session = null;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(queryForUser)) {

            // Find the user in the database and create a User object
            pst1.setString(1, id);
            ResultSet rs = pst1.executeQuery();

            if (rs.next()) {
                session = rs.getString("session_token");
            }
        }
        catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not find session id", "Error finding the session id by user, try again later.\nERROR #5");
            catchBlockCode(e);
        }

        return session;
    }

    public static String getUserIdByUsername(String userUsername) {
        String queryForUser = "SELECT * FROM users WHERE username = ?";
        String userId = null;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(queryForUser)) {

            // Find the user in the database and create a User object
            pst1.setString(1, userUsername);
            ResultSet rs = pst1.executeQuery();

            if (rs.next()) {
                userId = rs.getString("id");
            }
        }
        catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not user id", "Error finding the user id by username, try again later.\nERROR #6");
            catchBlockCode(e);
        }

        return userId;
    }

    public static String getUsernameById(String userId) {
        String queryForUser = "SELECT * FROM users WHERE id = ?";
        String username = null;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(queryForUser)) {

            // Find the user in the database and create a User object
            pst1.setString(1, userId);
            ResultSet rs = pst1.executeQuery();

            if (rs.next()) {
                username = rs.getString("username");
            }
        }
        catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not username", "Error finding the username by user id, try again later.\nERROR #7");
            catchBlockCode(e);
        }

        return username;
    }

    // ------------------------------------------------------------------ //

    public static boolean addUserToDatabase(String userUsername, String userPassword, String userId, String userImageName) {
        String insertQueryUsers = "INSERT INTO users(username, password, id, image) VALUES(?, ?, ?, ?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement insertPstUsers = con.prepareStatement(insertQueryUsers)) {

            insertPstUsers.setString(1, userUsername);
            insertPstUsers.setString(2, userPassword);
            insertPstUsers.setString(3, userId);
            insertPstUsers.setString(4, userImageName);
            insertPstUsers.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not add user to database", "Error adding the user to the database, try again later.\nERROR #8");
            catchBlockCode(e);
            return false;
        }
        return true;
    }

    public static boolean addSessionToDataBase(String userId, String session) {
        String insertQuery = "INSERT INTO session_tokens (session_token, id) VALUES (?, ?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement insertPstUsers = con.prepareStatement(insertQuery)) {

            insertPstUsers.setString(1, session);
            insertPstUsers.setString(2, userId);
            insertPstUsers.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not add session to the database", "Error adding the user session to the database, try again later.\nERROR #8");
            catchBlockCode(e);
            return false;
        }
        return true;
    }

    public static int addConversationToDatabase(String sender, String receiver) {
        String insertQuery = "INSERT INTO conversations (participant1, participant2) VALUES (?, ?)";
        String selectQuery = "SELECT * FROM conversations WHERE participant1 = ? AND participant2 = ?";

        int returnConvId = -1;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(insertQuery);
             PreparedStatement pst2 = con.prepareStatement(selectQuery)) {

            String[] correctTableName = compareStrings(sender,receiver).split("_");

            String result = checkIfConversationExists(con, sender, receiver);
            if (result == null) {

                pst1.setString(1, correctTableName[0]);
                pst1.setString(2, correctTableName[1]);
                pst1.executeUpdate();

                pst2.setString(1, correctTableName[0]);
                pst2.setString(2, correctTableName[1]);
                ResultSet rs = pst2.executeQuery();

                if (rs.next()) {
                    returnConvId = rs.getInt("conversation_id");

                    String tableNameFromDatabase = checkIfTableExists(con, correctTableName[0] + "_" + correctTableName[1]);
                    if (tableNameFromDatabase == null) {
                        if (!createConversationMessagesTable(con, correctTableName[0] + "_" + correctTableName[1] + "_" + returnConvId)) {
                            PreparedStatement pst3 = con.prepareStatement("DELETE FROM conversations WHERE participant1 = ? AND participant2 = ?");
                            pst3.setString(1, correctTableName[0]);
                            pst3.setString(2, correctTableName[1]);
                            pst3.executeUpdate();
                            return -1; // if there was an error in creating the conversation in the database
                        }
                    }
                    else
                        return Integer.parseInt(tableNameFromDatabase.split("_")[2]);

                }

            } else {
                pst2.setString(1, correctTableName[0]);
                pst2.setString(2, correctTableName[1]);
                ResultSet rs = pst2.executeQuery();

                if (rs.next()) {
                    returnConvId = rs.getInt("conversation_id");
                    return returnConvId;
                }
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not add conversation to the database", "Error adding the conversation to the database, try again later.\nERROR #9");
            catchBlockCode(e);
        }

        return returnConvId;
    }

    public static boolean addMessageToDatabase(Message message, int conversation_id) {
        String tableName = compareStrings(message.getSender(), message.getReceiver());
        String insertQuery = "INSERT INTO " + tableName + "_" + conversation_id + " (sender_id, receiver_id, timestamp, message_text) VALUES (?, ?, ?, ?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(insertQuery)) {

            Timestamp timestamp = Timestamp.valueOf(message.getTimestamp());

            // Find all the conversations this user has in the database and for each one we will create a chat box
            pst1.setString(1, message.getSender());
            pst1.setString(2, message.getReceiver());
            pst1.setTimestamp(3, timestamp);
            pst1.setString(4, message.getText());
            pst1.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not add message to the database", "Error adding the message to the database, try again later.\nERROR #10");
            catchBlockCode(e);
            return false;
        }
        return true;
    }

    public static String checkIfConversationExists(Connection con, String sender, String receiver) {
        String query = "SELECT * FROM conversations WHERE participant1 = ? AND participant2 = ?";
        String[] correctName = compareStrings(sender,receiver).split("_");

        try (PreparedStatement pst = con.prepareStatement(query)) {

            pst.setString(1, correctName[0]);
            pst.setString(2, correctName[1]);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getString("conversation_id"); // if the conversation exists in the database then return the conversation_id
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not search for the conversation", "Error in searching for the conversation in the database, try again later.\nERROR #11");
            catchBlockCode(e);
        }

        return null;
    }

    private static String checkIfTableExists(Connection con, String tableNameToCheck) {
        String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_name LIKE ?";

        try (PreparedStatement pst = con.prepareStatement(query)) {

            pst.setString(1, DatabaseConfig.getDbName());
            pst.setString(2, "%" + tableNameToCheck + "%");

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getString("table_name");
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not search for the table in database", "Error in searching for the table in the database, try again later.\nERROR #12");
            catchBlockCode(e);
        }
        return null;
    }

    public static boolean createConversationMessagesTable(Connection con, String tableName) {
        String query = "CREATE TABLE IF NOT EXISTS " + tableName + " (message_id SERIAL PRIMARY KEY, sender_id VARCHAR(255) NOT NULL, receiver_id VARCHAR(255) NOT NULL, timestamp TIMESTAMP NOT NULL, message_text TEXT NOT NULL)";

        try (Statement stmt = con.createStatement()) {
            stmt.executeUpdate(query);
            System.out.println("A new table was created: " + tableName);
        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not create conversation table", "Error in creating a coversation table in the database, try again later.\nERROR #13");
            catchBlockCode(e);
            return false;
        }
        return true;
    }

    public static boolean isUsernameUnique(String userUsername) {
        String findUserQuery = "SELECT COUNT(*) as count FROM users WHERE username = ?";
        boolean usernameUnique = true;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(findUserQuery)) {

            pst.setString(1, userUsername);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                int count = rs.getInt("count");
                if (count > 0) {
                    System.out.println("in here");
                    usernameUnique = false;
                }
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could check username uniqueness", "Error in checking for username uniqueness, try again later.\nERROR #14");
            catchBlockCode(e);
        }

        return usernameUnique;
    }

    public static boolean isUserConnected(String userId) {
        String findUserQuery = "SELECT * FROM session_tokens WHERE id = ?";
        boolean connected = false;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(findUserQuery)) {

            // Find the user in the database and create a User object
            pst1.setString(1, userId);
            ResultSet rs = pst1.executeQuery();

            if (rs.next()) {
                return true;
            }
        }
        catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could check if user connected", "Error in checking if the user is connected to the database, try again later.\nERROR #15");
            catchBlockCode(e);
        }

        return connected;
    }

    public static boolean removeUserSession(String userId) {
        String removeQuery = "DELETE FROM session_tokens WHERE id = ?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(removeQuery)) {

            pst.setString(1, userId);
            pst.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not remove user", "Error in removing a user from the database, try again later.\nERROR #16");
            catchBlockCode(e);
            return false;
        }
        return true;
    }

    public static boolean deleteUser(String userId) {
        String deleteQueryUsers = "DELETE FROM users WHERE id = ?";
        String deleteQuerySession = "DELETE FROM session_tokens WHERE id = ?";
        String deleteQueryConversations = "DELETE FROM conversations WHERE participant1 = ? OR participant2 = ?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(deleteQueryUsers);
             PreparedStatement pst2 = con.prepareStatement(deleteQuerySession);
             PreparedStatement pst3 = con.prepareStatement(deleteQueryConversations)) {

            pst1.setString(1, userId);
            pst1.executeUpdate();

            pst2.setString(1, userId);
            pst2.executeUpdate();

            ArrayList<String> chatTableNames = Database.findAllChats(con, userId);
            if (!chatTableNames.isEmpty()) {
                for (String name : chatTableNames) {
                    PreparedStatement pst = con.prepareStatement("DROP TABLE " + name);
                    pst.executeUpdate();
                }
            }

            pst3.setString(1, userId);
            pst3.setString(2, userId);
            pst3.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not delete user", "Error in deleting a user from the database, try again later.\nERROR #17");
            catchBlockCode(e);
            return false;
        }
        return true;
    }

    private static ArrayList<String> findAllChats(Connection con, String userId) {
        ArrayList<String> list = new ArrayList<String>();
        String query = "SELECT * FROM conversations WHERE participant1 = ? OR participant2 = ?";

        try (PreparedStatement pst = con.prepareStatement(query)) {

            pst.setString(1, userId);
            pst.setString(2, userId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                String tableName = rs.getString("participant1") + "_" + rs.getString("participant2") + "_" + rs.getString("conversation_id");
                list.add(tableName);
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not search for chats", "Error in searching for user chats in the database, try again later.\nERROR #18");
            catchBlockCode(e);
        }

        return list;
    }

    public static boolean cleanUserSessionTabel() {
        String removeQuery = "DELETE FROM session_tokens";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(removeQuery)) {

            pst.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not clean sessions table", "Error in cleaning the sessions table in the database.\nERROR #19");
            catchBlockCode(e);
            return false;
        }
        return true;
    }

    public static boolean isUserIdUnique(String userId) {
        String findUserQuery = "SELECT COUNT(*) as count FROM users WHERE id = ?";
        boolean userIdUnique = true;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(findUserQuery)) {

            pst.setString(1, userId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                int count = rs.getInt("count");
                if (count > 0) {
                    System.out.println("the username is not unique");
                    userIdUnique = false;
                }
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not assert uniqueness", "Error in checking for uniqueness of user id the database, try again later.\nERROR #20");
            catchBlockCode(e);
            return false;
        }

        return userIdUnique;
    }


    // ------------------------------------------------------------------ //

    public static boolean updateUsernameInDatabase(String newUsername, String oldUsername) {
        String updateQuery = "UPDATE users SET username = ? WHERE username = ?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(updateQuery)) {

            pst.setString(1, newUsername);
            pst.setString(2, oldUsername);
            pst.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not update username", "Error in updating the username in the database, try again later.\nERROR #21");
            catchBlockCode(e);
            return false;
        }
        return true;
    }

    public static boolean updatePasswordInDatabase(String newPassword, String username) {
        String updateQuery = "UPDATE users SET password = ? WHERE username = ?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(updateQuery)) {

            pst.setString(1, newPassword);
            pst.setString(2, username);
            pst.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not update password", "Error in updating the password in the database, try again later.\nERROR #22");
            catchBlockCode(e);
            return false;
        }
        return true;
    }

    public static boolean updateAvatarInDatabase(String newAvatar, String username) {
        String updateQuery = "UPDATE users SET image = ? WHERE username = ?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(updateQuery)) {

            pst.setString(1, newAvatar);
            pst.setString(2, username);
            pst.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not update avatar", "Error in updating the avatar in the database, try again later.\nERROR #23");
            catchBlockCode(e);
            return false;
        }
        return true;
    }


    // ------------------------------------------------------------------ //

    // Helper Functions
    public static String compareStrings(String sender, String receiver) {
        if (sender.compareTo(receiver) <= 0)
            return sender + "_" + receiver;
        else
            return receiver + "_" + sender;
    }

    private static void catchBlockCode(SQLException e) {
        Logger lgr = Logger.getLogger(Database.class.getName());
        lgr.log(Level.SEVERE, e.getMessage(), e);
    }

    private static void showAlertWithMessage(Alert.AlertType type, String title, String errorMessage) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(errorMessage);
            alert.showAndWait();
        });
    }

}
