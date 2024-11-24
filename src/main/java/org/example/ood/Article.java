package org.example.ood;

public class Article {
    private int id;
    private String title;
    private String description;
    private String source_name;
    private String author;
    private String imageUrl;
    private String publishedDate;

    // Constructor
    public Article(int id, String title, String description,String source_name, String author, String imageUrl, String publishedDate) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.source_name = source_name;
        this.author = author;
        this.imageUrl = imageUrl;
        this.publishedDate = publishedDate;
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

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getPublishedDate() { return publishedDate; }
    public void setPublishedDate(String publishedDate) { this.publishedDate = publishedDate; }
}
