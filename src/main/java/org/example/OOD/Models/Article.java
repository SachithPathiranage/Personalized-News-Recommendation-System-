package org.example.OOD.Models;

import java.util.concurrent.atomic.AtomicInteger;

public class Article {
    private int id;
    private String title;
    private String description;
    private String url;
    private String source_name;
    private String author;
    private String imageUrl;
    private String publishedDate;
    private AtomicInteger likes;
    private AtomicInteger dislikes;
    private String category;


    // Constructor
    public Article(int id, String title, String description, String url, String source_name, String author, String imageUrl, String publishedDate) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.url = url;
        this.source_name = source_name;
        this.author = author;
        this.imageUrl = imageUrl;
        this.publishedDate = publishedDate;
        this.likes = new AtomicInteger(0);
        this.dislikes = new AtomicInteger(0);
    }
    public Article(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public Article(int id, String title, String description, String newsUrl, String source, String author, String imageUrl, String date, String category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.url = newsUrl;
        this.source_name = source;
        this.author = author;
        this.imageUrl = imageUrl;
        this.publishedDate = date;
        this.likes = new AtomicInteger(0);
        this.dislikes = new AtomicInteger(0);
        this.category = category;
    }

    public Article(int id, String title, String description, String sourceName, String publishedDate, String imageUrl, String category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.source_name = sourceName;
        this.publishedDate = publishedDate;
        this.imageUrl = imageUrl;
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

    public int getLikes() {
        return likes.get();
    }

    public int getDislikes() {
        return dislikes.get();
    }

    // Methods to like and dislike the article
    public void likeArticle() {
        likes.incrementAndGet(); // Thread-safe increment
        System.out.println("Article liked! Total likes: " + likes);
    }

    public void dislikeArticle() {
        dislikes.incrementAndGet(); // Thread-safe increment
        System.out.println("Article disliked! Total dislikes: " + dislikes);
    }
}

