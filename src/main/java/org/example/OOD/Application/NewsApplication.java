package org.example.OOD.Application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.OOD.Article_Fetcher.NewsAPIHandler;
import org.example.OOD.Database_Handler.DatabaseHandler;
import org.example.OOD.Models.Article;
import org.example.OOD.Recommendation_Engine.RecommendationEngine;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class NewsApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(NewsApplication.class.getResource("/Design_Files/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 650, 455);
        stage.setTitle("User Login");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        // Cleanup resources if needed
        System.out.println("Application is stopping...");
        Platform.exit();
        System.exit(0); // Ensure JVM termination
    }

    public static void main(String[] args) {
        //Login_SignupController.testConnection();
//        NewsAPIHandler newsAPIHandler = new NewsAPIHandler();
//
//        // List of categories to fetch
//        String[] categories = {"business", "technology", "sports", "health", "science", "entertainment"};
//
//        for (String category : categories) {
//            newsAPIHandler.fetchAndSaveNewsArticles(category);
//        }
        try {
            // Step 1: Fetch articles from the database
            DatabaseHandler dbHandler = DatabaseHandler.getInstance();
            List<Article> articles = dbHandler.fetchArticles();

            if (articles.isEmpty()) {
                System.out.println("No articles found in the database.");
                return;
            }

            // Step 2: Extract titles for categorization
            RecommendationEngine recommendationEngine = new RecommendationEngine();
            List<String> titles = articles.stream()
                    .map(Article::getTitle)
                    .toList(); // Convert to a list of titles

            System.out.println("Titles to categorize: " + titles);

            // Step 3: Categorize articles
            String[] categories = recommendationEngine.categorizeArticles(titles);

            // Step 4: Update the database with categories
            for (int i = 0; i < articles.size(); i++) {
                Article article = articles.get(i);
                String category = categories[i];

                dbHandler.updateArticleCategory(article.getId(), category);
                System.out.println("Article ID: " + article.getId() +
                        " | Title: " + article.getTitle() +
                        " | Predicted Category: " + category);
            }

            System.out.println("Categorization complete!");

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        launch();
    }
}