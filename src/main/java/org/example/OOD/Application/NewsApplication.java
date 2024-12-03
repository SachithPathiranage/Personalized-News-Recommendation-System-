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
            // Step 1: Get the database handler instance
            DatabaseHandler dbHandler = DatabaseHandler.getInstance();

            // Step 2: Create the RecommendationEngine instance
            RecommendationEngine recommendationEngine = new RecommendationEngine();

            // Step 3: Call the method to categorize and update articles
            recommendationEngine.categorizeAndUpdateArticles(dbHandler);

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        launch();
    }
}