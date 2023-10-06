package app.mechat;

import java.security.SecureRandom;
import java.math.BigInteger;

public class UniqueUserIdGenerator {
    private static final int ID_LENGTH = 10; // Length of the user ID
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
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
        for (int i = 0; i < length; i++) {
            int randomIndex = RANDOM.nextInt(CHARACTERS.length());
            randomString.append(CHARACTERS.charAt(randomIndex));
        }
        return randomString.toString();
    }
}
