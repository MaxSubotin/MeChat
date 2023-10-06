package app.mechat;

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
                    userFromDatabase = new User(rs.getString("username"), rs.getString("id"));
            }
        }
        catch (SQLException e) {
            catchBlockCode(e);
        }

        return userFromDatabase;
    }



    public static ArrayList<Chat> getUsersChatsFromDatabase(String userId) {
        String queryForConversations = "SELECT * FROM conversations WHERE participant1 = ? OR participant2 = ?";
        ArrayList<Chat> usersChats = new ArrayList<Chat>();

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(queryForConversations)) {

            // Find all the conversations this user has in the database and for each one we will create a chat box
            pst1.setString(1, userId);
            pst1.setString(2, userId);
            ResultSet rs = pst1.executeQuery();

            while (rs.next()) {
                if (Objects.equals(rs.getString("participant1"), userId))
                    usersChats.add(new Chat(
                            null,
                            userId,
                            rs.getString("participant2"),
                            rs.getInt("conversation_id"))
                    );
                else
                    usersChats.add(new Chat(
                            null,
                            userId,
                            rs.getString("participant1"),
                            rs.getInt("conversation_id"))
                    );
            }

        } catch (SQLException e) {
            catchBlockCode(e);
        }

        return usersChats;
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
            catchBlockCode(e);
        }

        return userId;
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
            catchBlockCode(e);
        }

        return username;
    }

    // ------------------------------------------------------------------ //

    public static void addUserToDatabase(String userUsername, String userPassword, String userId) {
        String insertQueryUsers = "INSERT INTO users(username, password, id) VALUES(?, ?, ?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement insertPstUsers = con.prepareStatement(insertQueryUsers)) {

            insertPstUsers.setString(1, userUsername);
            insertPstUsers.setString(2, userPassword);
            insertPstUsers.setString(3, userId);
            insertPstUsers.executeUpdate();

        } catch (SQLException e) {
            catchBlockCode(e);
        }
    }

    public static void addSessionToDataBase(String userId, String session) {
        String insertQuery = "INSERT INTO session_tokens (session_token, id) VALUES (?, ?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement insertPstUsers = con.prepareStatement(insertQuery)) {

            insertPstUsers.setString(1, session);
            insertPstUsers.setString(2, userId);
            insertPstUsers.executeUpdate();

        } catch (SQLException e) {
            catchBlockCode(e);
        }
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
                    if (tableNameFromDatabase == null)
                        createConversationMessagesTable(con, correctTableName[0] + "_" + correctTableName[1] + "_" + returnConvId);
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
            catchBlockCode(e);
        }

        return returnConvId;
    }

    public static void addMessageToDatabase(Message message, int conversation_id) {
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
            catchBlockCode(e);
        }
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
            catchBlockCode(e);
        }
        return null;
    }

    public static void createConversationMessagesTable(Connection con, String tableName) {
        String query = "CREATE TABLE IF NOT EXISTS " + tableName + " (message_id SERIAL PRIMARY KEY, sender_id VARCHAR(255) NOT NULL, receiver_id VARCHAR(255) NOT NULL, timestamp TIMESTAMP NOT NULL, message_text TEXT NOT NULL)";

        try (Statement stmt = con.createStatement()) {

            stmt.executeUpdate(query);
            System.out.println("A new table was created: " + tableName);
        } catch (SQLException e) {
            catchBlockCode(e);
        }
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
            catchBlockCode(e);
        }

        return connected;
    }

    public static void removeUserSession(String userId) {
        String removeQuery = "DELETE FROM session_tokens WHERE id = ?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(removeQuery)) {

            pst.setString(1, userId);
            pst.executeUpdate();

        } catch (SQLException e) {
            catchBlockCode(e);
        }

    }

    public static void cleanUserSessionTabel() {
        String removeQuery = "DELETE FROM session_tokens";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(removeQuery)) {

            pst.executeUpdate();

        } catch (SQLException e) {
            catchBlockCode(e);
        }
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
                    userIdUnique = false;
                }
            }

        } catch (SQLException e) {
            catchBlockCode(e);
        }

        return userIdUnique;
    }


    // ------------------------------------------------------------------ //

    public static void updateUsernameInDatabase(String newUsername, String oldUsername) {
        String updateQuery = "UPDATE users SET username = ? WHERE username = ?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(updateQuery)) {

            pst.setString(1, newUsername);
            pst.setString(2, oldUsername);
            pst.executeUpdate();

        } catch (SQLException e) {
            catchBlockCode(e);
        }
    }

    public static void updatePasswordInDatabase(String newPassword, String username) {
        String updateQuery = "UPDATE users SET password = ? WHERE username = ?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(updateQuery)) {

            pst.setString(1, newPassword);
            pst.setString(2, username);
            pst.executeUpdate();

        } catch (SQLException e) {
            catchBlockCode(e);
        }
    }


    // ------------------------------------------------------------------ //

    // Helper Functions
    public static String compareStrings(String sender, String receiver) {
        if (sender.compareTo(receiver) <= 0)
            return sender + "_" + receiver;
        else
            return receiver + "_" + sender;
    }

//
//    // ------------------------------------------------------------------ //
//
//    public static void deleteRowsByUsername(String userUsername) {
//        String deleteQuery = "DELETE FROM users_games WHERE username = ?";
//        String updateQuery = "UPDATE users_info SET saved_games = ? WHERE username = ?";
//
//        try (Connection con = DatabaseConfig.getConnection();
//             PreparedStatement pstDelete = con.prepareStatement(deleteQuery);
//             PreparedStatement pstUpdate = con.prepareStatement(updateQuery)) {
//
//            pstDelete.setString(1, userUsername);
//            pstDelete.executeUpdate();
//
//            pstUpdate.setInt(1,1);
//            pstUpdate.setString(2,userUsername);
//            pstUpdate.executeUpdate();
//
//        } catch (SQLException e) {
//            catchBlockCode(e);
//        }
//    }
//
//    // ------------------------------------------------------------------ //
//
    private static void catchBlockCode(SQLException e) {
        Logger lgr = Logger.getLogger(Database.class.getName());
        lgr.log(Level.SEVERE, e.getMessage(), e);
    }



//
//    private static SudokuGameData turnJsonIntoArray(ResultSet rs) throws SQLException {
//        String json = rs.getString("game");
//        Gson gson = new Gson();
//        int[][] first = gson.fromJson(json, int[][].class);
//
//        json = rs.getString("solution");
//        gson = new Gson();
//        int[][] second = gson.fromJson(json, int[][].class);
//
//        return new SudokuGameData(first, second, rs.getString("difficulty"), rs.getString("timer"), rs.getInt("mistakes"));
//    }

}
