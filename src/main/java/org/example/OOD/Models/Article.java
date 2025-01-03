package org.example.OOD.Models;

import org.example.OOD.Database_Handler.DatabaseHandler;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.example.OOD.Models.User.currentUser;

public class Article {
    private int id;
    private String title;
    private String description;
    private String url;
    private String source_name;
    private String author;
    private String imageUrl;
    private String publishedDate;
    private int likes;
    private int dislikes;
    private int readers;
    private String category;
    private String content;

    // Adding similarityScore field
    private double similarityScore;


    // Constructors
    public Article(int id, String title, String description, String content, String category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.content = content;
        this.category = category;
    }

    public Article(int id, String title, String description, String newsUrl, String source, String author, String imageUrl, String date, String category, String content, int likes, int dislikes, int readers) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.url = newsUrl;
        this.source_name = source;
        this.author = author;
        this.imageUrl = imageUrl;
        this.publishedDate = date;
        this.category = category;
        this.content = content;
        this.likes = likes;
        this.dislikes = dislikes;
        this.readers = readers;
    }


    // Getters and setters
    public int getId() { return id; }

    public String getTitle() { return title; }

    public String getDescription() { return description; }

    public String getSource_name() {
        return source_name;
    }

    public String getUrl() {
        return url;
    }

    public String getAuthor() { return author; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getPublishedDate() { return publishedDate; }

    public String getContent() {
        return content;
    }

    public String getCategory() {
        return category;
    }

    public int getLikes() { return likes; }

    public int getDislikes() { return dislikes; }

    public int getReaders() { return readers; }

    // Getter and Setter for similarityScore
    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    // Method to get textual content of the article (for similarity computation)
    public String getText() {
        String baseText = (content != null && !content.isEmpty()) ? content : description;
        return (baseText != null && category != null) ? baseText + " " + category : baseText;
    }

    // Validation methods
    public static boolean isValidURL(String url) {
        String urlRegex = "^(https?://)?(www\\.)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/\\S*)?$";
        return url != null && Pattern.matches(urlRegex, url);
    }

    public static boolean isValidImageURL(String imageUrl) {
        String imageRegex = "^(https?://)?(www\\.)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\S*(\\.jpg|\\.png|\\.jpeg|\\.gif)$";
        return imageUrl != null && Pattern.matches(imageRegex, imageUrl);
    }

    public static boolean isNonEmptyString(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static boolean isValidCategory(String category, String[] validCategories) {
        if (category == null) return false;
        for (String validCategory : validCategories) {
            if (category.equalsIgnoreCase(validCategory)) {
                return true;
            }
        }
        return false;
    }


    // Update preferences for an article
    public static void updateUserPreferences(Map<String, List<Integer>> preferences, User user, Article article) {
        if (preferences.get("liked").contains(article.getId())) {
            user.getPreferences().addLikedArticle(article, user.getId());
        }
        if (preferences.get("disliked").contains(article.getId())) {
            user.getPreferences().addDislikedArticle(article, user.getId());
        }
        if (preferences.get("read").contains(article.getId())) {
            user.getPreferences().addReadArticle(article, user.getId());
        }
    }

    public static Article findArticleByTitle(String title) {
        // Search in liked articles
        for (Article article : currentUser.getPreferences().getLikedArticles()) {
            if (article.getTitle().equals(title)) {
                return article;
            }
        }

        // Search in disliked articles
        for (Article article : currentUser.getPreferences().getDislikedArticles()) {
            if (article.getTitle().equals(title)) {
                return article;
            }
        }

        // Search in read articles
        for (Article article : currentUser.getPreferences().getReadArticles()) {
            if (article.getTitle().equals(title)) {
                return article;
            }
        }
        // If not found in any list, return null
        return null;
    }

    @Override
    public String toString() {
        return "Article{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Article article = (Article) obj;
        return id == article.id; // Compare based on ID
    }

    @Override
    public int hashCode() {
        return Objects.hash(id); // Use ID for hash code
    }

}

