package org.example.OOD.Database_Handler;

import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import org.example.OOD.Configurations.Config;
import org.example.OOD.Models.Admin;
import org.example.OOD.Models.Article;
import org.example.OOD.Models.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DatabaseHandler {
    private static volatile DatabaseHandler instance;
    private static final String URL = Config.getProperties().getProperty("DB_URL");
    private static final String USER = Config.getProperties().getProperty("DB_USER");
    private static final String PASSWORD = Config.getProperties().getProperty("DB_PASSWORD");
    private static final int MAX_RETRIES = 3; // max retry attempts
    private static Connection connection;

    public DatabaseHandler() throws SQLException {
        connectWithRetry();
    }

    private static void connectWithRetry() throws SQLException {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                attempt++;
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                //System.out.println("Database connected successfully.");
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

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////Methods use for User Interactions/////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    //Method to Authenticate User
    public User authenticateUserAndRetrieveDetails(String email, String password, String selectUserSQL) {
        try (Connection connection = DatabaseHandler.getInstance().getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectUserSQL)) {

                preparedStatement.setString(1, email);
                preparedStatement.setString(2, password); // Ensure password is hashed similarly if hashed in the database

                ResultSet resultSet = preparedStatement.executeQuery();

                // Check if a record exists and retrieve the user's details
                if (resultSet.next()) {
                    User user = new User();
                    user.setId(resultSet.getString("id")); // Retrieve the id column
                    user.setName(resultSet.getString("name")); // Retrieve the name column
                    user.setEmail(email); // Set the email since it's already known
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Return null if authentication fails
    }

    //Method to Save User
    public static void saveUserToDatabase(User user) throws SQLException {
        String email = user.getEmail();
        String ID = user.getId();
        //String unique_code = user.getUnique_code();
        String name = user.getName();
        String password = user.getPassword();

        String insertUserSQL = "INSERT INTO users (id, name, email, password) VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseHandler.getInstance().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertUserSQL)) {

            preparedStatement.setString(1, String.valueOf(ID));
            //preparedStatement.setString(2, String.valueOf(unique_code));
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, email);
            preparedStatement.setString(4, password); // Consider hashing the password

            int rowsInserted = preparedStatement.executeUpdate();
            if (rowsInserted > 0) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Sign-up successful! Please log in.");
                alert.showAndWait();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Sign-up failed! Please check your details.");
            alert.showAndWait();
        }
    }

    //Method to Generate User ID
    public static String generateUserId() throws SQLException {
        String lastUserIdQuery = "SELECT id FROM users WHERE id LIKE 'U%' ORDER BY CAST(SUBSTRING(id, 2) AS UNSIGNED) DESC LIMIT 1";
        String newUserId = "U01"; // Default ID if no users exist

        try (PreparedStatement stmt = DatabaseHandler.getInstance().getConnection().prepareStatement(lastUserIdQuery);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                // Extract numeric part of last user ID (e.g., "U05" -> 5)
                String lastUserId = rs.getString("id");
                int lastIdNumber = Integer.parseInt(lastUserId.substring(1));

                // Increment the number part and format with leading zeros
                newUserId = String.format("U%02d", lastIdNumber + 1);
            }
        }
        return newUserId;
    }

    //Method to Check if Email is Registered
    public static boolean isEmailRegistered(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try {
            DatabaseHandler.getInstance();
            try (Connection connection = DatabaseHandler.getInstance().getConnection();
                 PreparedStatement pstmt = connection.prepareStatement(sql)) {

                pstmt.setString(1, email);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0; // true if email is already registered
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //Method to Get All Users
    public List<User> getUsers() {
        List<User> userList = FXCollections.observableArrayList();
        String query = "SELECT * FROM users";

        // Manually handle the database connection
        try (Connection connection = DatabaseHandler.getInstance().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet rs = preparedStatement.executeQuery()) {

            // Process the result set
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String email = rs.getString("email");
                userList.add(new User(id, name, email));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return userList;
    }

    public void deleteUserAndPreferences(String userId) throws SQLException {
        // Establish connection
        Connection connection = DatabaseHandler.getInstance().getConnection();
        PreparedStatement deletePreferencesStmt = null;
        PreparedStatement deleteUserStmt = null;

        try {
            // Disable auto-commit to handle transactions
            connection.setAutoCommit(false);

            // Query to delete preferences associated with the user
            String deletePreferencesQuery = "DELETE FROM user_preferences WHERE user_id = ?";
            deletePreferencesStmt = connection.prepareStatement(deletePreferencesQuery);
            deletePreferencesStmt.setString(1, userId);
            deletePreferencesStmt.executeUpdate();

            // Query to delete the user record
            String deleteUserQuery = "DELETE FROM users WHERE id = ?";
            deleteUserStmt = connection.prepareStatement(deleteUserQuery);
            deleteUserStmt.setString(1, userId);
            deleteUserStmt.executeUpdate();

            // Commit transaction
            connection.commit();

            System.out.println("Deleted user and associated preferences successfully for User ID: " + userId);

        } catch (SQLException e) {
            // Roll back in case of an error
            if (connection != null) {
                connection.rollback();
            }
            e.printStackTrace();
            throw e;
        } finally {
            // Close resources
            if (deletePreferencesStmt != null) deletePreferencesStmt.close();
            if (deleteUserStmt != null) deleteUserStmt.close();
            if (connection != null) connection.setAutoCommit(true); // Reset auto-commit to default
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////Methods use for Admin Interactions/////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //Method to Authenticate Admin
    public static Admin handleLogin_Admin(String email, String password, String query) {
        try (Connection connection = DatabaseHandler.getInstance().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, email);
            preparedStatement.setString(2, password);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String name = resultSet.getString("Name"); // Retrieve the name from the result set
                return new Admin(name, email, password);   // Use the Admin constructor to store the name
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // Return null if authentication fails or an exception occurs
    }

    //Method to Save Article
    public boolean saveArticle(Article article) {
        String insertQuery = "INSERT INTO news (title, description, content, url, published_at, source_name, author, image_url, category, likes, dislikes, readers) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
            // Set parameters from Article object
            statement.setString(1, article.getTitle());
            statement.setString(2, article.getDescription());
            statement.setString(3, article.getContent());
            statement.setString(4, article.getUrl());

            // Use current date and time for published_at
            String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            statement.setString(5, currentDateTime);

            statement.setString(6, article.getSource_name());
            statement.setString(7, article.getAuthor());
            statement.setString(8, article.getImageUrl());
            statement.setString(9, article.getCategory());

            // Likes, dislikes, readers default to 0
            statement.setInt(10, article.getLikes());
            statement.setInt(11, article.getDislikes());
            statement.setInt(12, article.getReaders());

            // Execute query
            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("Article saved successfully!");
                //refreshView();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////Methods use for User Preferences Interactions - Like,Dislike & Read/////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Increment likes for an article
    public void incrementMetric(int articleId, String metric) throws SQLException {
        String query = "UPDATE news SET " + metric + " = " + metric + " + 1 WHERE id = ?";
        try (Connection connection = getConnection()) {
            if (connection != null && !connection.isClosed()) {
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setInt(1, articleId);
                    statement.executeUpdate();
                }
            } else {
                System.out.println("Connection is closed or invalid. Attempting to reconnect...");
                // Optionally, attempt reconnection or handle as per your app's needs.
            }
        }
    }

    // Decrement likes for an article
    public void decrementMetric(int articleId, String metric) throws SQLException {
        String query = "UPDATE news SET " + metric + " = " + metric + " - 1 WHERE id = ? AND " + metric + " > 0";
        try (Connection connection = getConnection()) {
            if (connection != null && !connection.isClosed()) {
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setInt(1, articleId);
                    statement.executeUpdate();
                }
            } else {
                System.out.println("Connection is closed or invalid. Attempting to reconnect...");
                // Optionally, attempt reconnection or handle as per your app's needs.
            }
        }
    }

    //Method to Save Preferences
    public void saveUserPreference(String userId, int articleId, String preferenceType) throws SQLException {
        String query = "INSERT INTO user_preferences (user_id, article_id, preference_type) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseHandler.getInstance().getConnection();
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
        try (PreparedStatement statement = DatabaseHandler.getInstance().getConnection().prepareStatement(query)) {
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

        try (Connection connection = DatabaseHandler.getInstance().getConnection();
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

    //Method to Fetch All User Preferences
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

    //Method to Fetch User Preferences with Timestamp
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

    //Method to Check if Preference Exists
    public boolean isPreferenceRecorded(String userId, int articleId, String preferenceType) throws SQLException {
        String query = "SELECT COUNT(*) FROM user_preferences WHERE user_id = ? AND article_id = ? AND preference_type = ?";
        try (Connection connection = DatabaseHandler.getInstance().getConnection();
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

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////Methods use for Article Interactions/////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //Method to Create News Table
    public static void createNewsTableIfNotExists() {
        String createTableQuery = """
        CREATE TABLE IF NOT EXISTS news (
            id INT AUTO_INCREMENT PRIMARY KEY,
            title VARCHAR(255) NOT NULL,
            description TEXT,
            content TEXT,
            url VARCHAR(255) UNIQUE NOT NULL,
            published_at DATETIME,
            source_name VARCHAR(255),
            author VARCHAR(255),
            image_url VARCHAR(255),
            category VARCHAR(255),
            likes INT DEFAULT 0,
            dislikes INT DEFAULT 0,
            readers INT DEFAULT 0
        );
    """;

        try (var connection = DatabaseHandler.getConnection();
             var statement = connection.createStatement()) {
            statement.executeUpdate(createTableQuery);
            System.out.println("News table ensured to exist.");
        } catch (SQLException e) {
            System.err.println("Error creating news table: " + e.getMessage());
        }
    }

    //Method to Save Article
    public static void saveArticleToDatabase(String title, String description, String content, String url, String publishedAt,
                                             String sourceName, String author, String imageUrl) {
        createNewsTableIfNotExists(); // Ensure table exists before saving the article

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

    //Method to Check if Article is Removed
    public static boolean isRemovedArticle(String title, String description, String content, String url) {
        return "[Removed]".equalsIgnoreCase(title) ||
                "[Removed]".equalsIgnoreCase(description) ||
                "[Removed]".equalsIgnoreCase(content) ||
                "https://removed.com".equalsIgnoreCase(url);
    }

    //Method to Adjust IDs
    public static void adjustIDs() {
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

    //Method to Check if Article Exists
    public boolean isArticleExists(String url) throws SQLException {
        String query = "SELECT COUNT(*) FROM news WHERE url = ?";
        try (Connection connection = DatabaseHandler.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, url);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0; // If count > 0, the article exists
            }
        }
        return false;
    }

//    // Fetch articles from the database
//    public List<Article> fetchArticles() throws SQLException {
//        List<Article> articles = new ArrayList<>();
//        String query = "SELECT id, title, description, url FROM news";
//
//        try (Connection connection = DatabaseHandler.getInstance().getConnection();
//             PreparedStatement statement = connection.prepareStatement(query);
//             ResultSet resultSet = statement.executeQuery()) {
//
//            while (resultSet.next()) {
//                int id = resultSet.getInt("id");
//                String title = resultSet.getString("title");
//                String description = resultSet.getString("description");
//                String url = resultSet.getString("url");
//
//                articles.add(new Article(id, title, description,url));
//            }
//        }
//        return articles;
//    }

    //Method to Fetch News From Database
    public static List<Article> fetchNewsFromDatabase() {
        List<Article> articles = new ArrayList<>();
        String query = "SELECT id, title, description, url, published_at, author, source_name, image_url, category, content, likes, dislikes, readers FROM news";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String title = resultSet.getString("title");
                String description = resultSet.getString("description");
                String newsUrl = resultSet.getString("url");
                String date = resultSet.getString("published_at");
                String author = resultSet.getString("author");
                String source = resultSet.getString("source_name");
                String imageUrl = resultSet.getString("image_url");
                String category = resultSet.getString("category");
                String content = resultSet.getString("content");

                // Fetch the new columns
                int likes = resultSet.getInt("likes");
                int dislikes = resultSet.getInt("dislikes");
                int readers = resultSet.getInt("readers");

                // Create org.example.OOD.Article object and add to list
                Article article = new Article(id, title, description, newsUrl, source, author, imageUrl, date, category, content, likes, dislikes, readers);
                articles.add(article);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return articles;
    }

    // Method to fetch articles by category
    public List<Article> fetchArticlesByCategory(String category) throws SQLException {
        List<Article> articles = new ArrayList<>();
        String query = "SELECT * FROM news WHERE category = ?";
        try (Connection connection = DatabaseHandler.getInstance().getConnection();
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

    // Method to fetch an article by ID
    public Article fetchArticleById(int articleId) throws SQLException {
        Article article = null;
        String query = "SELECT id, title, description, content, category FROM news WHERE id = ?";

        try (Connection connection = DatabaseHandler.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, articleId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String title = resultSet.getString("title");
                    String description = resultSet.getString("description");
                    String content = resultSet.getString("content");
                    String category = resultSet.getString("category");

                    // Create an Article object
                    article = new Article(articleId, title, description, content, category);
                }
            }
        }
        return article;
    }

    // Method to fetch the category of an article by ID
    public String fetchArticleCategoryById(int articleId) throws SQLException {
        String query = "SELECT category FROM news WHERE id = ?";
        try (Connection connection = DatabaseHandler.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, articleId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("category");
                }
            }
        }
        return null; // Return null if no category is found
    }


    // Update the category for an article
    public void updateArticleCategory(int articleId, String category) throws SQLException {
        String query = "UPDATE news SET category = ? WHERE id = ?";

        try (Connection connection = DatabaseHandler.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, category);
            statement.setInt(2, articleId);
            statement.executeUpdate();
        }
    }

    public void deleteArticleAndPreferences(int articleId) throws SQLException {
        // Establish connection
        Connection connection = DatabaseHandler.getInstance().getConnection();
        PreparedStatement deletePreferencesStmt = null;
        PreparedStatement deleteArticleStmt = null;

        try {
            connection.setAutoCommit(false);  // Start transaction

            // Delete preferences related to the article
            String deletePreferencesQuery = "DELETE FROM user_preferences WHERE article_id = ?";
            deletePreferencesStmt = connection.prepareStatement(deletePreferencesQuery);
            deletePreferencesStmt.setInt(1, articleId);
            deletePreferencesStmt.executeUpdate();

            // Delete the article from articles table
            String deleteArticleQuery = "DELETE FROM news WHERE id = ?";
            deleteArticleStmt = connection.prepareStatement(deleteArticleQuery);
            deleteArticleStmt.setInt(1, articleId);
            deleteArticleStmt.executeUpdate();

            // Commit the transaction
            connection.commit();
        } catch (SQLException e) {
            if (connection != null) {
                connection.rollback();  // Rollback in case of error
            }
            e.printStackTrace();
            throw e;
        } finally {
            // Close all resources
            if (deletePreferencesStmt != null) deletePreferencesStmt.close();
            if (deleteArticleStmt != null) deleteArticleStmt.close();
            if (connection != null) connection.setAutoCommit(true);  // Reset auto-commit
        }
    }

}

//    public static void testConnection() {
//        try (Connection connection = DatabaseHandler.getInstance().getConnection()) {
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



