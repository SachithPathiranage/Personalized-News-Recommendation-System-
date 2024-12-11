package org.example.OOD.Models;

import javafx.scene.control.Alert;
import org.example.OOD.Database_Handler.DatabaseHandler;

import java.sql.SQLException;
import java.util.UUID;
import java.util.regex.Pattern;

public class User {
    private String id;
    private String unique_code;
    private String name;
    private String email;
    private String password;
    private UserPreferences preferences;
    static User currentUser; // Static field to store the logged-in user

    public User(String id, String unique_code, String name, String email, String password) {
        this.id = id;
        this.unique_code = unique_code;
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

    public User(int id, String name, String email) {
        this.id = String.valueOf(id);
        this.name = name;
        this.email = email;
        this.preferences = new UserPreferences();
    }

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
                User newUser = new User(userId, uniqueCode, name, email, password);

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
            Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid email format!");
            alert.showAndWait();
            return false;
        }

        if (DatabaseHandler.isEmailRegistered(email)) {
            System.out.println("Email is already registered. Please use a different email.");
            Alert alert = new Alert(Alert.AlertType.ERROR, "Email is already registered. Please use a different email.");
            alert.showAndWait();
            return false;
        }

        if (!isValidPassword(password)) {
            System.out.println("Password must be at least 8 characters long.");
            Alert alert = new Alert(Alert.AlertType.ERROR, "Password must be at least 8 characters long.");
            alert.showAndWait();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            System.out.println("Passwords do not match.");
            Alert alert = new Alert(Alert.AlertType.ERROR, "Passwords do not match.");
            alert.showAndWait();
            return false;
        }
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


    // Methods to interact with articles
    public void likeArticle(Article article) throws SQLException {
        User currentUser = User.getCurrentUser();
        if (currentUser != null && !currentUser.getPreferences().hasArticle(article)) {
            currentUser.getPreferences().addLikedArticle(article, currentUser.getId());
        } else {
            System.out.println("This article is already in your preferences.");
        }
    }

    public void dislikeArticle(Article article) throws SQLException {
        User currentUser = User.getCurrentUser();
        if (currentUser != null && !currentUser.getPreferences().hasArticle(article)) {
            currentUser.getPreferences().addDislikedArticle(article, currentUser.getId());
        }
        else {
            System.out.println("This article is already in your preferences.");
        }
    }
}
