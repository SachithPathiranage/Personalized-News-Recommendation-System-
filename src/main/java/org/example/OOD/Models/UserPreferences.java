package org.example.OOD.Models;

import javafx.scene.control.Alert;
import org.example.OOD.Database_Handler.DatabaseHandler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.example.OOD.Configurations.Alerts.showAlert;

public class UserPreferences {
    private static List<Article> likedArticles = new ArrayList<>();
    private static List<Article> dislikedArticles = new ArrayList<>();
    private List<Article> readArticles = new ArrayList<>();

    private static DatabaseHandler dbHandler;

    // Constructor
    public UserPreferences() {
        try {
            dbHandler = DatabaseHandler.getInstance();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Getters
    public List<Article> getLikedArticles() {
        return likedArticles;
    }

    public List<Article> getDislikedArticles() {
        return dislikedArticles;
    }

    public List<Article> getReadArticles() {
        return readArticles;
    }

    public void setLikedArticles(List<Article> likedArticles) {
        this.likedArticles = new ArrayList<>(likedArticles); // Ensures mutability
    }
    public void setDislikedArticles(List<Article> dislikedArticles) {
        this.dislikedArticles = new ArrayList<>(dislikedArticles); // Ensures mutability
    }
    public void setReadArticles(List<Article> readArticles) {
        this.readArticles = new ArrayList<>(readArticles); // Ensures mutability
    }

    // Methods to update preferences
    public void addLikedArticle(Article article, String userId) {
        try {
            // Remove from dislikedArticles if it exists there
            if (dislikedArticles.contains(article)) {
                removeDislikedArticle(article, userId);
                System.out.println("Removed from disliked articles to like the article.");
            }

            // Add to likedArticles if not already liked
            if (!likedArticles.contains(article)) {
                likedArticles.add(article);
                if (dbHandler.isPreferenceRecorded(userId, article.getId(), "liked")) {
                    System.out.println("Preference already recorded: " + article.getTitle() + " for user: " + userId);
                } else {
                    dbHandler.saveUserPreference(userId, article.getId(), "liked");
                    System.out.println("Added to liked articles: " + article.getTitle());
                    showAlert(Alert.AlertType.INFORMATION, "Liked Article", "Added to liked articles: " + article.getTitle());
                    dbHandler.incrementMetric(article.getId(), "likes");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addDislikedArticle(Article article, String userId) {
        try {
            // Remove from likedArticles if it exists there
            if (likedArticles.contains(article)) {
                removeLikedArticle(article, userId);
                System.out.println("Removed from liked articles to dislike the article.");
            }

            // Add to dislikedArticles if not already disliked
            if (!dislikedArticles.contains(article)) {
                dislikedArticles.add(article);
                if (dbHandler.isPreferenceRecorded(userId, article.getId(), "disliked")) {
                    System.out.println("Preference already recorded: " + article.getTitle() + " for user: " + userId);
                } else {
                    dbHandler.saveUserPreference(userId, article.getId(), "disliked");
                    System.out.println("Added to disliked articles: " + article.getTitle());
                    showAlert(Alert.AlertType.INFORMATION, "Disliked Article", "Added to disliked articles: " + article.getTitle());
                    dbHandler.incrementMetric(article.getId(), "dislikes");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeLikedArticle(Article article, String userId) {
        try {
            if (likedArticles.remove(article)) {
                dbHandler.removeUserPreference(userId, article.getId(), "liked");
                System.out.println("Removed from liked articles: " + article.getTitle());
                showAlert(Alert.AlertType.INFORMATION,"Removed from liked Article","Removed from liked articles: " + article.getTitle());
                dbHandler.decrementMetric(article.getId(), "likes");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeDislikedArticle(Article article, String userId) {
        try {
            if (dislikedArticles.remove(article)) {
                dbHandler.removeUserPreference(userId, article.getId(), "disliked");
                System.out.println("Removed from disliked articles: " + article.getTitle());
                showAlert(Alert.AlertType.INFORMATION,"Removed from disliked Article","Removed from disliked articles: " + article.getTitle());
                dbHandler.decrementMetric(article.getId(), "dislikes");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Check if an article is already liked or disliked
    public boolean isLiked(Article article) {
        return likedArticles.contains(article);
    }

    public boolean isDisliked(Article article) {
        return dislikedArticles.contains(article);
    }

    // Check if an article is already in preferences
    public boolean hasArticle(Article article) {
        return likedArticles.contains(article) || dislikedArticles.contains(article);
    }

    public void addReadArticle(Article article, String userId) {
        try {
            if (!readArticles.contains(article)) {
                readArticles.add(article);
                if (dbHandler.isPreferenceRecorded(userId, article.getId(), "read")) {
                    System.out.println("Already read the article: " + article.getTitle() + " by user: " + userId);
                } else {
                    dbHandler.saveUserPreference(userId, article.getId(), "read");
                    System.out.println("Added to read articles: " + article.getTitle());
                    dbHandler.incrementMetric(article.getId(), "readers");
                    showAlert(Alert.AlertType.INFORMATION,"Read Article","Added to read articles: " + article.getTitle());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void logPreferences(Map<String, List<Integer>> preferences) {
        System.out.println("Liked articles: " + preferences.get("liked"));
        System.out.println("Disliked articles: " + preferences.get("disliked"));
        System.out.println("Read articles: " + preferences.get("read"));
    }

    public boolean isRead(Article article) {
        return readArticles.contains(article);
    }

    @Override
    public String toString() {
        return "UserPreferences{" +
                "likedArticles=" + likedArticles +
                ", dislikedArticles=" + dislikedArticles +
                ", readArticles=" + readArticles +
                '}';
    }
}
