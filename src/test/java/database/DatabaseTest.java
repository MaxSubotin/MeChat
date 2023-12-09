package database;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import util.Message;
import util.RegularChat;
import util.User;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;


class DatabaseTest {

    // 🔷 Real user from the database:
    private final String REAL_USERNAME = "Max";
    private final String REAL_PASSWORD = "123";
    private final String REAL_TABLE_NAME = "nwpwiudn7b_vxn89pkhf3_33";
    private final User REAL_USER = new User("Max","NWPwiUdn7B","male");

    // 🔶 Fake user, not in the database:
    private final String FAKE_USERNAME = "MaxMax";
    private final String FAKE_PASSWORD = "MaxMax";
    private final String FAKE_TABLE_NAME = "abc123_def456_111";
    private final User FAKE_USER = new User("MaxMax","abc123","male");


    // 🔴 Some GET unit tests: 🔴

    @DisplayName("Test getUserFromDatabase #1:")
    @Test
    public void getUserFromDatabaseTest1() {
        // username and password are correct
        char[] password = REAL_PASSWORD.toCharArray();

        User resultUser = Database.getUserFromDatabase(REAL_USERNAME, password);
        Arrays.fill(password, '\0'); // Zeroing out the password in memory for user safety

        assertNotNull(resultUser, "Expected user was not found");
        assertEquals(REAL_USER.getName(), resultUser.getName(),"Expected username does not match");
        assertEquals(REAL_USER.getId(), resultUser.getId(), "Expected user id does not match");
        assertEquals(REAL_USER.getUserImageWithoutSuffix(), resultUser.getUserImageWithoutSuffix(), "Expected user image does not match");
    }

    @DisplayName("Test getUserFromDatabase #2:")
    @Test
    public void getUserFromDatabaseTest2() {
        // username incorrect, password correct
        String username = FAKE_USERNAME + "!@#";
        char[] password = REAL_PASSWORD.toCharArray();

        User resultUser = Database.getUserFromDatabase(username, password);
        Arrays.fill(password, '\0'); // Zeroing out the password in memory for user safety

        assertNull(resultUser);
    }

    @DisplayName("Test getUsersChatsFromDatabase #1:")
    @Test
    public void getUsersChatsFromDatabaseTest1() {
        // userId correct
        ArrayList<RegularChat> userChats = Database.getUsersChatsFromDatabase(REAL_USER.getId());

        assertNotNull(userChats, "Expected user chats were not found");
        assertFalse(userChats.isEmpty(), "Expected to have chats, instead found no chats");
    }

    @DisplayName("Test getUsersChatsFromDatabase #2:")
    @Test
    public void getUsersChatsFromDatabaseTest2() {
        // userId incorrect
        ArrayList<RegularChat> userChats = Database.getUsersChatsFromDatabase(FAKE_USER.getId());

        assertNotNull(userChats, "Expected user chats were not found");
        assertTrue(userChats.isEmpty(), "Expected to have no chats, instead found some chats");
    }

    @DisplayName("Test getChatMessagesFromDatabase #1:")
    @Test
    public void getChatMessagesFromDatabaseTest1() {
        // table name is correct
        ArrayList<Message> messages = Database.getChatMessagesFromDatabase(REAL_TABLE_NAME);

        assertNotNull(messages, "Expected chat messages were not found");
        assertFalse(messages.isEmpty(), "Expected to have messages, instead found no messages");
    }

    @DisplayName("Test getUserIdByUsername #1:")
    @Test
    public void getUserIdByUsernameTest1() {
        // username is correct
        String userId = Database.getUserIdByUsername(REAL_USERNAME);

        assertNotNull(userId, "Expected userId was not found");
        assertEquals(REAL_USER.getId(), userId, "Expected to have messages, instead found no messages");
    }

    @DisplayName("Test getUserIdByUsername #2:")
    @Test
    public void getUserIdByUsernameTest2() {
        // username is incorrect
        String userId = Database.getUserIdByUsername(FAKE_USERNAME);

        assertNull(userId, "Expected username is actually correct and was found");
    }


    // 🟢 Some ADD unit tests: 🟢

    @DisplayName("Test addUserToDatabase #1:")
    @Test
    public void addUserToDatabaseTest1() { // adding a user that does not exist
        // new user info
        String usernameAndPassword = FAKE_USERNAME; // username and password are the same
        String hashedPassword = BCrypt.withDefaults().hashToString(12, usernameAndPassword.toCharArray());
        User user = FAKE_USER;

        assertTrue(
                Database.addUserToDatabase(
                        usernameAndPassword,
                        hashedPassword,
                        user.getId(),
                        user.getUserImageWithoutSuffix()
                ) , "Could not add the user to the database"
        );

        Database.deleteUser(user.getId());
    }

    @DisplayName("Test addConversationToDatabase #1:")
    @Test
    public void addConversationToDatabaseTest1() {
        String senderId = "abc123", receiverId = "def456";

        int conversation_id = Database.addConversationToDatabase(senderId,receiverId);
        assertNotEquals(-1,conversation_id, "Unexpected conversation id, add conversation was unsuccessful");

        RegularChat chat = new RegularChat(null, senderId, receiverId, conversation_id);
        Database.deleteRegularChat(chat);
    }


    // 🟠 Some boolean function unit tests: 🟠

    @DisplayName("Test isUsernameUnique #1:")
    @Test
    public void isUsernameUniqueTest1() {
        String existingUsername = "Max";
        assertFalse(Database.isUsernameUnique(existingUsername), "Expected the username to be unique");
    }

    @DisplayName("Test isUsernameUnique #2:")
    @Test
    public void isUsernameUniqueTest2() {
        String nonExistingUsername = "NewMax";
        assertTrue(Database.isUsernameUnique(nonExistingUsername), "Expected the username to be unique");
    }

    @DisplayName("Test update Username/Password/Image:")
    @Test
    public void updateUserInfoTest() {
        Database.addUserToDatabase(
                FAKE_USERNAME,
                FAKE_PASSWORD,
                FAKE_USER.getId(),
                FAKE_USER.getUserImageWithoutSuffix()
        );

        assertTrue(Database.updateUsernameInDatabase(FAKE_USERNAME+"123", FAKE_USERNAME));
        assertTrue(Database.updatePasswordInDatabase(FAKE_PASSWORD+"123", FAKE_PASSWORD));
        assertTrue(Database.updateAvatarInDatabase("female", FAKE_USER.getUserImageWithoutSuffix()));

        Database.deleteUser(FAKE_USER.getId());
    }

}












