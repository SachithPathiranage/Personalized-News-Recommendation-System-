package org.example.OOD.Models;

import java.util.ArrayList;
import java.util.List;

public class Category {
    private String name;
    private List<String> articles;

    public Category(String name) {
        this.name = name;
        this.articles = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<String> getArticles() {
        return articles;
    }

    public void addArticle(String article) {
        this.articles.add(article);
    }
}

