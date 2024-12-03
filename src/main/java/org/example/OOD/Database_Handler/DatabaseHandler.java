package org.example.OOD.Database_Handler;

import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import org.example.OOD.Configurations.Config;
import org.example.OOD.Models.Article;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHandler {
    private static volatile DatabaseHandler instance;
    private static final String URL = Config.getProperties().getProperty("DB_URL");
    private static final String USER = Config.getProperties().getProperty("DB_USER");
    private static final String PASSWORD = Config.getProperties().getProperty("DB_PASSWORD");
    private static final int MAX_RETRIES = 3; // max retry attempts
    private static Connection connection;

    private DatabaseHandler() throws SQLException {
        connectWithRetry();
    }

    private static void connectWithRetry() throws SQLException {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                attempt++;
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Database connected successfully.");
                break;
            } catch (CommunicationsException e) {
                System.err.println("Attempt " + attempt + ": Communications link failure. Retrying...");
                if (attempt == MAX_RETRIES) {
                    System.err.println("Max retries reached. Could not establish a database connection.");
                    throw new SQLException("Unable to connect to database after " + MAX_RETRIES + " attempts.", e);
                }
                try {
                    Thread.sleep(2000); // wait for 2 seconds before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Thread interrupted during retry wait", ie);
                }
            } catch (ClassNotFoundException | SQLException ex) {
                System.err.println("Database connection creation failed: " + ex.getMessage());
                throw new SQLException("Error connecting to the database", ex);
            }
        }
    }

    public static DatabaseHandler getInstance() throws SQLException {
        if (instance == null) {
            synchronized (DatabaseHandler.class) {
                if (instance == null) {
                    instance = new DatabaseHandler();
                }
            }
        } else if (connection == null || connection.isClosed()) {
            synchronized (DatabaseHandler.class) {
                if (connection == null || connection.isClosed()) {
                    instance = new DatabaseHandler();
                }
            }
        }
        return instance;
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connectWithRetry();
        }
        return connection;
    }

    //Method to Save Preferences
    public void saveUserPreference(String userId, int articleId, String preferenceType) throws SQLException {
        String query = "INSERT INTO user_preferences (user_id, article_id, preference_type) VALUES (?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, userId);
            statement.setInt(2, articleId);
            statement.setString(3, preferenceType.toLowerCase());
            statement.executeUpdate();
            System.out.println("Preference saved: " + preferenceType + " for article ID " + articleId);
        }
    }

    //Method to Remove Preferences
    public void removeUserPreference(String userId, int articleId, String preferenceType) throws SQLException {
        String query = "DELETE FROM user_preferences WHERE user_id = ? AND article_id = ? AND preference_type = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, userId);
            statement.setInt(2, articleId);
            statement.setString(3, preferenceType);
            statement.executeUpdate();
        }
    }

    //Method to Fetch User Preferences
    public List<Integer> fetchUserPreferences(String userId, String preferenceType) throws SQLException {
        List<Integer> articleIds = new ArrayList<>();
        String query = "SELECT article_id FROM user_preferences WHERE user_id = ? AND preference_type = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, userId);
            statement.setString(2, preferenceType.toLowerCase()); // Pass the preferenceType to filter

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                articleIds.add(resultSet.getInt("article_id"));
            }
        }

        return articleIds;
    }

    public Map<String, List<Integer>> fetchAllUserPreferences(String userId) throws SQLException {
        Map<String, List<Integer>> preferencesMap = new HashMap<>();
        preferencesMap.put("liked", new ArrayList<>());
        preferencesMap.put("disliked", new ArrayList<>());
        preferencesMap.put("read", new ArrayList<>());

        String query = "SELECT article_id, preference_type FROM user_preferences WHERE user_id = ?";

        try (Connection connection = DatabaseHandler.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String preferenceType = resultSet.getString("preference_type");
                    int articleId = resultSet.getInt("article_id");

                    preferencesMap.get(preferenceType).add(articleId);
                }
            }
        }

        return preferencesMap;
    }

    public List<Map<String, Object>> fetchUserPreferencesWithTimestamp(String userId, String preferenceType) throws SQLException {
        List<Map<String, Object>> preferencesList = new ArrayList<>();

        String query = "SELECT article_id, created_at FROM user_preferences WHERE user_id = ? AND preference_type = ?";

        try (Connection connection = DatabaseHandler.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, userId);
            statement.setString(2, preferenceType);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int articleId = resultSet.getInt("article_id");
                    Timestamp createdAt = resultSet.getTimestamp("created_at");

                    Map<String, Object> preferenceDetails = new HashMap<>();
                    preferenceDetails.put("articleId", articleId);
                    preferenceDetails.put("createdAt", createdAt);

                    preferencesList.add(preferenceDetails);
                }
            }
        }

        return preferencesList;
    }

    public boolean isPreferenceRecorded(String userId, int articleId, String preferenceType) throws SQLException {
        String query = "SELECT COUNT(*) FROM user_preferences WHERE user_id = ? AND article_id = ? AND preference_type = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, userId);
            statement.setInt(2, articleId);
            statement.setString(3, preferenceType.toLowerCase());

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0; // Check if the count is greater than 0
            }
        }
        return false;
    }

    public boolean isArticleExists(String url) throws SQLException {
        String query = "SELECT COUNT(*) FROM news WHERE url = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, url);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0; // If count > 0, the article exists
            }
        }
        return false;
    }

    // Fetch articles from the database
    public List<Article> fetchArticles() throws SQLException {
        List<Article> articles = new ArrayList<>();
        String query = "SELECT id, title FROM news";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String title = resultSet.getString("title");
                articles.add(new Article(id, title));
            }
        }
        return articles;
    }

    public List<Article> fetchArticlesByCategory(String category) throws SQLException {
        List<Article> articles = new ArrayList<>();
        String query = "SELECT * FROM news WHERE category = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, category);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String title = resultSet.getString("title");
                String description = resultSet.getString("description");
                String sourceName = resultSet.getString("source_name");
                String publishedDate = resultSet.getString("published_at");
                String imageUrl = resultSet.getString("image_url");
                articles.add(new Article(id, title, description, sourceName, publishedDate, imageUrl, category));
            }
        }
        return articles;
    }


    // Update the category for an article
    public void updateArticleCategory(int articleId, String category) throws SQLException {
        String query = "UPDATE news SET category = ? WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, category);
            statement.setInt(2, articleId);
            statement.executeUpdate();
        }
    }

}

//    public static void testConnection() {
//        try (Connection connection = getConnection()) {
//            if (connection != null && !connection.isClosed()) {
//                System.out.println("Connection successful!");
//            } else {
//                System.out.println("Failed to connect.");
//            }
//        } catch (SQLException e) {
//            System.err.println("Error connecting to the database: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }



