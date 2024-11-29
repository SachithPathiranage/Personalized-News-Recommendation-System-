package org.example.OOD.Controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.example.OOD.Models.Article;
import org.example.OOD.Database_Handler.DatabaseHandler;
import org.example.OOD.Models.User;
import org.example.OOD.Models.UserPreferences;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NewsController {
    @FXML
    private ListView<HBox> newsListView;
    private User currentUser; // Store the logged-in user

    private static List<Article> fetchNewsFromDatabase() {
        List<Article> articles = new ArrayList<>();
        String query = "SELECT * FROM news";

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

                // Create org.example.OOD.Article object and add to list
                Article article = new Article(id, title, description, newsUrl, source, author, imageUrl, date);
                articles.add(article);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return articles;
    }

    public void initializeNews() {
        this.currentUser = User.getCurrentUser(); // Get the logged-in user
        if (currentUser != null) {
            System.out.println("Loading news for: " + currentUser.getName() + " with ID: " + currentUser.getId());

            // Fetch all articles from the database
            List<Article> articles = fetchNewsFromDatabase();

            try {
                // Fetch user preferences from the database
                List<Integer> likedArticles = DatabaseHandler.getInstance()
                        .fetchUserPreferences(currentUser.getId(), "liked");
                List<Integer> dislikedArticles = DatabaseHandler.getInstance()
                        .fetchUserPreferences(currentUser.getId(), "disliked");
                List<Integer> readArticles = DatabaseHandler.getInstance()
                        .fetchUserPreferences(currentUser.getId(), "read");

                // If no preferences exist, log a message
                if (likedArticles.isEmpty() && dislikedArticles.isEmpty() && readArticles.isEmpty()) {
                    System.out.println("No preferences found for the user. Initializing with default articles.");
                }

                // Log preferences for debugging
                System.out.println("Fetched liked articles: " + likedArticles);
                System.out.println("Fetched disliked articles: " + dislikedArticles);
                System.out.println("Fetched read articles: " + readArticles);


                // Iterate over the articles and update user preferences accordingly
                for (Article article : articles) {
                    if (likedArticles.contains(article.getId())) {
                        currentUser.getPreferences().addLikedArticle(article, currentUser.getId());
                    }
                    if (dislikedArticles.contains(article.getId())) {
                        currentUser.getPreferences().addDislikedArticle(article, currentUser.getId());
                    }
                    if (readArticles.contains(article.getId())) {
                        currentUser.getPreferences().addReadArticle(article, currentUser.getId());
                    }

                    // Create a news item for each article and add it to the ListView
                    HBox newsItem = createNewsItem(article);
                    newsListView.getItems().add(newsItem);
                }
            } catch (SQLException e) {
                // Handle any SQL exceptions gracefully
                e.printStackTrace();
                System.out.println("Error fetching user preferences from the database.");
            }
        } else {
            // Handle the case when no user is logged in
            System.out.println("No user is logged in.");
        }
    }

    private HBox createNewsItem(Article article) {
        HBox cellContainer = new HBox(10); // The outer container
        cellContainer.setPadding(new Insets(10));

        HBox hBox = new HBox(10); // The Content container
        hBox.setPadding(new Insets(10));
        hBox.setMaxWidth(870);
        hBox.getStyleClass().add("hbox-article");

        // Dynamically bind width to ListView's width
        hBox.prefWidthProperty().bind(newsListView.widthProperty().subtract(20)); // Subtract padding/margin as needed

        // Inner HBox for content
        HBox contentBox = new HBox(10);
        contentBox.setAlignment(Pos.CENTER_LEFT);

        // Image
        ImageView imageView = new ImageView();
        imageView.getStyleClass().add("image-article");

        // Placeholder image while loading
        imageView.setImage(new Image(getClass().getResource("/Images/news.jpeg").toExternalForm()));
        imageView.setFitWidth(150);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);

        // Background task to load the image
        Task<Image> loadImageTask = new Task<>() {
            @Override
            protected Image call() throws Exception {
                String imageUrl = article.getImageUrl();
                if (imageUrl == null || imageUrl.isEmpty()) {
                    throw new IllegalArgumentException("Invalid or missing image URL");
                }

                String finalUrl = resolveRedirects(imageUrl); // Resolve redirects

                // Check if specific headers are needed for this image
                if (requiresSpecialHeaders(finalUrl)) {
                    // Use fetchImageWithHeaders for images requiring headers
                    return fetchImageWithHeaders(finalUrl);
                } else {
                    // Standard image loading
                    return new Image(finalUrl, true); // Load asynchronously
                }
            }
        };

        // Success handler: Update ImageView with the loaded image
        loadImageTask.setOnSucceeded(event -> {
            Image loadedImage = loadImageTask.getValue();
            if (loadedImage.isError()) {
                // Use fallback if image loading fails
                imageView.setImage(new Image(getClass().getResource("/Images/news.jpeg").toExternalForm()));
            } else {
                imageView.setImage(loadedImage);
            }
        });

        // Error handler: Log and use fallback image
        loadImageTask.setOnFailed(event -> {
            Throwable exception = loadImageTask.getException();
            System.out.println("Error loading image for URL: " + article.getImageUrl() + " - " + exception.getMessage());
            imageView.setImage(new Image(getClass().getResource("/Images/news.jpeg").toExternalForm()));
        });

        // Run the task on a background thread
        new Thread(loadImageTask).start();


        // Text (middle section)
        VBox textContainer = new VBox(5);
        textContainer.setAlignment(Pos.TOP_LEFT);
        textContainer.setStyle("-fx-background-color: transparent;");

        // Title
        Label titleLabel = new Label(article.getTitle());
        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add("label-title");

        // Description
        Label descriptionLabel = new Label(article.getDescription());
        descriptionLabel.setWrapText(true);
        descriptionLabel.getStyleClass().add("label-description");

        // Source and date
        Label sourceDateLabel = new Label("Source: " + article.getSource_name() + " | " + article.getPublishedDate());
        sourceDateLabel.getStyleClass().add("label-source-date");

        // Buttons (Hidden initially)
        Button readButton = new Button("📰 Read Article");
        Button likeButton = new Button("👍 Like");
        Button dislikeButton = new Button("👎 Dislike");

        if (currentUser != null) {
            UserPreferences preferences = currentUser.getPreferences();

            // Set initial button labels and styles
            if (preferences.isLiked(article)) {
                likeButton.setText("Unlike");
                likeButton.setStyle("-fx-background-color: #2196f3; -fx-max-width: 125;");
            } else if (preferences.isDisliked(article)) {
                dislikeButton.setText("Remove Dislike");
                dislikeButton.setStyle("-fx-background-color: #f44336;-fx-max-width: 125;");
            }
            if (preferences.isRead(article)) {
                readButton.setText("Read Again ✅");
                readButton.setStyle("-fx-background-color: green;"); // Optional style for "read" state
            }

            // Like button action
            likeButton.setOnAction(event -> {
                if (preferences.isLiked(article)) {
                    preferences.removeLikedArticle(article,currentUser.getId());
                    likeButton.setText("👍 Like");
                    likeButton.setStyle(""); // Reset to no color
                } else {
                    preferences.addLikedArticle(article,currentUser.getId());
                    likeButton.setText("Unlike");
                    likeButton.setStyle("-fx-background-color: #2196f3; -fx-max-width: 125;"); // Blue for liked
                    dislikeButton.setText("👎 Dislike");
                    dislikeButton.setStyle(""); // Reset dislike button
                }
            });

            // Dislike button action
            dislikeButton.setOnAction(event -> {
                if (preferences.isDisliked(article)) {
                    preferences.removeDislikedArticle(article,currentUser.getId());
                    dislikeButton.setText("👎 Dislike");
                    dislikeButton.setStyle(""); // Reset to no color
                } else {
                    preferences.addDislikedArticle(article, currentUser.getId());
                    dislikeButton.setText("Remove Dislike");
                    dislikeButton.setStyle("-fx-background-color: #f44336; -fx-max-width: 125;"); // Red for disliked
                    likeButton.setText("👍 Like");
                    likeButton.setStyle(""); // Reset like button
                }
            });

            // Read button action
            readButton.setOnAction(event -> {
                User.ReadArticleAction(article);
                preferences.addReadArticle(article, currentUser.getId());
                readButton.setText("Read Again ✅");
                readButton.setStyle("-fx-background-color: green; -fx-max-width: 125;"); // Update to show article was read
            });

        }

        readButton.getStyleClass().add("read-button");
        likeButton.getStyleClass().add("like-button");
        dislikeButton.getStyleClass().add("dislike-button");

        VBox buttonContainer = new VBox(5, readButton, likeButton, dislikeButton);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setVisible(true); // Hide by default

        // Show buttons on hover
        cellContainer.setOnMouseEntered(event -> buttonContainer.setVisible(true));
        cellContainer.setOnMouseExited(event -> buttonContainer.setVisible(true));

        // Layout
        textContainer.getStyleClass().add("vbox-article");
        textContainer.getChildren().addAll(titleLabel, descriptionLabel, sourceDateLabel);

        contentBox.getChildren().addAll(imageView, textContainer);

        hBox.getChildren().add(contentBox);

        // Add both components (hBox and buttonContainer) to the outer cellContainer
        cellContainer.getChildren().addAll(hBox, buttonContainer);

        return cellContainer;
    }

    /**
     * Resolves redirects for the given URL.
     *
     * @param url The initial image URL.
     * @return The final resolved URL after following redirects.
     * @throws IOException If an I/O error occurs.
     */
    public String resolveRedirects(String url) throws IOException {
        return resolveRedirects(url, 0); // Start with zero redirects
    }

    private String resolveRedirects(String url, int redirectCount) throws IOException {
        if (redirectCount > 5) {
            throw new IOException("Too many redirects for URL: " + url);
        }

        // Use URI to handle deprecated URL constructors
        try {
            URI uri = new URI(url);
            URL urlObject = uri.toURL();

            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("HEAD");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                String redirectedUrl = connection.getHeaderField("Location");
                if (redirectedUrl == null || redirectedUrl.isEmpty()) {
                    throw new IOException("Redirection URL is missing for: " + url);
                }
                return resolveRedirects(redirectedUrl, redirectCount + 1); // Recursive call
            }

            return url; // No redirection

        } catch (Exception e) {
            throw new IOException("Error resolving URL: " + url, e);
        }
    }

    // Method to fetch the image with custom headers
    public Image fetchImageWithHeaders(String imageUrl) {
        try {
            // Resolve any redirects
            String resolvedUrl = resolveRedirects(imageUrl);

            // Create an HTTP client
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(resolvedUrl);
                request.addHeader("User-Agent", "Mozilla/5.0");
                request.addHeader("Referer", "https://fortune.com/"); // Example referer, change if needed

                // Execute the request and get the image as a stream
                InputStream inputStream = httpClient.execute(request).getEntity().getContent();
                return new Image(inputStream);
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch image: " + e.getMessage());
            // Fallback to placeholder image
            return new Image(getClass().getResource("/Images/news.jpeg").toExternalForm());
        }
    }

    private boolean requiresSpecialHeaders(String url) {
        // Example condition: Check for specific domains or patterns
        return url.contains("fortune.com") || url.contains("fxstreet.com");
    }
}
