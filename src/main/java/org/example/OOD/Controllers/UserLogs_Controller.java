package org.example.OOD.Controllers;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import org.example.OOD.Database_Handler.DatabaseHandler;
import org.example.OOD.Models.Article;
import org.example.OOD.Models.User;
import org.example.OOD.Models.UserPreferences;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import static org.example.OOD.Configurations.Alerts.showAlert;

public class UserLogs_Controller {

    @FXML
    private ListView<HBox> readArticlesListView;

    @FXML
    private ListView<HBox> likedArticlesListView;

    @FXML
    private ListView<HBox> dislikedArticlesListView;

    private UserPreferences userPreferences = new UserPreferences();


    public void initialize() {
        loadUserPreferences();

        // Add click listeners to each ListView
        readArticlesListView.setOnMouseClicked(event -> {
            try {
                handleArticleClick(readArticlesListView);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        likedArticlesListView.setOnMouseClicked(event -> {
            try {
                handleArticleClick(likedArticlesListView);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        dislikedArticlesListView.setOnMouseClicked(event -> {
            try {
                handleArticleClick(dislikedArticlesListView);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void loadUserPreferences() {
        try {
            // Fetch all user preferences from the database
            DatabaseHandler dbHandler = DatabaseHandler.getInstance();
            List<Map<String, Object>> likedArticles = dbHandler.fetchUserPreferencesWithTimestamp(User.getCurrentUser().getId(), "liked");
            List<Map<String, Object>> dislikedArticles = dbHandler.fetchUserPreferencesWithTimestamp(User.getCurrentUser().getId(), "disliked");
            List<Map<String, Object>> readArticles = dbHandler.fetchUserPreferencesWithTimestamp(User.getCurrentUser().getId(), "read");

            // Convert article IDs to Article objects and update UserPreferences
            likedArticles.forEach(data -> addArticleToList(likedArticlesListView, data, "liked"));
            dislikedArticles.forEach(data -> addArticleToList(dislikedArticlesListView, data, "disliked"));
            readArticles.forEach(data -> addArticleToList(readArticlesListView, data, "read"));

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR,"Error loading preferences", e.getMessage());
            e.printStackTrace();
        }
    }

    private void addArticleToList(ListView<HBox> listView, Map<String, Object> preferenceData, String preferenceType) {
        // Extract data from preferenceData
        int articleId = (int) preferenceData.get("articleId");
        Timestamp createdAt = (Timestamp) preferenceData.get("createdAt");

        // Format timestamp for a more user-friendly display
        String formattedDate = new SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a").format(createdAt);

        // Fetch all articles from the database
        //DatabaseHandler dbHandler = DatabaseHandler.getInstance();
        List<Article> articles = DatabaseHandler.fetchNewsFromDatabase();

        // Find the matching article by ID
        Article matchingArticle = articles.stream()
                .filter(article -> article.getId() == articleId)
                .findFirst()
                .orElse(null);

        // If article is found, create a visually appealing HBox to display it
        if (matchingArticle != null) {
            // Create a label for the article title and date
            Label articleLabel = new Label(String.format(
                    "📰 %s\n🗓️ Added on: %s",
                    matchingArticle.getTitle(),
                    formattedDate
            ));
            articleLabel.setWrapText(true); // Ensure text wraps if too long
            articleLabel.getStyleClass().add("article-label"); // Add styling via CSS

            // Create a "Remove" button
            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().add("remove-button");
            removeButton.setOnAction(event -> removePreference(matchingArticle, preferenceType, listView));

            // Add both the label and button to an HBox
            HBox articleRow = new HBox(10, articleLabel, removeButton);
            articleRow.setAlignment(Pos.CENTER_LEFT);
            articleRow.getStyleClass().add("article-row"); // Add styling via CSS

            // Add the HBox to the ListView
            listView.getItems().add(articleRow);

            // Update user preferences
            updateUserPreferences(preferenceType, matchingArticle);
        } else {
            showAlert(Alert.AlertType.WARNING,"Article Not Found", "No article found for ID: " + articleId);
        }
    }

    private void updateUserPreferences(String preferenceType, Article article) {
        switch (preferenceType.toLowerCase()) {
            case "liked":
                userPreferences.addLikedArticle(article, User.getCurrentUser().getId());
                break;
            case "disliked":
                userPreferences.addDislikedArticle(article, User.getCurrentUser().getId());
                break;
            case "read":
                userPreferences.addReadArticle(article, User.getCurrentUser().getId());
                break;
        }
    }

    private void handleArticleClick(ListView<HBox> listView) throws SQLException {
        // Get the selected item (HBox) from the ListView
        HBox selectedItem = listView.getSelectionModel().getSelectedItem();
        System.out.println("Clicked item: " + selectedItem);

        if (selectedItem != null) {
            // Extract the article title from the Label inside the HBox
            String title = extractTitle(selectedItem); // Assuming the first child is the Label
            System.out.println("Extracted title: " + title);

            // Find the article based on the title
            Article article = getArticleByTitle(title);
            System.out.println(article);
            if (article != null) {
                System.out.println("Found article: " + article.getTitle());
                NewsController newsController = new NewsController();
                newsController.ReadArticleAction(article);  // Handles the reading action
            } else {
                System.out.println("Article not found for title: " + title);
            }
        }
    }

    private Article getArticleByTitle(String title)  {
        // Ensure the title is not null or empty
        if (title == null || title.isEmpty()) {
            return null;
        }

        // Retrieve all articles from the database
        List<Article> articles = DatabaseHandler.fetchNewsFromDatabase(); // Assuming this fetches all articles

        // Search for the article by title
        for (Article article : articles) {
            if (article.getTitle().equalsIgnoreCase(title)) { // Compare title (case-insensitive)
                return article; // Return the article if title matches
            }
        }

        // Return null if no matching article was found
        return null;
    }

    private String extractTitle(HBox listViewItem) {
        // Assuming the first child of HBox is the Label with the article title
        Label articleLabel = (Label) listViewItem.getChildren().get(0); // Get the first child (Label)

        // Get the text from the Label
        String labelText = articleLabel.getText();

        // Find the index of the emoji "🗓️" to isolate the title
        int dateIndex = labelText.indexOf("🗓️");

        // Extract the title before the "🗓️" emoji and trim leading/trailing spaces
        String extractedTitle = dateIndex > 0
                ? labelText.substring(0, dateIndex).trim()
                : labelText.trim();

        // Remove any leading emoji "📰" or unnecessary spaces
        extractedTitle = extractedTitle.replace("📰", "").trim();

        System.out.println("Extracted title: " + extractedTitle);
        return extractedTitle;
    }


    private void removePreference(Article article, String preferenceType, ListView<HBox> listView) {
        try {
            // Get the user ID (assuming it’s stored globally or passed into this class)
            String userId = User.getCurrentUser().getId();

            // Call the removeUserPreference method
            DatabaseHandler dbHandler = DatabaseHandler.getInstance();
            dbHandler.removeUserPreference(userId, article.getId(), preferenceType);

            // Update the ListView by removing the corresponding HBox
            listView.getItems().removeIf(item -> {
                Label articleLabel = (Label) item.getChildren().get(0); // Assuming first child is the Label
                return articleLabel.getText().contains(article.getTitle());
            });

            showAlert(Alert.AlertType.INFORMATION,"Preference Removed", "The article preference has been successfully removed.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR,"Error Removing Preference", e.getMessage());
            e.printStackTrace();
        }
    }
}
