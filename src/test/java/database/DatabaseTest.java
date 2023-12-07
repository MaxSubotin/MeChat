package database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.User;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;


class DatabaseTest {
    //@DisplayName("Test getUserFromDatabase #1:")
    @Test
    public void getUserFromDatabaseTest1() {

        // Set up test data or configure the test database with known data
        String username = "Max";
        char[] password = "123".toCharArray();

        // Execute the method under test
        User resultUser = Database.getUserFromDatabase(username, password);
        Arrays.fill(password, '\0'); // Zeroing out the password in memory for user safety

        // Verify the result using assertions
        assertNotNull(resultUser, "Expected user was not found");
        assertEquals("Max", resultUser.getName(),"Expected username does not match");
        assertEquals("NWPwiUdn7B", resultUser.getId(), "Expected user id does not match");
        assertEquals("male", resultUser.getUserImageWithoutSuffix(), "Expected user image does not match");

        // ... other assertions ...

    }

}