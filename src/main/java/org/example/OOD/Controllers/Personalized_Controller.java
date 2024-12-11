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
import javafx.stage.Stage;
import org.example.OOD.Models.Article;
import org.example.OOD.Models.User;
import org.example.OOD.Models.UserPreferences;
import org.example.OOD.Recommendation_Engine.Personalize;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class Personalized_Controller {
    @FXML
    private ComboBox<Integer> articleLimitDropdown;

    @FXML
    private ListView<HBox> personalizedListView; // The main ListView for personalized news

    private User currentUser; // Current logged-in user

    private static final int DEFAULT_ARTICLE_LIMIT = 5;

    /**
     * Initialize the controller.
     */

    public void initialize() {

        // Initialize the dropdown
        articleLimitDropdown.setItems(FXCollections.observableArrayList(5, 10, 20, 30));
        articleLimitDropdown.setValue(DEFAULT_ARTICLE_LIMIT);

        // Get the current user
        currentUser = User.getCurrentUser();
        System.out.println(currentUser.getPreferences());

        if (currentUser == null) {
            System.out.println("No user is logged in.");
            return;
        }

        System.out.println("Loading personalized news for: " + currentUser.getName() + " (ID: " + currentUser.getId() + ")");

        // Create an instance of the Personalize class
        Personalize personalize = new Personalize();

        try {
            // Initialize user preferences
            User.initializeUserPreferences();

            // Add a listener to detect changes in the dropdown value
            articleLimitDropdown.setOnAction(event -> {
                int selectedLimit = articleLimitDropdown.getValue();
                // Fetch and handle recommendations
                List<Article> recommendations = null;
                try {
                    recommendations = personalize.fetchAndHandleRecommendations(currentUser.getId(), selectedLimit);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                // Clear the existing items in the ListView before adding new ones
                personalizedListView.getItems().clear();

                // Populate the personalized articles in the ListView
                populatePersonalizedArticles(recommendations, currentUser.getPreferences());
            });

            // Fetch and handle recommendations
            List<Article> recommendations = personalize.fetchAndHandleRecommendations(currentUser.getId(), DEFAULT_ARTICLE_LIMIT);

            // Populate the personalized articles in the ListView
            populatePersonalizedArticles(recommendations, currentUser.getPreferences());

        } catch (
                SQLException e) {
            System.out.println("Error fetching recommendations: " + e.getMessage());
        }
    }

    /**
     * Populate the personalized ListView with articles.
     */
    private void populatePersonalizedArticles(List<Article> articles, UserPreferences preferences) {
        if (personalizedListView == null) {
            System.out.println("Error: personalizedListView is not initialized.");
            return;
        }

        for (Article article : articles) {
            HBox newsItem = createPersonalizedNewsItem(article, preferences);
            personalizedListView.getItems().add(newsItem);

            updatePersonalizedUI();
        }
    }

    /**
     * Create an HBox representing a personalized news item.
     */
    private HBox createPersonalizedNewsItem(Article article, UserPreferences preferences) {
        HBox cellContainer = new HBox(10);
        cellContainer.setPadding(new Insets(10));

        HBox hBox = new HBox(10);
        hBox.setPadding(new Insets(10));
        hBox.setMaxWidth(870);
        hBox.getStyleClass().add("hbox-article");

        // Dynamically bind width to ListView's width
        hBox.prefWidthProperty().bind(personalizedListView.widthProperty().subtract(20));

        // Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(150);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);

        // Load article image asynchronously
        loadImageAsync(article, imageView);

        // Text
        VBox textContainer = new VBox(5);
        Label titleLabel = new Label(article.getTitle());
        titleLabel.getStyleClass().add("label-title");
        titleLabel.setWrapText(true);

        Label descriptionLabel = new Label(article.getDescription());
        descriptionLabel.getStyleClass().add("label-description");
        descriptionLabel.setWrapText(true);

        Label sourceLabel = new Label("Source: " + article.getSource_name() + " | " + article.getPublishedDate());
        sourceLabel.getStyleClass().add("label-source");

        textContainer.getChildren().addAll(titleLabel, descriptionLabel, sourceLabel);

        // Buttons
        Button likeButton = new Button(preferences.isLiked(article) ? "Unlike" : "👍 Like");
        Button dislikeButton = new Button(preferences.isDisliked(article) ? "Remove Dislike" : "👎 Dislike");
        Button readButton = new Button(preferences.isRead(article) ? "Read Again ✅" : "📰 Read Article");

        setupButtonActions(likeButton, dislikeButton, readButton, article, preferences);

        VBox buttonContainer = new VBox(5, readButton, likeButton, dislikeButton);
        buttonContainer.setAlignment(Pos.CENTER);

        // Add elements to HBox
        hBox.getChildren().addAll(imageView, textContainer);
        cellContainer.getChildren().addAll(hBox, buttonContainer);

        return cellContainer;
    }

    /**
     * Load the article image asynchronously.
     */
    private void loadImageAsync(Article article, ImageView imageView) {
        Task<Image> imageTask = new Task<>() {
            @Override
            protected Image call() {
                try {
                    return new Image(article.getImageUrl(), true);
                } catch (Exception e) {
                    return new Image(getClass().getResource("/Images/news.jpeg").toExternalForm());
                }
            }
        };

        imageTask.setOnSucceeded(event -> imageView.setImage(imageTask.getValue()));
        imageTask.setOnFailed(event -> imageView.setImage(new Image(getClass().getResource("/Images/news.jpeg").toExternalForm())));

        new Thread(imageTask).start();
    }

    /**
     * Setup actions for Like, Dislike, and Read buttons.
     */
    private void setupButtonActions(Button likeButton, Button dislikeButton, Button readButton, Article article, UserPreferences preferences) {

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

        readButton.setOnAction(event -> {
            NewsController newsController = new NewsController();
            newsController.ReadArticleAction(article);  // Handles the reading action
            preferences.addReadArticle(article, currentUser.getId());
            readButton.setText("Read Again ✅");
            readButton.setStyle("-fx-background-color: #59ea59; -fx-max-width: 125; -fx-border-color: #02460d"); // Update to show article was read

        });

        readButton.getStyleClass().add("read-button");
        likeButton.getStyleClass().add("like-button");
        dislikeButton.getStyleClass().add("dislike-button");
    }

    /**
     * Update the UI based on the current user's preferences.
     */
    private void updatePersonalizedUI() {
        if (personalizedListView == null || currentUser == null) {
            System.out.println("Error: personalizedListView or currentUser is not initialized.");
            return;
        }

        UserPreferences preferences = currentUser.getPreferences();

        for (HBox item : personalizedListView.getItems()) {
            if (item.getChildren().size() < 2) continue;

            VBox buttonContainer = (VBox) item.getChildren().get(1);

            if (buttonContainer.getChildren().size() < 3) continue;

            Button readButton = (Button) buttonContainer.getChildren().get(0);
            Button likeButton = (Button) buttonContainer.getChildren().get(1);
            Button dislikeButton = (Button) buttonContainer.getChildren().get(2);

            HBox hBox = (HBox) item.getChildren().get(0);
            Label titleLabel = (Label) ((VBox) hBox.getChildren().get(1)).getChildren().get(0);
            String articleTitle = titleLabel.getText();

            // Find the article using the title
            Article article = Article.findArticleByTitle(articleTitle);

            if (article == null) continue;

            // Update button styles based on preferences
            if (preferences.getLikedArticles().contains(article)) {
                likeButton.setText("Unlike");
                likeButton.setStyle("-fx-background-color: #2196f3; -fx-max-width: 125; -fx-border-color: #031452");
                dislikeButton.setText("👎 Dislike");
                dislikeButton.setStyle("");
            } else {
                likeButton.setText("👍 Like");
                likeButton.setStyle("");
            }

            if (preferences.getDislikedArticles().contains(article)) {
                dislikeButton.setText("Remove Dislike");
                dislikeButton.setStyle("-fx-background-color: #f44336; -fx-max-width: 125; -fx-border-color: #500202");
                likeButton.setText("👍 Like");
                likeButton.setStyle("");
            } else {
                dislikeButton.setText("👎 Dislike");
                dislikeButton.setStyle("");
            }

            if (preferences.getReadArticles().contains(article)) {
                readButton.setText("Read Again ✅");
                readButton.setStyle("-fx-background-color: #59ea59; -fx-max-width: 125; -fx-border-color: #02460d");
            } else {
                readButton.setText("📰 Read Article");
                readButton.setStyle("");
            }
        }
    }

    @FXML
    public void BackButtonClick(ActionEvent actionEvent) {
        try {
            // Load the FXML file for NewsDisplay.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Design_Files/NewsDisplay.fxml"));
            Parent root = loader.load();

            // Get the NewsController instance
            NewsController newsController = loader.getController();

            // Pass the user-specific state to the controller
            newsController.initializeNews();

            // Create a new stage for the News Display
            Stage newsStage = new Stage();
            newsStage.setTitle("News Articles");
            newsStage.setScene(new Scene(root, 1110, 650));
            newsStage.setResizable(true);
            newsStage.show();

            // Close the current window
            Stage currentStage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Failed to load news feed!");
            errorAlert.showAndWait();
        }
    }
}
