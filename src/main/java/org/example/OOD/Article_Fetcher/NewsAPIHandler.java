package org.example.OOD.Article_Fetcher;

import javafx.scene.control.Alert;
import org.example.OOD.Configurations.Config;
import org.example.OOD.Database_Handler.DatabaseHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;

import static org.example.OOD.Configurations.Alerts.showAlert;
import static org.example.OOD.Database_Handler.DatabaseHandler.getConnection;

public class NewsAPIHandler {

    private static final String API_KEY = Config.getProperties().getProperty("NEWS_API_KEY");
    private static final String API_URL = Config.getProperties().getProperty("API_URL");
    private static final String COUNTRY = "us"; // Specify country for the news (optional)

    public void fetchAndSaveNewsArticles(String category) {
        try {
            // Build the API URL
            String urlString = API_URL + "?category=" + category + "&country=" + COUNTRY + "&apiKey=" + API_KEY;
            URL url = new URL(urlString);

            // Connect to the API
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the API response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse the JSON response
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray articles = jsonResponse.getJSONArray("articles");

                for (int i = 0; i < articles.length(); i++) {
                    JSONObject article = articles.getJSONObject(i);
                    String title = article.optString("title", "N/A");
                    String description = article.optString("description", "N/A");
                    String content = article.optString("content", "N/A");
                    String urlToArticle = article.optString("url", "N/A");
                    String publishedAt = article.optString("publishedAt", "N/A");
                    String sourceName = article.getJSONObject("source").optString("name", "N/A");
                    String author = article.optString("author", "N/A");
                    String imageUrl = article.optString("urlToImage", "N/A");

                    // Before saving an article
                    if (DatabaseHandler.isRemovedArticle(title, description, content, urlToArticle)) {
                        System.out.println("Skipping removed article: " + title);
                    } else {
                        DatabaseHandler.saveArticleToDatabase(title, description, content, urlToArticle, publishedAt, sourceName, author, imageUrl);
                    }

                    // After bulk deletions or significant updates
                    DatabaseHandler.adjustIDs();

                }

                System.out.println("Articles from category \"" + category + "\" have been fetched and saved.");

            } else {
                System.err.println("Failed to fetch articles. HTTP response code: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Error fetching or saving articles: " + e.getMessage());
        }
    }
}
