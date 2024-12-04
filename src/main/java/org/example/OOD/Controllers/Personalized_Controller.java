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
import javafx.scene.web.WebView;
import org.example.OOD.Models.Article;
import org.example.OOD.Models.User;
import org.example.OOD.Models.UserPreferences;
import org.example.OOD.Recommendation_Engine.Personalize;

import java.sql.SQLException;
import java.util.List;

public class Personalized_Controller {
    @FXML
    private ListView<HBox> personalizedListView; // The main ListView for personalized news

    @FXML
    private WebView webView; // WebView for article details

    private User currentUser; // Current logged-in user

    /**
     * Initialize the controller.
     */

    public void initialize() {
        currentUser = User.getCurrentUser();

        if (currentUser == null) {
            System.out.println("No user is logged in.");
            return;
        }

        System.out.println("Loading personalized news for: " + currentUser.getName() + " (ID: " + currentUser.getId() + ")");

        // Create an instance of the Personalize class
        Personalize personalize = new Personalize();

        try {
            // Fetch the top N recommended articles (e.g., top 5)
            List<Article> recommendations = personalize.recommendArticles(currentUser.getId(), 5);

            if (recommendations.isEmpty()) {
                System.out.println("No recommendations available.");
            } else {
                // Display the recommended articles (or process further as needed)
                System.out.println("Top recommendations:");
                for (Article article : recommendations) {
                    System.out.println("- " + article.getTitle() + " (Similarity Score: " + article.getSimilarityScore() + ")");
                }
            }
            populatePersonalizedArticles(recommendations, currentUser.getPreferences());

        } catch (SQLException e) {
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
        likeButton.setOnAction(event -> {
            if (preferences.isLiked(article)) {
                preferences.removeLikedArticle(article, currentUser.getId());
                likeButton.setText("👍 Like");
            } else {
                preferences.addLikedArticle(article, currentUser.getId());
                likeButton.setText("Unlike");
            }
        });

        dislikeButton.setOnAction(event -> {
            if (preferences.isDisliked(article)) {
                preferences.removeDislikedArticle(article, currentUser.getId());
                dislikeButton.setText("👎 Dislike");
            } else {
                preferences.addDislikedArticle(article, currentUser.getId());
                dislikeButton.setText("Remove Dislike");
            }
        });

        readButton.setOnAction(event -> {
            preferences.addReadArticle(article, currentUser.getId());
            readButton.setText("Read Again ✅");
        });
    }
}
