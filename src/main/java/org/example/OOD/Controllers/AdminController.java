package org.example.OOD.Controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.example.OOD.Database_Handler.DatabaseHandler;
import org.example.OOD.Models.Article;
import org.example.OOD.Models.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

import static org.example.OOD.Configurations.Alerts.showAlert;

public class AdminController {
    public TableView userTableView;
    public TableColumn userIdColumn;
    public TableColumn userNameColumn;
    public TableColumn userEmailColumn;
    public TableColumn userPreferencesColumn;
    public TableView articleTableView;
    public TableColumn articleIdColumn;
    public TableColumn articleCategoryColumn;
    public TableColumn articleTitleColumn;
    public TableColumn articleAuthorColumn;
    public Button deleteUserButton;
    public Button deleteArticleButton;

    DatabaseHandler databaseHandler;

    public AdminController() throws SQLException {
        databaseHandler = DatabaseHandler.getInstance();
    }

    public void initialize() throws SQLException {
        populateUsers();
        populateArticles();
    }

    public void populateUsers() throws SQLException {
        // Set up columns to match User class fields
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        userNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        userEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Populate TableView with users from the database
        ObservableList<User> users = FXCollections.observableArrayList(DatabaseHandler.getInstance().getUsers());
        userTableView.setItems(users);
    }

    public void populateArticles() {
        // Set up other columns
        articleIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        articleTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        articleCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        articleAuthorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));

        // Set user preferences column
        userPreferencesColumn.setCellValueFactory(cellData -> {
            // Explicitly cast to the expected type
            Article article = (Article) ((TableColumn.CellDataFeatures) cellData).getValue();

            String preferences = "Likes: " + article.getLikes() + " | " +
                    ", Dislikes: " + article.getDislikes() + " | " +
                    ", Readers: " + article.getReaders();

            return new SimpleStringProperty(preferences);
        });

        // Convert List<Article> to ObservableList<Article>
        ObservableList<Article> articleObservableList = FXCollections.observableArrayList(DatabaseHandler.fetchNewsFromDatabase());

        // Populate data
        articleTableView.setItems(articleObservableList);
    }

    // Method to handle deleting a user
    @FXML
    private void handleDeleteUser() {
        // Get the selected user from the TableView
        User selectedUser = (User) userTableView.getSelectionModel().getSelectedItem();

        if (selectedUser != null) {
            // Confirm deletion
            Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmationAlert.setTitle("Confirm Deletion");
            confirmationAlert.setHeaderText("Delete User");
            confirmationAlert.setContentText("Are you sure you want to delete user " + selectedUser.getName() + "?");
            Optional<ButtonType> result = confirmationAlert.showAndWait();
            //showAlert(Alert.AlertType.CONFIRMATION,"Confirm Deletion", "Are you sure you want to delete user " + selectedUser.getName() + "?");

            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Remove from TableView
                userTableView.getItems().remove(selectedUser);

                // Delete from Database
                try {
                    DatabaseHandler dbHandler = new DatabaseHandler();
                    dbHandler.deleteUserAndPreferences(selectedUser.getId());
                    showAlert(Alert.AlertType.INFORMATION, "User Deleted", "User " + selectedUser.getName() + " was successfully deleted.");
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Deletion Error", "An error occurred while deleting the user from the database.");
                }
            }
        } else {
            // No user selected
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a user to delete.");
        }
    }

    @FXML
    private void handleDeleteArticle() {
        // Get selected article from TableView
        Article selectedArticle = (Article) articleTableView.getSelectionModel().getSelectedItem();

        if (selectedArticle != null) {
            // Confirm deletion
            Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmationAlert.setTitle("Confirm Deletion");
            confirmationAlert.setHeaderText("Delete Article");
            confirmationAlert.setContentText("Are you sure you want to delete article " + selectedArticle.getTitle() + "?");
            Optional<ButtonType> result = confirmationAlert.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {

                try {
                    // Delete article and its preferences from the database
                    DatabaseHandler dbHandler = DatabaseHandler.getInstance();
                    dbHandler.deleteArticleAndPreferences(selectedArticle.getId());

                    // Remove article from the TableView
                    articleTableView.getItems().remove(selectedArticle);

                    System.out.println("Article deleted successfully.");
                } catch (SQLException e) {
                    e.printStackTrace();
                    System.out.println("Error deleting article.");
                }
            } else {
                System.out.println("No article selected.");
            }

        } else {
            System.out.println("No article selected.");
        }
    }

    /**
     * Refreshes the articleTableView with the latest articles from the database.
     */
    public void refreshTableView() {
        // Fetch the latest articles from the database
        ObservableList<Article> updatedArticles = FXCollections.observableArrayList(DatabaseHandler.fetchNewsFromDatabase());

        // Set the updated articles in the TableView
        articleTableView.setItems(updatedArticles);

        System.out.println("TableView refreshed successfully.");
    }

    @FXML
    public void AddButtonClick(ActionEvent actionEvent) {
        try {
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Design_Files/AddArticle.fxml"));
            Parent root = loader.load();

            // Get the controller instance of AddArticleController
            AddArticleController addArticleController = loader.getController();

            // Pass the current AdminController instance to AddArticleController
            addArticleController.setAdminController(this);

            // Create a new stage for the Add Article window
            Stage addArticleStage = new Stage();
            addArticleStage.setTitle("Add Article");
            addArticleStage.setScene(new Scene(root, 600, 850));
            addArticleStage.setResizable(false);
            addArticleStage.show();

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
