package org.example.OOD.Models;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

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

    @FXML
    public static void ReadArticleAction(Article article) {
        // Get the article URL
        String articleUrl = article.getUrl(); // Replace with the correct method to get the article URL

        if (articleUrl != null && !articleUrl.isEmpty()) {
            try {
                // Load the FXML file
                FXMLLoader loader = new FXMLLoader(User.class.getResource("/Design_Files/article_display.fxml"));

                // Create a new scene from the FXML
                Parent root = loader.load();

                // Find the WebView node directly from the FXML root
                WebView webView = (WebView) root.lookup("#webView");

                if (webView != null) {
                    // Load the article URL into the WebView
                    webView.getEngine().load(articleUrl);
                } else {
                    System.out.println("WebView node not found in FXML.");
                }

                // Set up the stage and display it
                Stage stage = new Stage();
                stage.setTitle("Read org.example.OOD.Article");
                stage.setScene(new Scene(root));
                stage.show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Invalid article URL.");
        }
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
