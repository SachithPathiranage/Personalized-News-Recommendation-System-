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
    private static final String API_URL = Config.getProperties().getProperty("API_URL") + API_KEY;

    public void fetchAndStoreNews() {
        // List of categories for which you want to fetch news
        List<String> categories = List.of("business", "technology", "entertainment", "sports", "health", "general");

        try {
            // Loop through each category and fetch the news
            for (String category : categories) {
                // Fetch data for the current category
                String jsonResponse = fetchDataFromAPI(category);

                // Parse JSON and insert into database
                if (jsonResponse != null) {
                    System.out.println("Fetched news for category: " + category);
                    parseAndStoreNews(jsonResponse);  // Parse and store the fetched data
                }
            }
        } catch (Exception e) {
            System.err.println("Error while fetching and storing news: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String fetchDataFromAPI(String categories) throws Exception {
        StringBuilder result = new StringBuilder();

        // Construct the URL with categories and API_KEY
        String urlString = API_URL + "&category=" + categories + "&pageSize=6&apiKey=" + API_KEY;

        // Create the URL object with the constructed string
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        // Fetch data from API
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }

    private void parseAndStoreNews(String jsonResponse) throws SQLException {
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONArray articles = jsonObject.getJSONArray("articles");

        String insertQuery = "INSERT INTO news (title, description, content, url, published_at, source_name, author, image_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        // Adjust IDs
        adjustIDs();

        // Use try-with-resources for automatic resource management
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {

            int batchSize = 0;

            for (int i = 0; i < articles.length(); i++) {
                JSONObject article = articles.getJSONObject(i);

                String title = article.optString("title", "").trim();
                String description = article.optString("description", "").trim();
                String content = article.optString("content", "").trim();
                String url = article.optString("url", "").trim();
                String publishedAt = article.optString("publishedAt", "").replace("T", " ").replace("Z", "").trim();
                String sourceName = article.getJSONObject("source").optString("name", "").trim();
                String author = article.optString("author", "").trim();
                String imageUrl = article.optString("urlToImage", "").trim();

                // Skip invalid articles:
                // - If description, content, title, or URL is missing or empty
                // - If the article already exists in the database
                // - If it's marked for removal
                if (description.isEmpty() || content.isEmpty() || title.isEmpty() || url.isEmpty() ||
                        DatabaseHandler.getInstance().isArticleExists(url) ||
                        isRemovedArticle(title, description, content, url)) {
                    continue; // Skip this article
                }

                // Truncate the image URL if it's too long
                if (imageUrl.length() > 500) {
                    imageUrl = imageUrl.substring(0, 500);
                }

                // Set parameters
                preparedStatement.setString(1, title);
                preparedStatement.setString(2, description);
                preparedStatement.setString(3, content);
                preparedStatement.setString(4, url);
                preparedStatement.setString(5, publishedAt);
                preparedStatement.setString(6, sourceName);
                preparedStatement.setString(7, author);
                preparedStatement.setString(8, imageUrl);

                // Add to batch
                preparedStatement.addBatch();
                batchSize++;

                // Execute batch after every 1000 articles to avoid memory issues with large data
                if (batchSize % 1000 == 0) {
                    preparedStatement.executeBatch();
                    System.out.println("Inserted batch of 1000 articles.");
                }
            }

            // Execute remaining batch if there are any left
            if (batchSize % 1000 != 0) {
                preparedStatement.executeBatch();
                System.out.println("Inserted remaining articles.");
            }

            System.out.println("News articles have been successfully stored in the database.");
        } catch (SQLException e) {
            // Log error for debugging purposes
            System.err.println("SQL error while inserting articles: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // Catch any other exceptions (e.g., JSON parsing errors)
            System.err.println("Error while parsing or storing news: " + e.getMessage());
            e.printStackTrace();
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
