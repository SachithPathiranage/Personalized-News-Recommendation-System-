package org.example.OOD.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.OOD.Database_Handler.DatabaseHandler;
import org.example.OOD.Models.Article;

import java.sql.SQLException;

public class AddArticleController {

    @FXML
    private TextField titleField;
    @FXML
    private TextField descriptionField;
    @FXML
    private TextArea contentArea;
    @FXML
    private TextField urlField;
    @FXML
    private TextField sourceNameField;
    @FXML
    private TextField authorField;
    @FXML
    private TextField imageUrlField;
    @FXML
    private ComboBox<String> categoryDropdown;

    private DatabaseHandler databaseHandler;
    private AdminController adminController;

    private static final String[] VALID_CATEGORIES = {"Food", "Politics", "Entertainment", "Health", "Sports", "Tech", "Travel", "Business"};

    public AddArticleController() throws SQLException {
        this.databaseHandler = new DatabaseHandler(); // Initialize DatabaseHandler
    }

    public void setAdminController(AdminController adminController) {
        this.adminController = adminController;
    }

    public Article createArticleFromInput() {
        try {
            String title = titleField.getText().trim();
            String description = descriptionField.getText().trim();
            String content = contentArea.getText().trim();
            String url = urlField.getText().trim();
            String sourceName = sourceNameField.getText().trim();
            String author = authorField.getText().trim();
            String imageUrl = imageUrlField.getText().trim();
            String category = categoryDropdown.getValue();

            // Validation

            if (!Article.isNonEmptyString(title)) {
                showAlert(Alert.AlertType.WARNING,"Validation Error", "Title cannot be empty.");
                return null;
            }
            if (!Article.isNonEmptyString(description)) {
                showAlert(Alert.AlertType.WARNING,"Validation Error", "Description cannot be empty.");
                return null;
            }
            if (!Article.isNonEmptyString(content)) {
                showAlert(Alert.AlertType.WARNING,"Validation Error", "Content cannot be empty.");
                return null;
            }
            if (!Article.isValidURL(url)) {
                showAlert(Alert.AlertType.ERROR,"Validation Error", "Invalid URL format.");
                return null;
            }
            if (!Article.isNonEmptyString(sourceName)) {
                showAlert(Alert.AlertType.WARNING,"Validation Error", "Source Name cannot be empty.");
                return null;
            }
            if (!Article.isNonEmptyString(author)) {
                showAlert(Alert.AlertType.WARNING,"Validation Error", "Author cannot be empty.");
                return null;
            }
            if (!Article.isValidImageURL(imageUrl)) {
                showAlert(Alert.AlertType.ERROR,"Validation Error", "Invalid Image URL format.");
                return null;
            }
            if (!Article.isValidCategory(category, VALID_CATEGORIES)) {
                showAlert(Alert.AlertType.WARNING,"Validation Error", "Category not selected.");
                return null;
            }

            // If validation passes, create the Article object
            String publishedDate = java.time.LocalDateTime.now().toString();
            return new Article(0, title, description, url, sourceName, author, imageUrl, publishedDate, category, content, 0, 0, 0);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR,"Error", "An error occurred: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    private void saveArticle() {
        Article article = createArticleFromInput();
        if (article != null) {
            boolean success = databaseHandler.saveArticle(article);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Article saved successfully!");
                clearForm();

                // Refresh TableView in AdminController
                if (adminController != null) {
                    adminController.refreshTableView();
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to save the article.");
            }
        }
    }

    @FXML
    private void clearForm() {
        titleField.clear();
        descriptionField.clear();
        contentArea.clear();
        urlField.clear();
        sourceNameField.clear();
        authorField.clear();
        imageUrlField.clear();
        categoryDropdown.setValue(null);
    }

    private void showAlert(Alert.AlertType alertType ,String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
