package org.example.OOD.Article_Fetcher;

import org.example.OOD.Configurations.Config;
import org.example.OOD.Database_Handler.DatabaseHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.List;

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
                    if (isRemovedArticle(title, description, content, urlToArticle)) {
                        System.out.println("Skipping removed article: " + title);
                    } else {
                        saveArticleToDatabase(title, description, content, urlToArticle, publishedAt, sourceName, author, imageUrl);
                    }

                    // After bulk deletions or significant updates
                    adjustIDs();

                }

                System.out.println("Articles from category \"" + category + "\" have been fetched and saved.");
            } else {
                System.err.println("Failed to fetch articles. HTTP response code: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Error fetching or saving articles: " + e.getMessage());
        }
    }

    private void saveArticleToDatabase(String title, String description, String content, String url, String publishedAt,
                                       String sourceName, String author, String imageUrl) {
        String insertQuery = "INSERT INTO news (title, description, content, url, published_at, source_name, author, image_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            // Convert ISO 8601 to MySQL-compatible DATETIME format
            String formattedPublishedAt = publishedAt.replace("T", " ").replace("Z", "");

            // Check if the article already exists
            DatabaseHandler dbHandler = DatabaseHandler.getInstance();
            if (!dbHandler.isArticleExists(url)) {
                try (var connection = DatabaseHandler.getConnection();
                     var preparedStatement = connection.prepareStatement(insertQuery)) {
                    preparedStatement.setString(1, title);
                    preparedStatement.setString(2, description);
                    preparedStatement.setString(3, content);
                    preparedStatement.setString(4, url);
                    preparedStatement.setString(5, formattedPublishedAt);
                    preparedStatement.setString(6, sourceName);
                    preparedStatement.setString(7, author);
                    preparedStatement.setString(8, imageUrl);

                    preparedStatement.executeUpdate();
                    System.out.println("Article saved: " + title);
                }
            } else {
                System.out.println("Article already exists in the database: " + title);
            }
        } catch (SQLException e) {
            System.err.println("Error saving article to database: " + e.getMessage());
        }
    }

    private boolean isRemovedArticle(String title, String description, String content, String url) {
        return "[Removed]".equalsIgnoreCase(title) ||
                "[Removed]".equalsIgnoreCase(description) ||
                "[Removed]".equalsIgnoreCase(content) ||
                "https://removed.com".equalsIgnoreCase(url);
    }

    public void adjustIDs() {
        String fetchQuery = "SELECT id FROM news ORDER BY id ASC";
        String updateQuery = "UPDATE news SET id = ? WHERE id = ?";

        try (Connection connection = getConnection();
             Statement fetchStatement = connection.createStatement();
             ResultSet resultSet = fetchStatement.executeQuery(fetchQuery);
             PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {

            int newId = 1;
            while (resultSet.next()) {
                int oldId = resultSet.getInt("id");

                // Update ID only if it's different
                if (oldId != newId) {
                    updateStatement.setInt(1, newId);
                    updateStatement.setInt(2, oldId);
                    updateStatement.executeUpdate();
                }

                newId++;
            }

            // Reset the auto-increment value
            try (Statement resetAutoIncrement = connection.createStatement()) {
                resetAutoIncrement.execute("ALTER TABLE news AUTO_INCREMENT = " + newId);
            }

            System.out.println("IDs adjusted successfully.");
        } catch (Exception e) {
            System.err.println("Error while adjusting IDs: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
