package app.mechat;

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
                if (Objects.equals(rs.getString("password"), userPassword))
                    userFromDatabase = new User(rs.getString("username"), rs.getString("phonenumber"));
            }
        }
        catch (SQLException e) {
            catchBlockCode(e);
        }

        return userFromDatabase;
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

    public static ArrayList<Message> getChatMessagesFromDatabase(int conversation_id) {
        String queryForConversations = "SELECT * FROM messages WHERE conversation_id = ?";
        ArrayList<Message> chatMessages = new ArrayList<>();

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(queryForConversations)) {

            // Find all the conversations this user has in the database and for each one we will create a chat box
            pst1.setInt(1, conversation_id);
            ResultSet rs = pst1.executeQuery();

            while (rs.next()) {
                chatMessages.add(new Message(rs.getString("message_text"), rs.getString("sender_username"), rs.getString("timestamp")));
            }

        } catch (SQLException e) {
            catchBlockCode(e);
        }

        return chatMessages;
    }


    // ------------------------------------------------------------------ //

    public static void addUserToDatabase(String userUsername, String userPassword, String phoneNumber) {
        String insertQueryUsers = "INSERT INTO users(username, password, phonenumber) VALUES(?, ?, ?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement insertPstUsers = con.prepareStatement(insertQueryUsers)) {

            insertPstUsers.setString(1, userUsername);
            insertPstUsers.setString(2, userPassword);
            insertPstUsers.setString(3, phoneNumber);
            insertPstUsers.executeUpdate();

        } catch (SQLException e) {
            catchBlockCode(e);
        }
    }

    public static void addConversationToDatabase(String sender, String receiver) {
        String insertQuery = "INSERT INTO conversations (participant1, participant2) VALUES (?, ?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(insertQuery)) {

            // Find all the conversations this user has in the database and for each one we will create a chat box
            pst1.setString(1, sender);
            pst1.setString(2, receiver);
            pst1.executeUpdate();

        } catch (SQLException e) {
            catchBlockCode(e);
        }
    }

    public static void addMessageToDatabase(Message message, int conversation_id) {
        String insertQuery = "INSERT INTO messages (conversation_id, sender_username, timestamp, message_text) VALUES (?, ?, ?, ?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst1 = con.prepareStatement(insertQuery)) {

            Timestamp timestamp = Timestamp.valueOf(message.getTimestamp());

            // Find all the conversations this user has in the database and for each one we will create a chat box
            pst1.setInt(1, conversation_id);
            pst1.setString(2, message.getSender());
            pst1.setTimestamp(3, timestamp);
            pst1.setString(4, message.getText());
            pst1.executeUpdate();

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

    public static boolean isPhoneNumberUnique(String userPhoneNumber) {
        String findUserQuery = "SELECT COUNT(*) as count FROM users WHERE phonenumber = ?";
        boolean usernameUnique = true;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement pst = con.prepareStatement(findUserQuery)) {

            pst.setString(1, userPhoneNumber);
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




//    // ------------------------------------------------------------------ //
//
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
