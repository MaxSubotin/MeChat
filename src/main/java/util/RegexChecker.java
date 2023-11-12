package util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexChecker {

    // Function to check if a string is a valid username
    public static boolean isValidUsername(String username) {
        // Define the regex pattern for a valid username
        String usernamePattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{3,12}$";

        // Use Pattern and Matcher to check the input string against the regex
        Pattern pattern = Pattern.compile(usernamePattern);
        Matcher matcher = pattern.matcher(username);

        return matcher.matches() && !containsSpecialCharacters(username);
    }

    // Function to check if a string is a valid password
    public static boolean isValidPassword(String password) {
        // Define the regex pattern for a valid password
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{3,12}$";

        // Use Pattern and Matcher to check the input string against the regex
        Pattern pattern = Pattern.compile(passwordPattern);
        Matcher matcher = pattern.matcher(password);

        return matcher.matches();
    }

    // Helper function to check if a string contains special characters
    private static boolean containsSpecialCharacters(String input) {
        String specialChars = ".,:;\"'\\/!?@#";
        for (char c : input.toCharArray()) {
            if (specialChars.contains(String.valueOf(c))) {
                return true;
            }
        }
        return false;
    }

//    public static void main(String[] args) {
//        // Test the username and password validation functions
//        String validUsername = "Mm1";
//        String invalidUsername = "mM1!";
//        String validPassword = "Mm1!";
//        String invalidPassword = "#@!";
//
//        System.out.println("Valid Username: " + isValidUsername(validUsername)); // true
//        System.out.println("Invalid Username: " + isValidUsername(invalidUsername)); // false
//        System.out.println("Valid Password: " + isValidPassword(validPassword)); // true
//        System.out.println("Invalid Password: " + isValidPassword(invalidPassword)); // false
//    }

}
