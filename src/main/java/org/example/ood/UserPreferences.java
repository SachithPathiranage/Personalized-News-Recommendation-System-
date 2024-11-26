package org.example.ood;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserPreferences {
    private List<Article> likedArticles;
    private List<Article> dislikedArticles;
    private List<Article> readArticles;

    // Constructor
    public UserPreferences() {
        this.likedArticles = new ArrayList<>();
        this.dislikedArticles = new ArrayList<>();
        this.readArticles = new ArrayList<>();
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
            // Remove from disliked if present
            if (dislikedArticles.remove(article)) {
                DatabaseHandler.getInstance().removeUserPreference(userId, article.getId(), "disliked");
            }
            if (!likedArticles.contains(article)) {
                likedArticles.add(article);
                if (DatabaseHandler.getInstance().isPreferenceRecorded(userId, article.getId(), "liked")) {
                    System.out.println("Preference already recorded: " + article.getTitle() + " for user: " + userId);
                } else {
                    DatabaseHandler.getInstance().saveUserPreference(userId, article.getId(), "liked");
                    System.out.println("Added to liked articles: " + article.getTitle());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addDislikedArticle(Article article, String userId) {
        try {
            // Remove from liked if present
            if (likedArticles.remove(article)) {
                DatabaseHandler.getInstance().removeUserPreference(userId, article.getId(), "liked");
            }
            if (!dislikedArticles.contains(article)) {
                dislikedArticles.add(article);
                if (DatabaseHandler.getInstance().isPreferenceRecorded(userId, article.getId(), "disliked")) {
                    System.out.println("Preference already recorded: " + article.getTitle() + " for user: " + userId);
                } else {
                    DatabaseHandler.getInstance().saveUserPreference(userId, article.getId(), "disliked");
                    System.out.println("Added to disliked articles: " + article.getTitle());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeLikedArticle(Article article, String userId) {
        try {
            if (likedArticles.remove(article)) {
                DatabaseHandler.getInstance().removeUserPreference(userId, article.getId(), "liked");
                System.out.println("Removed from liked articles: " + article.getTitle());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeDislikedArticle(Article article, String userId) {
        try {
            if (dislikedArticles.remove(article)) {
                DatabaseHandler.getInstance().removeUserPreference(userId, article.getId(), "disliked");
                System.out.println("Removed from disliked articles: " + article.getTitle());
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
                if (DatabaseHandler.getInstance().isPreferenceRecorded(userId, article.getId(), "read")) {
                    System.out.println("Already read the article: " + article.getTitle() + " by user: " + userId);
                } else {
                    DatabaseHandler.getInstance().saveUserPreference(userId, article.getId(), "read");
                    System.out.println("Added to read articles: " + article.getTitle());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isRead(Article article) {
        return readArticles.contains(article);
    }

}
