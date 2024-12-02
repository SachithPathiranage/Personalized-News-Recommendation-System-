package org.example.OOD.Models;

import java.sql.SQLException;

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
    public User(String id, String email, String name) {
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
    // Static methods to manage the current user
    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
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
