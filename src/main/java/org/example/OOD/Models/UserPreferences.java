package org.example.OOD.Models;

import org.example.OOD.Database_Handler.DatabaseHandler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserPreferences {
    private List<Article> likedArticles;
    private List<Article> dislikedArticles;
    private List<Article> readArticles;
    private DatabaseHandler dbHandler;

    // Constructor
    public UserPreferences() {
        this.likedArticles = new ArrayList<>();
        this.dislikedArticles = new ArrayList<>();
        this.readArticles = new ArrayList<>();
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

    // Methods to update preferences
    public void addLikedArticle(Article article, String userId) {
        try {
            // Add to liked if not already liked
            if (!likedArticles.contains(article)) {
                likedArticles.add(article);
                if (dbHandler.isPreferenceRecorded(userId, article.getId(), "liked")) {
                    System.out.println("Preference already recorded: " + article.getTitle() + " for user: " + userId);
                } else {
                    dbHandler.saveUserPreference(userId, article.getId(), "liked");
                    System.out.println("Added to liked articles: " + article.getTitle());
                    dbHandler.incrementMetric(article.getId(), "likes");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addDislikedArticle(Article article, String userId) {
        try {
            // Add to disliked if not already disliked
            if (!dislikedArticles.contains(article)) {
                dislikedArticles.add(article);
                if (dbHandler.isPreferenceRecorded(userId, article.getId(), "disliked")) {
                    System.out.println("Preference already recorded: " + article.getTitle() + " for user: " + userId);
                } else {
                    dbHandler.saveUserPreference(userId, article.getId(), "disliked");
                    System.out.println("Added to disliked articles: " + article.getTitle());
                    dbHandler.incrementMetric(article.getId(), "dislikes");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeLikedArticle(Article article, String userId) {
        try {
            if (likedArticles.remove(article)) {
                dbHandler.removeUserPreference(userId, article.getId(), "liked");
                System.out.println("Removed from liked articles: " + article.getTitle());
                dbHandler.decrementMetric(article.getId(), "likes");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeDislikedArticle(Article article, String userId) {
        try {
            if (dislikedArticles.remove(article)) {
                dbHandler.removeUserPreference(userId, article.getId(), "disliked");
                System.out.println("Removed from disliked articles: " + article.getTitle());
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
                "likedArticles=" + likedArticles.stream().map(Article::getTitle).toList() +
                ", dislikedArticles=" + dislikedArticles +
                ", readArticles=" + readArticles +
                '}';
    }

//    public String getUserPreferences(Article article, String userId) {
//        try {
//            // Fetch preferences from the database
//            Map<String, List<Integer>> preferencesMap = dbHandler.fetchAllUserPreferences(userId);
//
//            // Count likes, dislikes, and reads for the current article
//            int likesCount = preferencesMap.get("liked").contains(article.getId()) ? 1 : 0;
//            int dislikesCount = preferencesMap.get("disliked").contains(article.getId()) ? 1 : 0;
//            int readsCount = preferencesMap.get("read").contains(article.getId()) ? 1 : 0;
//
//            // Format the string: "Likes: X, Dislikes: Y, Reads: Z"
//            return String.format("Likes: %d, Dislikes: %d, Reads: %d", likesCount, dislikesCount, readsCount);
//        } catch (SQLException e) {
//            e.printStackTrace();
//            return "Error loading preferences";
//        }
//    }
}
