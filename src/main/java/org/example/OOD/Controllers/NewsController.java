package org.example.OOD.Controllers;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.example.OOD.Application.NewsApplication;
import org.example.OOD.Database_Handler.DatabaseHandler;
import org.example.OOD.Models.Article;
import org.example.OOD.Models.Category;
import org.example.OOD.Models.User;
import org.example.OOD.Models.UserPreferences;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static org.example.OOD.Configurations.Alerts.showAlert;
import static org.example.OOD.Database_Handler.DatabaseHandler.fetchNewsFromDatabase;
import static org.example.OOD.Models.UserPreferences.logPreferences;

public class NewsController {
    @FXML
    public WebView webView;
    @FXML
    private ListView<HBox> newsListView;
    private User currentUser; // Store the logged-in user

    @FXML
    private ListView<HBox> businessListView;

    @FXML
    private ListView<HBox> entertainmentListView;

    @FXML
    private ListView<HBox> foodListView;

    @FXML
    private ListView<HBox> healthListView;

    @FXML
    private ListView<HBox> politicsListView;

    @FXML
    private ListView<HBox> sportsListView;

    @FXML
    private ListView<HBox> techListView;

    @FXML
    private ListView<HBox> travelListView;

    @FXML
    private ComboBox<Integer> articleLimitDropdown;
    private static final int DEFAULT_ARTICLE_LIMIT = 10;
    private Map<String, ListView<HBox>> categoryMap;
    private Map<Integer, List<HBox>> articleUIMap = new HashMap<>();


    public void initializeNews() {

        // Initialize the dropdown
        articleLimitDropdown.setItems(FXCollections.observableArrayList(10, 25, 50, 100));
        articleLimitDropdown.setValue(DEFAULT_ARTICLE_LIMIT);

        categoryMap = new HashMap<>();

        // Fetch predefined categories
        List<Category> predefinedCategories = Category.getCategories();

        // Add predefined categories to the map
        predefinedCategories.forEach(category -> {
            switch (category.getName()) {
                case "Tech":
                    categoryMap.put(String.valueOf(category), techListView);
                    break;
                case "Entertainment":
                    categoryMap.put(String.valueOf(category), entertainmentListView);
                    break;
                case "Business":
                    categoryMap.put(String.valueOf(category), businessListView);
                    break;
                case "Sports":
                    categoryMap.put(String.valueOf(category), sportsListView);
                    break;
                case "Politics":
                    categoryMap.put(String.valueOf(category), politicsListView);
                    break;
                case "Travel":
                    categoryMap.put(String.valueOf(category), travelListView);
                    break;
                case "Food":
                    categoryMap.put(String.valueOf(category), foodListView);
                    break;
                case "Health":
                    categoryMap.put(String.valueOf(category), healthListView);
                    break;
            }
        });

        this.currentUser = User.getCurrentUser(); // Get the logged-in user

        // Add a listener to detect changes in the dropdown value
        articleLimitDropdown.setOnAction(event -> {
            int selectedLimit = articleLimitDropdown.getValue();
            loadArticles(selectedLimit); // Load articles based on the selected limit
        });

        // Initialize the news with the default article limit
        loadArticles(DEFAULT_ARTICLE_LIMIT);
    }

    // Load articles based on the selected limit
    private void loadArticles(int articleLimit) {
        if (currentUser == null) {
            System.out.println("No user is logged in.");
            return;
        }

        System.out.println("Loading articles for: " + currentUser.getName() + " with ID: " + currentUser.getId());

        // Fetch all articles
        List<Article> articles = fetchNewsFromDatabase();
        if (articles == null || articles.isEmpty()) {
            System.out.println("No articles found in the database.");
            showAlert(Alert.AlertType.ERROR, "Error", "No articles found in the database.");
            return;
        }

        try {
            // Shuffle the articles randomly
            Collections.shuffle(articles);

            // Truncate the shuffled list to the selected limit
            List<Article> randomArticles = articles.stream()
                    .limit(articleLimit)
                    .collect(Collectors.toList());

            // Clear the existing items in the ListView before adding new ones
            newsListView.getItems().clear();

            // Populate the ListView with articles
            Map<String, List<Integer>> userPreferences = DatabaseHandler.getInstance().fetchAllUserPreferences(currentUser.getId());


            // Log the user's preferences
            logPreferences(userPreferences);

            // Populate articles for general tab
            populateArticles(randomArticles, userPreferences);

            // Populate category-specific news
            populateCategoryNews(randomArticles);

        } catch (SQLException e) {
            System.out.println("Error fetching data from the database.");
            e.printStackTrace();
        }
    }


    // Populate the ListView with articles and update preferences
    private void populateArticles(List<Article> articles, Map<String, List<Integer>> preferences) throws SQLException {
        if (newsListView != null) {
            for (Article article : articles) {
                // Use the Article class to update preferences
                Article.updateUserPreferences(preferences, currentUser, article);

                // Create the news item UI element and add it to the ListView
                HBox newsItem = createNewsItem(article);
                newsListView.getItems().add(newsItem);

                // Store the UI element in the global map
                articleUIMap.computeIfAbsent(article.getId(), id -> new ArrayList<>()).add(newsItem);

            }
        } else {
            System.out.println("Error: newsListView is not initialized.");
        }
    }

    // Populate articles for each category in categoryMap
    private void populateCategoryNews(List<Article> randomArticles) {
        if (categoryMap != null && !categoryMap.isEmpty()) {
            // Clear existing items from all category ListViews before populating new ones
            categoryMap.values().forEach(listView -> listView.getItems().clear());


            // Group the randomly selected articles by their categories
            Map<String, List<Article>> categorizedArticles = randomArticles.stream()
                    .collect(Collectors.groupingBy(Article::getCategory));

            categoryMap.forEach((category, listView) -> {
                // Get the articles for the current category from the grouped map
                List<Article> articlesByCategory = categorizedArticles.getOrDefault(category, Collections.emptyList());

                for (Article article : articlesByCategory) {
                    // Create the news item UI element and add it to the ListView
                    HBox newsItem = null;
                    try {
                        newsItem = createNewsItem(article);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    listView.getItems().add(newsItem);

                }
            });
        } else {
            System.out.println("Error: categoryMap is not initialized or is empty.");
        }
    }

    // Create a news item UI element
    private HBox createNewsItem(Article article) throws SQLException {
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
            //System.out.println("Error loading image for URL: " + article.getImageUrl() + " - " + exception.getMessage());
            imageView.setImage(new Image(getClass().getResource("/Images/news.jpeg").toExternalForm()));
        });


        // Run the task in the ExecutorService
        NewsApplication.executorService.submit(loadImageTask);


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
            User.initializeUserPreferences();
            UserPreferences preferences = currentUser.getPreferences();
            //System.out.println(preferences);
            updateArticleUI(article);


            // Set initial button labels and styles
            if (preferences.isLiked(article)) {
                likeButton.setText("Unlike");
                likeButton.setStyle("-fx-background-color: #2196f3; -fx-max-width: 125;");
            }
            else if (preferences.isDisliked(article)) {
                dislikeButton.setText("Remove Dislike");
                dislikeButton.setStyle("-fx-background-color: #f44336;-fx-max-width: 125;");
            }
            if (preferences.isRead(article)) {
                readButton.setText("Read Again ✅");
                readButton.setStyle("-fx-background-color: #59ea59;"); // Optional style for "read" state
            }

            // Like button action
            likeButton.setOnAction(event -> {
                if (preferences.isLiked(article)) {
                    preferences.removeLikedArticle(article, currentUser.getId());
                    likeButton.setText("👍 Like");
                    likeButton.setStyle(""); // Reset to no color
                } else {
                    preferences.addLikedArticle(article, currentUser.getId());
                    likeButton.setText("Unlike");
                    likeButton.setStyle("-fx-background-color: #2196f3; -fx-max-width: 125; -fx-border-color: #031452"); // Blue for liked
                    dislikeButton.setText("👎 Dislike");
                    dislikeButton.setStyle(""); // Reset dislike button
                }
            });

            // Dislike button action
            dislikeButton.setOnAction(event -> {
                if (preferences.isDisliked(article)) {
                    preferences.removeDislikedArticle(article, currentUser.getId());
                    dislikeButton.setText("👎 Dislike");
                    dislikeButton.setStyle(""); // Reset to no color
                } else {
                    preferences.addDislikedArticle(article, currentUser.getId());
                    dislikeButton.setText("Remove Dislike");
                    dislikeButton.setStyle("-fx-background-color: #f44336; -fx-max-width: 125; -fx-border-color: #500202"); // Red for disliked
                    likeButton.setText("👍 Like");
                    likeButton.setStyle(""); // Reset like button
                }
            });

            // Read button action
            readButton.setOnAction(event -> {
                ReadArticleAction(article);
                preferences.addReadArticle(article, currentUser.getId());
                readButton.setText("Read Again ✅");
                readButton.setStyle("-fx-background-color: #59ea59; -fx-max-width: 125; -fx-border-color: #02460d"); // Update to show article was read

            });

        }
        // Set button styles
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

    // Update the UI based on user preferences
    private void updateArticleUI(Article article) {
        if (articleUIMap.containsKey(article.getId())) {
            List<HBox> uiElements = articleUIMap.get(article.getId());
            for (HBox newsItem : uiElements) {
                // Find the buttons in the newsItem and update their styles/text
                for (Node node : newsItem.getChildren()) {
                    if (node instanceof VBox) {
                        VBox buttonContainer = (VBox) node;
                        for (Node button : buttonContainer.getChildren()) {
                            if (button instanceof Button) {
                                Button btn = (Button) button;
                                updateButtonState(btn, article);
                            }
                        }
                    }
                }
            }
        }
    }

    // Update the button state based on user preferences
    private void updateButtonState(Button button, Article article) {
        UserPreferences preferences = currentUser.getPreferences();

        if ("👍 Like".equals(button.getText()) || "Unlike".equals(button.getText())) {
            if (preferences.isLiked(article)) {
                button.setText("Unlike");
                button.setStyle("-fx-background-color: #2196f3; -fx-max-width: 125; -fx-border-color: #031452");
            } else {
                button.setText("👍 Like");
                button.setStyle("");
            }
        } else if ("👎 Dislike".equals(button.getText()) || "Remove Dislike".equals(button.getText())) {
            if (preferences.isDisliked(article)) {
                button.setText("Remove Dislike");
                button.setStyle("-fx-background-color: #f44336; -fx-max-width: 125; -fx-border-color: #500202");
            } else {
                button.setText("👎 Dislike");
                button.setStyle("");
            }
        } else if ("📰 Read Article".equals(button.getText()) || "Read Again ✅".equals(button.getText())) {
            if (preferences.isRead(article)) {
                button.setText("Read Again ✅");
                button.setStyle("-fx-background-color: #59ea59; -fx-max-width: 125; -fx-border-color: #02460d");
            } else {
                button.setText("📰 Read Article");
                button.setStyle("");
            }
        }
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

    @FXML
    public void ReadArticleAction(Article article) {
        // Get the article URL
        String articleUrl = article.getUrl(); // Replace with the correct method to get the article URL
        System.out.println("Article URL: " + articleUrl);


        if (articleUrl != null && !articleUrl.isEmpty()) {
            try {
                // Load the FXML file
                FXMLLoader loader = new FXMLLoader(User.class.getResource("/Design_Files/article_display.fxml"));

                // Create a new scene from the FXML
                Parent root = loader.load();

                // Find the WebView node directly from the FXML root
                WebView webView = (WebView) root.lookup("#webView");

                if (webView != null) {
                    // Load the article URL into the WebView
                    webView.getEngine().load(articleUrl);
                } else {
                    System.out.println("WebView node not found in FXML.");
                }

                // Set up the stage and display it
                Stage stage = new Stage();
                stage.setTitle("Read Article");
                stage.setScene(new Scene(root));
                stage.show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid article URL.");
            System.out.println("Invalid article URL.");
        }
    }

    @FXML
    public void handleUserIconClick() {
        try {
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Design_Files/user_preferences.fxml"));
            Parent root = loader.load();

            // Create a new stage for the User Preferences window
            Stage userPreferencesStage = new Stage();
            userPreferencesStage.setTitle("User Logs");
            userPreferencesStage.setScene(new Scene(root, 760, 620));
            userPreferencesStage.setResizable(false);
            userPreferencesStage.show();
        } catch (IOException e) {
            e.printStackTrace(); // Log any issues loading the FXML file
        }
    }

    @FXML
    public void ForYouButtonClick(ActionEvent actionEvent) {
        try {
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Design_Files/For_you.fxml"));
            Parent root = loader.load();

            // Create a new stage for the User Preferences window
            Stage ForYouStage = new Stage();
            ForYouStage.setTitle("For You");
            ForYouStage.setScene(new Scene(root, 1150, 650));
            ForYouStage.setResizable(false);
            ForYouStage.show();

            Stage previousStage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            previousStage.close();
        } catch (IOException e) {
            e.printStackTrace(); // Log any issues loading the FXML file
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // Call the method to navigate to the login page
        Login_SignupController loginSignupController = new Login_SignupController();
        loginSignupController.backToLogin(event);
    }
}

