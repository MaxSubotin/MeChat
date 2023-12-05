package util;

import database.Database;

import java.security.SecureRandom;

public class IdGenerator {
    private static final int ID_LENGTH = 10; // Length of the user ID
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateUniqueUserId() {
        String userId;
        do {
            userId = generateRandomString(ID_LENGTH);
        } while (!Database.isUserIdUnique(userId)); // Check in the database that the user id is unique
        return userId;
    }

    private static String generateRandomString(int length) {
        StringBuilder randomString = new StringBuilder(length);

        // Ensure the first character is a letter
        int randomIndex = RANDOM.nextInt(CHARACTERS.length() - 10); // Exclude the last 10 characters (digits)
        randomString.append(CHARACTERS.charAt(randomIndex));

        // Generate the remaining characters
        for (int i = 1; i < length; i++) {
            randomIndex = RANDOM.nextInt(CHARACTERS.length());
            randomString.append(CHARACTERS.charAt(randomIndex));
        }
        return randomString.toString();
    }
}
