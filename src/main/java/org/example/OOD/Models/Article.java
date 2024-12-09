package org.example.OOD.Models;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

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


    // Constructor
    public Article(int id, String title, String description, String sourceName, String publishedDate, String imageUrl, String category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.source_name = sourceName;
        this.publishedDate = publishedDate;
        this.imageUrl = imageUrl;
    }

    public Article(int id, String title,String description , String url) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.url = url;
    }

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
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSource_name() {
        return source_name;
    }

    public void setSource_name(String source_name) {
        this.source_name = source_name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getPublishedDate() { return publishedDate; }
    public void setPublishedDate(String publishedDate) { this.publishedDate = publishedDate; }

    public String getContent() {
        return content;
    }

    public String getCategory() {
        return category;
    }


    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public int getDislikes() { return dislikes; }
    public void setDislikes(int dislikes) { this.dislikes = dislikes; }

    public int getReaders() { return readers; }
    public void setReaders(int readers) { this.readers = readers; }

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

    public void setCategory(String category) {
        this.category = category;
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

    @Override
    public String toString() {
        return "Article{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description;
    }
}

