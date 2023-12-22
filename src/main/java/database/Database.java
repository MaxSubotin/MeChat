package database;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import util.*;
import at.favre.lib.crypto.bcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

// error counter: 26

public class Database {

    public static User getUserFromDatabase(String userUsername, char[] userPassword) { // 🟢
        String queryForUser = "SELECT * FROM users WHERE username = ?";
        User userFromDatabase = null;

        //long startTime = System.currentTimeMillis(); // Record the start time

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(queryForUser)) {

            // Find the user in the database and create a User object
            pst1.setString(1, userUsername);
            try (ResultSet rs = pst1.executeQuery()) {
                if (rs.next()) {
                    BCrypt.Result result = BCrypt.verifyer().verify(userPassword, rs.getString("password"));
                    if (result.verified)
                        userFromDatabase = new User(rs.getString("username"), rs.getString("id"), rs.getString("image"));
                }
            }
        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "User Not Found", "User not found, please try again later.\nERROR #1");
            catchBlockCode(e); // User not found error is handled in the MainViewController file
        }
//        finally {
//            long endTime = System.currentTimeMillis(); // Record the end time
//            long executionTime = endTime - startTime; // Calculate the execution time
//            System.out.println("Method execution time: " + executionTime + " milliseconds");
//        }

        return userFromDatabase;
    }

    public static ArrayList<Chat> getUsersChatsFromDatabase(String userId) { // 🟢
        String firstQuery = "SELECT * FROM chat_participants WHERE user_id = ?";
        String secondQuery = "SELECT * FROM chats WHERE chat_id = ?";
        ArrayList<Chat> usersChats = new ArrayList<>();

        //long startTime = System.currentTimeMillis(); // Record the start time

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(firstQuery);
             PreparedStatement pst2 = con.prepareStatement(secondQuery)) {

            // Find all the conversations this user has in the database and for each one we will create a chat box
            pst1.setString(1, userId);

            try (ResultSet rs = pst1.executeQuery()) {
                while (rs.next()) {
                    String chatId = rs.getString("chat_id");

                    pst2.setString(1, chatId);
                    try (ResultSet rs2 = pst2.executeQuery()) {
                        if (rs2.next()) {
                            if (Objects.equals(rs2.getString("chat_type"), "group")) {
                                usersChats.add(new GroupChat(
                                        null,
                                        userId,
                                        getGroupChatParticipants(con, chatId),
                                        chatId,
                                        0,
                                        rs2.getString("chat_name"),
                                        getGroupChatAdmin(con, chatId)
                                ));
                            } else {
                                usersChats.add(new RegularChat(
                                        null,
                                        userId,
                                        getSecondRegularChatParticipant(con, chatId, userId),
                                        chatId,
                                        0
                                ));
                            }
                        }
                    }
                }
            }

        } catch (SQLException e) {
            catchBlockCode(e);
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not load user chats", "Could not load the user chats for user: " + userId + "\nERROR #2");
            usersChats = null;
        }
//        finally {
//            long endTime = System.currentTimeMillis(); // Record the end time
//            long executionTime = endTime - startTime; // Calculate the execution time
//            System.out.println("Method execution time: " + executionTime + " milliseconds");
//        }

        return usersChats;
    }


    public static ArrayList<Message> getChatMessagesFromDatabase(String tableName) { // 🟢 // tableName is also the chatId
        String firstQuery = "SELECT * FROM chats WHERE chat_id = ?";
        String secondQuery = "SELECT * FROM " + tableName;
        ArrayList<Message> chatMessages = new ArrayList<>();
        String chatType;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(firstQuery);
             PreparedStatement pst2 = con.prepareStatement(secondQuery)) {

            // Find all the conversations this user has in the database and for each one we will create a chat box
            pst1.setString(1, tableName);
            try (ResultSet rs1 = pst1.executeQuery()) {
                if (rs1.next()) {
                    chatType = rs1.getString("chat_type");

                    try (ResultSet rs2 = pst2.executeQuery()) {
                        if (Objects.equals(chatType, "group")) {
                            while (rs2.next()) {
                                chatMessages.add(new GroupMessage(rs2.getString("message_text"), rs2.getString("sender_id"), rs2.getString("timestamp"), tableName, getGroupChatParticipants(con, tableName)));
                            }
                        } else {
                            while (rs2.next()) {
                                chatMessages.add(new RegularMessage(rs2.getString("message_text"), rs2.getString("sender_id"), rs2.getString("timestamp"), tableName, getSecondRegularChatParticipant(con, tableName, rs2.getString("sender_id"))));
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            catchBlockCode(e);
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not load table", "Could not load the table, table name: " + tableName + "\nERROR #3");
            chatMessages = null;
        }

        return chatMessages;
    }

    public static String getUserIdBySession(String session) { // 🟢
        String queryForUser = "SELECT * FROM session_tokens WHERE session_token = ?";
        String userId = null;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(queryForUser)) {

            // Find the user in the database and create a User object
            pst1.setString(1, session);
            try (ResultSet rs = pst1.executeQuery()) {
                if (rs.next()) {
                    userId = rs.getString("id");
                }
            }
        }
        catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not find user", "Error finding the user by session id, try again later.\nERROR #4");
            catchBlockCode(e);
        }

        return userId;
    }

    public static String getSessionByUserId(String id) { // 🟢
        String queryForUser = "SELECT * FROM session_tokens WHERE id = ?";
        String session = null;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(queryForUser)) {

            // Find the user in the database and create a User object
            pst1.setString(1, id);
            try (ResultSet rs = pst1.executeQuery()) {
                if (rs.next()) {
                    session = rs.getString("session_token");
                }
            }
        }
        catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not find session id", "Error finding the session id by user, try again later.\nERROR #5");
            catchBlockCode(e);
        }

        return session;
    }

    public static String getUserIdByUsername(String userUsername) { // 🟢
        String queryForUser = "SELECT * FROM users WHERE username = ?";
        String userId = null;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(queryForUser)) {

            // Find the user in the database and create a User object
            pst1.setString(1, userUsername);
            try (ResultSet rs = pst1.executeQuery()) {
                if (rs.next()) {
                    userId = rs.getString("id");
                }
            }
        }
        catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not get user id", "Error finding the user id by username, try again later.\nERROR #6");
            catchBlockCode(e);
        }

        return userId;
    }

    public static String getUsernameById(String userId) { // 🟢
        String queryForUser = "SELECT * FROM users WHERE id = ?";
        String username = null;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(queryForUser)) {

            // Find the user in the database and create a User object
            pst1.setString(1, userId);
            try (ResultSet rs = pst1.executeQuery()) {
                if (rs.next()) {
                    username = rs.getString("username");
                }
            }
        }
        catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not get username", "Error finding the username by user id, try again later.\nERROR #7");
            catchBlockCode(e);
        }

        return username;
    }

    public static String getUserImageById(String userId) { // 🟢
        String query = "SELECT * FROM users WHERE id = ?";
        String userImage = "male.png";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(query)) {

            // Find the user in the database and create a User object
            pst1.setString(1, userId);
            try (ResultSet rs = pst1.executeQuery()) {
                if (rs.next()) {
                    userImage = rs.getString("image") + ".png";
                }
            }
        }
        catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not get user image", "Error finding the user image by user id, try again later.\nERROR #25");
            catchBlockCode(e);
        }

        return userImage;
    }

    public static User getUserByUserId(String userId) { // 🟢
        String query = "SELECT * FROM users WHERE id = ?";
        User returnedUser = null;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(query)) {

            // Find the user in the database and create a User object
            pst1.setString(1, userId);
            try (ResultSet rs = pst1.executeQuery()) {
                if (rs.next()) {
                    returnedUser = new User(rs.getString("username"), userId, rs.getString("image"));
                }
            }
        }
        catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not get user image", "Error finding the user image by user id, try again later.\nERROR #25");
            catchBlockCode(e);
        }

        return returnedUser;
    }

    // ------------------------------------------------------------------ //

    public static boolean addUserToDatabase(String userUsername, String userPassword, String userId, String userImageName) { // 🟢
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

    public static boolean addSessionToDataBase(String userId, String session) { // 🟢
        String insertQuery = "INSERT INTO session_tokens (session_token, id) VALUES (?, ?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement insertPstUsers = con.prepareStatement(insertQuery)) {

            insertPstUsers.setString(1, session);
            insertPstUsers.setString(2, userId);
            insertPstUsers.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not add session to the database", "Error adding the user session to the database, try again later.\nERROR #24");
            catchBlockCode(e);
            return false;
        }
        return true;
    }

//    public static int addConversationToDatabase(String sender, String receiver) { // 🟣 TO BE DELETED
//        String insertQuery = "INSERT INTO conversations (participant1, participant2) VALUES (?, ?)";
//        String selectQuery = "SELECT * FROM conversations WHERE participant1 = ? AND participant2 = ?";
//
//        int returnConvId = -1;
//
//        try (Connection con = DatabaseConfig.getConnection();
//             PreparedStatement pst1 = con.prepareStatement(insertQuery);
//             PreparedStatement pst2 = con.prepareStatement(selectQuery)) {
//
//            String[] correctTableName = compareStrings(sender,receiver).split("_");
//
//            String result = checkIfConversationExists(con, sender, receiver);
//            if (result == null) {
//
//                pst1.setString(1, correctTableName[0]);
//                pst1.setString(2, correctTableName[1]);
//                pst1.executeUpdate();
//
//                pst2.setString(1, correctTableName[0]);
//                pst2.setString(2, correctTableName[1]);
//                ResultSet rs = pst2.executeQuery();
//
//                if (rs.next()) {
//                    returnConvId = rs.getInt("conversation_id");
//
//                    String tableNameFromDatabase = checkIfTableExists(con, correctTableName[0] + "_" + correctTableName[1]);
//                    if (tableNameFromDatabase == null) {
//                        if (!createConversationMessagesTable(con, correctTableName[0] + "_" + correctTableName[1] + "_" + returnConvId)) {
//                            PreparedStatement pst3 = con.prepareStatement("DELETE FROM conversations WHERE participant1 = ? AND participant2 = ?");
//                            pst3.setString(1, correctTableName[0]);
//                            pst3.setString(2, correctTableName[1]);
//                            pst3.executeUpdate();
//                            return -1; // if there was an error in creating the conversation in the database
//                        }
//                    }
//                    else
//                        return Integer.parseInt(tableNameFromDatabase.split("_")[2]);
//
//                }
//
//            } else {
//                pst2.setString(1, correctTableName[0]);
//                pst2.setString(2, correctTableName[1]);
//                ResultSet rs = pst2.executeQuery();
//
//                if (rs.next()) {
//                    returnConvId = rs.getInt("conversation_id");
//                    return returnConvId;
//                }
//            }
//
//        } catch (SQLException e) {
//            showAlertWithMessage(Alert.AlertType.ERROR, "Could not add conversation to the database", "Error adding the conversation to the database, try again later.\nERROR #9");
//            catchBlockCode(e);
//        }
//
//        return returnConvId;
//    }

    public static boolean addRegularMessageToDatabase(RegularMessage message, String chatId) { // 🟢
        //String tableName = compareStrings(message.getSender(), message.getReceiver());
        String insertQuery = "INSERT INTO " + chatId + " (chat_id, sender_id, timestamp, message_text) VALUES (?, ?, ?, ?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(insertQuery)) {

            Timestamp timestamp = Timestamp.valueOf(message.getTimestamp());

            // Find all the conversations this user has in the database and for each one we will create a chat box
            pst1.setString(1, message.getChatId());
            pst1.setString(2, message.getSender());
            pst1.setTimestamp(3, timestamp);
            pst1.setString(4, message.getText());
            pst1.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not add regular message to the database", "Error adding the regular message to the database, try again later.\nERROR #10");
            catchBlockCode(e);
            return false;
        }
        return true;
    }

    public static boolean addGroupMessageToDatabase(GroupMessage message, String chatId) { // 🟢
        //String tableName = compareStrings(message.getSender(), message.getReceiver());
        String insertQuery = "INSERT INTO " + chatId + " (chat_id, sender_id, timestamp, message_text) VALUES (?, ?, ?, ?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(insertQuery)) {

            Timestamp timestamp = Timestamp.valueOf(message.getTimestamp());

            // Find all the conversations this user has in the database and for each one we will create a chat box
            pst1.setString(1, message.getChatId());
            pst1.setString(2, message.getSender());
            pst1.setTimestamp(3, timestamp);
            pst1.setString(4, message.getText());
            pst1.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not add group message to the database", "Error adding the group message to the database, try again later.\nERROR #10");
            catchBlockCode(e);
            return false;
        }
        return true;
    }

    public static String checkIfConversationExists(Connection con, String sender, String receiver) { // 🟢
        String query = "SELECT DISTINCT cp1.chat_id " +
                "FROM chat_participants cp1 " +
                "JOIN chat_participants cp2 ON cp1.chat_id = cp2.chat_id " +
                "WHERE cp1.user_id = ? AND cp2.user_id = ?";
        String query2 = "SELECT * FROM chats WHERE chat_id = ?";

        try (PreparedStatement pst1 = con.prepareStatement(query);
             PreparedStatement pst2 = con.prepareStatement(query2)) {

            pst1.setString(1, sender);
            pst1.setString(2, receiver);

            try (ResultSet rs1 = pst1.executeQuery()) {
                while (rs1.next()) {
                    pst2.setString(1, rs1.getString("chat_id"));
                    try (ResultSet rs2 = pst2.executeQuery()) {
                        if (rs2.next()) {
                            if (Objects.equals(rs2.getString("chat_type"), "regular"))
                                return rs1.getString("chat_id");
                        }
                    }
                }
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not search for the conversation", "Error in searching for the conversation in the database, try again later.\nERROR #11");
            catchBlockCode(e);
        }
        return null;
    }

    private static String checkIfTableExists(Connection con, String tableNameToCheck) { // 🟢
        String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_name LIKE ?";

        try (PreparedStatement pst = con.prepareStatement(query)) {

            pst.setString(1, DatabaseConfig.getDbName());
            pst.setString(2, "%" + tableNameToCheck + "%");

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("table_name");
                }
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not search for the table in database", "Error in searching for the table in the database, try again later.\nERROR #12");
            catchBlockCode(e);
        }
        return null;
    }

    public static boolean createChatMessagesTable(Connection con, String tableName) { // 🟢
        String query = "CREATE TABLE IF NOT EXISTS " + tableName + " (chat_id VARCHAR(255) NOT NULL, sender_id VARCHAR(255) NOT NULL, timestamp TIMESTAMP NOT NULL, message_text TEXT NOT NULL)";

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

    public static boolean isUsernameUnique(String userUsername) { // 🟢
        String findUserQuery = "SELECT COUNT(*) as count FROM users WHERE username = ?";
        boolean usernameUnique = true;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(findUserQuery)) {

            pst.setString(1, userUsername);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    if (count > 0) {
                        usernameUnique = false;
                    }
                }
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could check username uniqueness", "Error in checking for username uniqueness, try again later.\nERROR #14");
            catchBlockCode(e);
        }

        return usernameUnique;
    }

    public static boolean isGroupNameUnique(String groupName) { // 🟢
        String findUserQuery = "SELECT COUNT(*) as count FROM chats WHERE chat_name = ?";
        boolean usernameUnique = true;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(findUserQuery)) {

            pst.setString(1, groupName);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    if (count > 0) {
                        usernameUnique = false;
                    }
                }
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could check group name uniqueness", "Error in checking for group name uniqueness, try again later.\nERROR #14");
            catchBlockCode(e);
        }

        return usernameUnique;
    }

    public static boolean isUserConnected(String userId) { // 🟢
        String findUserQuery = "SELECT * FROM session_tokens WHERE id = ?";
        boolean connected = false;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(findUserQuery)) {

            // Find the user in the database and create a User object
            pst1.setString(1, userId);
            try (ResultSet rs = pst1.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could check if user connected", "Error in checking if the user is connected to the database, try again later.\nERROR #15");
            catchBlockCode(e);
        }

        return connected;
    }

    public static boolean removeUserSession(String userId) { // 🟢
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


    public static boolean deleteUser(String userId) { // 🟢
        String deleteQueryUsers = "DELETE FROM users WHERE id = ?";
        String deleteQuerySession = "DELETE FROM session_tokens WHERE id = ?";
        String deleteQueryChatParticipant = "DELETE FROM chat_participants WHERE user_id = ?";
        String deleteQueryChats = "DELETE FROM chats WHERE chat_id = ?";

        String isGroupChatQuery = "SELECT * FROM chats WHERE chat_id = ?";
        String numOfUsersInGroup = "SELECT COUNT(*) AS num_users FROM chat_participants WHERE chat_id = ?";

        // find all chats, delete all regular chats, remove participant form all group chats, if admin appoint a new one, if exactly 1 participant then delete group

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(deleteQueryUsers);
             PreparedStatement pst1 = con.prepareStatement(deleteQuerySession);
             PreparedStatement pst2 = con.prepareStatement(deleteQueryChatParticipant);
             PreparedStatement pst3 = con.prepareStatement(deleteQueryChats);
             PreparedStatement pst4 = con.prepareStatement(isGroupChatQuery);
             PreparedStatement pst5 = con.prepareStatement(numOfUsersInGroup)) {

            pst.setString(1, userId);
            pst.executeUpdate();

            pst1.setString(1, userId);
            pst1.executeUpdate();

            ArrayList<String> chatIds = Database.findAllChats(con, userId);
            if (!chatIds.isEmpty()) {
                for (String chatId : chatIds) {

                    pst4.setString(1, chatId);
                    try {
                        try (ResultSet rs1 = pst4.executeQuery()) {
                            if (rs1.next()) {
                                if (Objects.equals(rs1.getString("chat_type"), "regular")) deleteRegularChat(chatId);
                                else {

                                    pst5.setString(1, chatId);
                                    try (ResultSet rs2 = pst5.executeQuery()) {
                                        if (rs2.next()) {
                                            if (rs2.getInt("num_users") == 1) {
                                                pst3.setString(1, chatId);
                                                pst3.executeUpdate();
                                                try (PreparedStatement pst6 = con.prepareStatement("DROP TABLE " + chatId)) {
                                                    pst6.executeUpdate();
                                                }
                                            } else {
                                                String groupAdmin = Database.getGroupChatAdmin(con, chatId);
                                                if (Objects.equals(userId, groupAdmin)) {
                                                    ArrayList<String> participants = Database.getGroupChatParticipants(con, chatId);
                                                    if (participants != null) {
                                                        participants.remove(userId);
                                                        Database.updateGroupChatAdmin(chatId, userId, participants.get(0));
                                                    }
                                                }

                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    catch (SQLException e) {
                        throw new SQLException();
                    }
                }
            }

            pst2.setString(1, userId);
            pst2.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not delete user", "Error in deleting a user from the database, try again later.\nERROR #17");
            catchBlockCode(e);
            return false;
        }
        return true;
    }

    public static boolean deleteRegularChat(String chat_id) { // 🟢
        // remove the conversation and remove the conversation table

        String removeChatQuery = "DELETE FROM chats WHERE chat_id = ?";
        String removeParticipantsQuery = "DELETE FROM chat_participants WHERE chat_id = ?";
        String removeTableQuery = "DROP TABLE " + chat_id;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(removeChatQuery);
             PreparedStatement pst2 = con.prepareStatement(removeParticipantsQuery)) {

            pst1.setString(1, chat_id);
            pst1.executeUpdate();

            pst2.setString(1, chat_id);
            pst2.executeUpdate();

            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate(removeTableQuery);
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not delete regular chat", "Error in deleting a regular chat from the database, try again later.\nERROR #26");
            catchBlockCode(e);
            return false;
        }

        return true;
    }

    public static boolean deleteMessageBubble(Message message) { // 🟢
        // Find the message in the database using the Text and Timestamp and Change the text of the message in the database to: --< this message was deleted >--
        String updateQuery = "UPDATE " + message.getChatId() + " SET message_text = ? WHERE timestamp = ? AND message_text = ?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(updateQuery)) {

            Timestamp timestamp = Timestamp.valueOf(message.getTimestamp());

            pst1.setString(1, "--< this message was deleted >--");
            pst1.setTimestamp(2, timestamp);
            pst1.setString(3, message.getText());
            pst1.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not delete message", "Error in deleting a message from the database, try again later.\nERROR #26");
            catchBlockCode(e);
            return false;
        }

        return true;
    }

    private static ArrayList<String> findAllChats(Connection con, String userId) { // 🟢
        ArrayList<String> list = new ArrayList<String>();
        String query = "SELECT * FROM chat_participants WHERE user_id = ?";

        try (PreparedStatement pst = con.prepareStatement(query)) {

            pst.setString(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("chat_id"));
                }
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not search for chats", "Error in searching for user chats in the database, try again later.\nERROR #18");
            catchBlockCode(e);
        }

        return list;
    }

    public static boolean cleanUserSessionTable() { // 🟢
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

    public static boolean isUserIdUnique(String userId) { // 🟢
        String findUserQuery = "SELECT COUNT(*) as count FROM users WHERE id = ?";
        boolean userIdUnique = true;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(findUserQuery)) {

            pst.setString(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    if (count > 0) {
                        System.out.println("the username is not unique");
                        userIdUnique = false;
                    }
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

    public static boolean updateUsernameInDatabase(String newUsername, String oldUsername) { // 🟢
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

    public static boolean updatePasswordInDatabase(String newPassword, String username) { // 🟢
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

    public static boolean updateAvatarInDatabase(String newAvatar, String username) { // 🟢
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


    // ----------------------- Group Chat Stuff ------------------------- //

    public static String getGroupIdByName(String groupName) { // 🟢
        String query = "SELECT * FROM chats WHERE chat_name = ?";
        String groupId = null;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            pst.setString(1, groupName);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    groupId = rs.getString("chat_id");
                }
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not get group id", "Error in getting group id from the database, try again later.\nERROR #23");
            catchBlockCode(e);
        }

        return groupId;
    }

    public static String getGroupNameByGroupId(String groupId) { // 🟢
        String query = "SELECT * FROM chats WHERE chat_id = ?";
        String groupName = null;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            pst.setString(1, groupId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    groupName = rs.getString("chat_name");
                }
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not get group name", "Error in getting group name from the database, try again later.\nERROR #23");
            catchBlockCode(e);
        }

        return groupName;
    }

    public static boolean updateGroupName(String group_id, String newName) {
        String query = "UPDATE chats SET chat_name = ? WHERE chat_id = ?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            pst.setString(1, newName);
            pst.setString(2, group_id);
            pst.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not update group name", "Error in updating group name in the database, try again later.\nERROR #23");
            catchBlockCode(e);
            return false;
        }

        return true;
    }

    public static boolean addRegularChatToDatabase(RegularChat regularChat) { // 🟢
        String addChatQuery = "INSERT INTO chats(chat_id, chat_type, chat_name) VALUES(?,?,?)";
        String addParticipantsQuery = "INSERT INTO chat_participants(chat_id, user_id, is_admin) VALUES(?,?,?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(addChatQuery);
             PreparedStatement pst2 = con.prepareStatement(addParticipantsQuery)) {

            if (Database.checkIfConversationExists(con, regularChat.getSender(), regularChat.getReceiver()) == null) {

                pst1.setString(1, regularChat.getChatId());
                pst1.setString(2, "regular");
                pst1.setString(3, "null");
                pst1.executeUpdate();

                for (int i = 0; i < 2; i++) {
                    pst2.setString(1, regularChat.getChatId());
                    pst2.setString(2, i == 0 ? regularChat.getSender() : regularChat.getReceiver());
                    pst2.setBoolean(3, false);
                    pst2.executeUpdate();
                }

                if (Database.checkIfTableExists(con, regularChat.getChatId()) == null)
                    Database.createChatMessagesTable(con, regularChat.getChatId());
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not add regular chat", "Error in adding regular chat to the database, try again later.\nERROR #23");
            catchBlockCode(e);
            return false;
        }

        return true;
    }
    public static boolean addGroupChatToDatabase(GroupChat groupChat) { // 🟢
        String addChatQuery = "INSERT INTO chats(chat_id, chat_type, chat_name) VALUES(?,?,?)";
        String addParticipantsQuery = "INSERT INTO chat_participants(chat_id, user_id, is_admin) VALUES(?,?,?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(addChatQuery);
             PreparedStatement pst2 = con.prepareStatement(addParticipantsQuery)) {

            pst1.setString(1, groupChat.getChatId());
            pst1.setString(2, "group");
            pst1.setString(3, groupChat.getGroupName());
            pst1.executeUpdate();

            for (String userId : groupChat.getReceivers()) {
                pst2.setString(1, groupChat.getChatId());
                pst2.setString(2, userId);
                pst2.setBoolean(3, Objects.equals(groupChat.getAdmin(), userId));
                pst2.executeUpdate();
            }

            if (Database.checkIfTableExists(con, groupChat.getChatId()) == null)
                Database.createChatMessagesTable(con, groupChat.getChatId());

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not add group chat", "Error in adding group chat to the database, try again later.\nERROR #23");
            catchBlockCode(e);
            return false;
        }

        return true;
    }

    public static String getSecondRegularChatParticipant(Connection connection, String chatId, String firstParticipant) { // 🟢
        String query = "SELECT * FROM chat_participants WHERE chat_id = ?";
        String secondParticipantId = null;

        try (PreparedStatement pst = connection.prepareStatement(query)) {

            pst.setString(1, chatId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    if (!Objects.equals(firstParticipant, rs.getString("user_id"))) {
                        secondParticipantId = rs.getString("user_id");
                        break;
                    }
                }
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not get second participant", "Error in getting second participant id from the database, try again later.\nERROR #23");
            catchBlockCode(e);
        }

        return secondParticipantId;
    }

    public static ArrayList<String> getGroupChatParticipants(Connection connection, String chatId) { // 🟢
        String query = "SELECT * FROM chat_participants WHERE chat_id = ?";
        ArrayList<String> participants = new ArrayList<>();

        try (PreparedStatement pst = connection.prepareStatement(query)) {

            pst.setString(1, chatId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    participants.add(rs.getString("user_id"));
                }
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not get group participants", "Error in getting group participants from the database, try again later.\nERROR #23");
            catchBlockCode(e);
            return null;
        }

        return participants;
    }

    public static boolean isUserInGroupChat(String userId, String groupId) {
        String findUserQuery = "SELECT COUNT(*) as count FROM chat_participants WHERE chat_id = ? AND user_id = ?";
        boolean userInGroup = false;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(findUserQuery)) {

            pst.setString(1, groupId);
            pst.setString(2, userId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    if (count > 0) {
                        System.out.println("the username is already in the group");
                        userInGroup = true;
                    }
                }
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not assert uniqueness", "Error in checking for uniqueness of user in group in the database, try again later.\nERROR #20");
            catchBlockCode(e);
            return false;
        }

        return userInGroup;
    }

    public static String getGroupChatAdmin(Connection connection, String chatId) { // 🟢
        String query = "SELECT * FROM chat_participants WHERE chat_id = ? AND is_admin = true LIMIT 1";
        String adminId = null;

        try (PreparedStatement pst = connection.prepareStatement(query)) {

            pst.setString(1, chatId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    adminId = rs.getString("user_id");
                }
            }

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not get group admin", "Error in getting group admin from the database, try again later.\nERROR #23");
            catchBlockCode(e);
        }

        return adminId;
    }

    public static boolean updateGroupChatAdmin(String groupId, String oldAdminId, String newAdminId) { // 🟢
        String updateNewAdmin = "UPDATE chat_participants SET is_admin = true WHERE chat_id = ? AND user_id = ?";
        String updateOldAdmin = "UPDATE chat_participants SET is_admin = false WHERE chat_id = ? AND user_id = ?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(updateNewAdmin);
             PreparedStatement pst2 = con.prepareStatement(updateOldAdmin)) {

            pst1.setString(1, groupId);
            pst1.setString(2, newAdminId);
            pst1.executeUpdate();

            pst2.setString(1, groupId);
            pst2.setString(2, oldAdminId);
            pst2.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not group admin", "Error in updating the group admin in the database, try again later.\nERROR #23");
            catchBlockCode(e);
            return false;
        }
        return true;
    }

    public static boolean removeGroupChatParticipant(String groupId, String participantId) { // 🟢
        String removeQuery = "DELETE FROM chat_participants WHERE chat_id = ? AND user_id = ?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(removeQuery)) {

            pst.setString(1, groupId);
            pst.setString(2, participantId);
            pst.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not clean sessions table", "Error in cleaning the sessions table in the database.\nERROR #19");
            catchBlockCode(e);
            return false;
        }
        return true;
    }

    public static boolean deleteGroupChat(String groupId) { // 🟢
        String removeChatQuery = "DELETE FROM chats WHERE chat_id = ?";
        String removeParticipantsQuery = "DELETE FROM chat_participants WHERE chat_id = ?";
        String removeChatTable = "DROP TABLE " + groupId;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(removeChatQuery);
             PreparedStatement pst2 = con.prepareStatement(removeParticipantsQuery);
             PreparedStatement pst3 = con.prepareStatement(removeChatTable)) {

            pst1.setString(1, groupId);
            pst1.executeUpdate();

            pst2.setString(1, groupId);
            pst2.executeUpdate();

            pst3.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not delete group chat", "Error in delete group chat in the database.\nERROR #19");
            catchBlockCode(e);
            return false;
        }
        return true;
    }


    public static boolean addGroupChatParticipant(String groupId, String userId) {
        String query = "INSERT INTO chat_participants(chat_id, user_id, is_admin) VALUES(?,?,?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            pst.setString(1, groupId);
            pst.setString(2, userId);
            pst.setBoolean(3, false);
            pst.executeUpdate();

        } catch (SQLException e) {
            showAlertWithMessage(Alert.AlertType.ERROR, "Could not add user to group chat", "Error in add a user to group chat in the database.\nERROR #19");
            catchBlockCode(e);
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ //


    // Helper Functions

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
