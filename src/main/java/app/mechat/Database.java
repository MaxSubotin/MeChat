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
                    userFromDatabase = new User(rs.getString("username"));
            }
        }
        catch (SQLException e) {
            catchBlockCode(e);
        }

        return userFromDatabase;
    }

    public static String getUsernameBySession(String session) {
        String queryForUser = "SELECT * FROM session_tokens WHERE session_token = ?";
        String Username = "";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(queryForUser)) {

            // Find the user in the database and create a User object
            pst1.setString(1, session);
            ResultSet rs = pst1.executeQuery();

            if (rs.next()) {
                Username = rs.getString("username");
            }
        }
        catch (SQLException e) {
            catchBlockCode(e);
        }

        return Username;
    }

    public static ArrayList<Chat> getUsersChatsFromDatabase(String userUsername) {
        String queryForConversations = "SELECT * FROM conversations WHERE participant1 = ? OR participant2 = ?";
        ArrayList<Chat> usersChats = new ArrayList<Chat>();

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(queryForConversations)) {

            // Find all the conversations this user has in the database and for each one we will create a chat box
            pst1.setString(1, userUsername);
            pst1.setString(2, userUsername);
            ResultSet rs = pst1.executeQuery();

            while (rs.next()) {
                if (Objects.equals(rs.getString("participant1"), userUsername))
                    usersChats.add(new Chat(
                            null,
                            userUsername,
                            rs.getString("participant2"),
                            rs.getInt("conversation_id"))
                    );
                else
                    usersChats.add(new Chat(
                            null,
                            userUsername,
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
                chatMessages.add(new Message(rs.getString("message_text"), rs.getString("sender_username"), rs.getString("receiver_username"),rs.getString("timestamp"), tableName.split("_")[2]));
            }

        } catch (SQLException e) {
            catchBlockCode(e);
        }

        return chatMessages;
    }


    // ------------------------------------------------------------------ //

    public static void addUserToDatabase(String userUsername, String userPassword) {
        String insertQueryUsers = "INSERT INTO users(username, password) VALUES(?, ?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement insertPstUsers = con.prepareStatement(insertQueryUsers)) {

            insertPstUsers.setString(1, userUsername);
            insertPstUsers.setString(2, userPassword);
            insertPstUsers.executeUpdate();

        } catch (SQLException e) {
            catchBlockCode(e);
        }
    }

    public static void addSessionToDataBase(String userUsername, String session) {
        String insertQuery = "INSERT INTO session_tokens (session_token, username) VALUES (?, ?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement insertPstUsers = con.prepareStatement(insertQuery)) {

            insertPstUsers.setString(1, session);
            insertPstUsers.setString(2, userUsername);
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

    public static void addMessageToDatabase(Message message, String receiver, int conversation_id) {
        String tableName = compareStrings(message.getSender(), receiver);
        String insertQuery = "INSERT INTO " + tableName + "_" + conversation_id + " (sender_username, receiver_username, timestamp, message_text) VALUES (?, ?, ?, ?)";

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
        String query = "CREATE TABLE IF NOT EXISTS " + tableName + " (message_id SERIAL PRIMARY KEY, sender_username VARCHAR(255) NOT NULL, receiver_username VARCHAR(255) NOT NULL, timestamp TIMESTAMP NOT NULL, message_text TEXT NOT NULL)";

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
                    usernameUnique = false;
                }
            }

        } catch (SQLException e) {
            catchBlockCode(e);
        }

        return usernameUnique;
    }

    public static boolean isUserConnected(String username) {
        String findUserQuery = "SELECT * FROM session_tokens WHERE username = ?";
        boolean connected = false;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(findUserQuery)) {

            // Find the user in the database and create a User object
            pst1.setString(1, username);
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

    public static void removeUserSession(String userUsername) {
        String removeQuery = "DELETE FROM session_tokens WHERE username = ?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(removeQuery)) {

            pst.setString(1, userUsername);
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


//    public static void saveCurrentGame(Player currentPlayer, int[][] currentGame, int[][] gameSolution, String difficulty, String timer, int mistakes) {
//        String saveQuery = "INSERT INTO users_games(username, date, game, solution, difficulty, timer, mistakes) VALUES(?, ?, ?::json, ?::json, ?, ?, ?)";
//        String updateQuery = "UPDATE users_info SET saved_games = ? WHERE username = ?";
//
//        try (Connection con = DatabaseConfig.getConnection();
//             PreparedStatement pstSave = con.prepareStatement(saveQuery);
//             PreparedStatement pstUpdate = con.prepareStatement(updateQuery)) {
//
//            // Get the current date
//            LocalDate currentDate = LocalDate.now();
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
//            String formattedDate = currentDate.format(formatter);
//
//            pstSave.setString(1, currentPlayer.getUsername());
//            pstSave.setString(2, formattedDate + "-" + LoginController.currentPlayer.getSavedGamesCounter());
//            pstSave.setString(3, new Gson().toJson(currentGame));
//            pstSave.setString(4, new Gson().toJson(gameSolution));
//            pstSave.setString(5, difficulty);
//            pstSave.setString(6, timer);
//            pstSave.setInt(7, mistakes);
//            pstSave.executeUpdate();
//
//            LoginController.currentPlayer.setSavedGamesCounter(LoginController.currentPlayer.getSavedGamesCounter() + 1);
//
//            pstUpdate.setInt(1, currentPlayer.getSavedGamesCounter());
//            pstUpdate.setString(2, currentPlayer.getUsername());
//            pstUpdate.executeUpdate();
//
//        } catch (SQLException e) {
//            catchBlockCode(e);
//        }
//    }
//
//    public static ArrayList<String> getAllUserGameDates(String userUsername) {
//        String query = "SELECT * FROM users_games WHERE username = ?";
//        ArrayList<String> dates = new ArrayList<String>();
//
//        try (Connection con = DatabaseConfig.getConnection();
//             PreparedStatement pst = con.prepareStatement(query)) {
//
//            pst.setString(1,userUsername);
//            ResultSet rs = pst.executeQuery();
//
//            while(rs.next()) {
//                dates.add(rs.getString("date"));
//            }
//
//        } catch (SQLException e) {
//            catchBlockCode(e);
//        }
//
//        return dates;
//    }
//
//    public static SudokuGameData getGameByUsernameAndDate(String userUsername, String gameDate) {
//        String query = "SELECT * FROM users_games WHERE username = ? AND date = ?";
//        SudokuGameData requestedGame = null;
//
//        try (Connection con = DatabaseConfig.getConnection();
//             PreparedStatement pst = con.prepareStatement(query)) {
//
//            pst.setString(1, userUsername);
//            pst.setString(2, gameDate);
//            ResultSet rs = pst.executeQuery();
//
//            if (rs.next()) {
//                requestedGame = turnJsonIntoArray(rs);
//            }
//
//        } catch (SQLException e) {
//            catchBlockCode(e);
//        }
//
//        return requestedGame;
//    }
//
//    public static SudokuGameData getLastGame(String userUsername) {
//        String query = "SELECT * FROM users_games WHERE username = ? ORDER BY date DESC LIMIT 1";
//        SudokuGameData requestedGame = null;
//
//        try (Connection con = DatabaseConfig.getConnection();
//             PreparedStatement pst = con.prepareStatement(query)) {
//
//            pst.setString(1, userUsername);
//            ResultSet rs = pst.executeQuery();
//
//            if (rs.next()) {
//                requestedGame = turnJsonIntoArray(rs);
//            }
//
//        } catch (SQLException e) {
//            catchBlockCode(e);
//        }
//        return requestedGame;
//    }
//
//    public static void setUserMistakesCounter(String userUsername, int count) {
//        String query = "UPDATE users_info SET mistakes = ? WHERE username = ?";
//
//        try (Connection con = DatabaseConfig.getConnection();
//             PreparedStatement pst = con.prepareStatement(query)) {
//
//            pst.setInt(1, count);
//            pst.setString(2, userUsername);
//            pst.executeUpdate();
//
//        } catch (SQLException e) {
//            catchBlockCode(e);
//        }
//    }
//
//    public static void setUserSolvedCounter(String userUsername, int count) {
//        String query = "UPDATE users_info SET solved = ? WHERE username = ?";
//
//        try (Connection con = DatabaseConfig.getConnection();
//             PreparedStatement pst = con.prepareStatement(query)) {
//
//            pst.setInt(1, count);
//            pst.setString(2, userUsername);
//            pst.executeUpdate();
//
//        } catch (SQLException e) {
//            catchBlockCode(e);
//        }
//    }
//
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
