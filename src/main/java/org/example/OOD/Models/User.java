package org.example.OOD.Models;

import javafx.scene.control.Alert;
import org.example.OOD.Database_Handler.DatabaseHandler;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

import static org.example.OOD.Configurations.Alerts.showAlert;

public class User {
    private String id;
    private String unique_code;
    private String name;
    private String email;
    private String password;
    private UserPreferences preferences;
    public static User currentUser; // Static field to store the logged-in user

    public User(String id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.preferences = new UserPreferences();
    }
    public User(String id, String name, String email) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.preferences = new UserPreferences();
    }

    public User() {}


    // Getters and setters for each field
    public String getUnique_code() {
        return unique_code;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public UserPreferences getPreferences() {
        return preferences;
    }
    public void setPreferences(UserPreferences preferences) {
        this.preferences = preferences;
    }

    // Static methods to manage the current user
    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User handleLogin(String email, String password) throws SQLException {
        User currentUser = DatabaseHandler.getInstance().authenticateUserAndRetrieveDetails(email, password, "SELECT id, name FROM users WHERE email = ? AND password = ?");

        if (currentUser != null) {
            User.setCurrentUser(new User(currentUser.getId(), email, currentUser.getName())); // Set the user's email and name
        }
        return currentUser;
    }

    public static boolean handleSignUp(String name, String email, String password, String confirmPassword) {
        try {
            if (validateSignup(email, password, confirmPassword)) {
                // Generate unique ID and code for the user
                String uniqueCode = UUID.randomUUID().toString();
                String userId = DatabaseHandler.generateUserId();

                // Create new user object
                User newUser = new User(userId, name, email, password);

                // Save user to database
                DatabaseHandler.saveUserToDatabase(newUser);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean validateSignup(String email, String password, String confirmPassword) {
        if (!isValidEmail(email)) {
            System.out.println("Invalid email format.");
            showAlert(Alert.AlertType.ERROR, "Invalid email format!", "Please enter a valid email address.");
            return false;
        }

        if (DatabaseHandler.isEmailRegistered(email)) {
            System.out.println("Email is already registered. Please use a different email.");
            showAlert(Alert.AlertType.ERROR, "Email is already registered!", "Please use a different email.");
            return false;
        }

        if (!isValidPassword(password)) {
            System.out.println("Password must be at least 8 characters long.");
            showAlert(Alert.AlertType.ERROR, "Password must be at least 8 characters long!", "Please enter a password with at least 8 characters.");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            System.out.println("Passwords do not match.");
            showAlert(Alert.AlertType.ERROR, "Passwords do not match!", "Please enter the same password in both fields.");
            return false;
        }
        System.out.println("Sign-up successful!");
        showAlert(Alert.AlertType.INFORMATION, "Sign-up successful!", "Please log in.");
        return true;
    }

    public static boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    public static void initializeUserPreferences() {
        try {
            // Fetch preferences as a map
            Map<String, List<Integer>> preferencesMap = DatabaseHandler.getInstance().fetchAllUserPreferences(User.getCurrentUser().getId());

            // Convert map of article IDs to a map of Article objects
            UserPreferences preferences = new UserPreferences();

            // Populate liked articles
            List<Article> likedArticles = preferencesMap.getOrDefault("liked", new ArrayList<>())
                    .stream()
                    .map(id -> fetchArticleSafely(id))
                    .filter(Objects::nonNull) // Remove nulls if fetch fails
                    .toList();
            preferences.setLikedArticles(likedArticles);

            // Populate disliked articles
            List<Article> dislikedArticles = preferencesMap.getOrDefault("disliked", new ArrayList<>())
                    .stream()
                    .map(id -> fetchArticleSafely(id))
                    .filter(Objects::nonNull)
                    .toList();
            preferences.setDislikedArticles(dislikedArticles);

            // Populate read articles
            List<Article> readArticles = preferencesMap.getOrDefault("read", new ArrayList<>())
                    .stream()
                    .map(id -> fetchArticleSafely(id))
                    .filter(Objects::nonNull)
                    .toList();
            preferences.setReadArticles(readArticles);

            // Set the preferences in the current user
            User.getCurrentUser().setPreferences(preferences);

            // Log the updated preferences
            //System.out.println("Updated User Preferences: " + preferences);

        } catch (SQLException e) {
            e.printStackTrace();
            // Handle or log the error for failing to initialize preferences
        }
    }

    private static Article fetchArticleSafely(int articleId) {
        try {
            return DatabaseHandler.getInstance().fetchArticleById(articleId);
        } catch (SQLException e) {
            e.printStackTrace(); // Log the error
            return null; // Return null to signify fetch failure
        }
    }
}
