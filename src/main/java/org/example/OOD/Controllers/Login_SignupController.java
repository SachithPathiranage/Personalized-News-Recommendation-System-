package org.example.OOD.Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.OOD.Models.Admin;
import org.example.OOD.Models.User;
import java.sql.SQLException;


public class Login_SignupController {
    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField_signup;

    @FXML
    private PasswordField passwordField_signup;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private void handleLogin_User(ActionEvent event) throws SQLException {
        String email = emailField.getText();
        String password = passwordField.getText();

        // Delegate login handling to the User class
        User currentUser = User.handleLogin(email, password);

        if (currentUser != null) {
            Alert alert = new Alert(AlertType.INFORMATION, "Login successful!");
            alert.showAndWait();

            try {
                // Load the NewsDisplay.fxml file
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Design_Files/NewsDisplay.fxml"));
                Parent root = loader.load();

                // Get the NewsController instance and initialize
                NewsController newsController = loader.getController();
                newsController.initializeNews();

                // Create a new stage for the News page
                Stage newsStage = new Stage();
                newsStage.setTitle("News Articles");
                newsStage.setScene(new Scene(root, 1125, 650));
                newsStage.setResizable(false);
                newsStage.show();

                // Close the login window
                Stage loginStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                loginStage.close();

            } catch (Exception e) {
                e.printStackTrace();
                Alert errorAlert = new Alert(AlertType.ERROR, "Failed to load news articles!");
                errorAlert.showAndWait();
            }
        } else {
            Alert alert = new Alert(AlertType.ERROR, "Invalid email or password!");
            alert.showAndWait();
        }
    }

    @FXML
    private void openSignUp() {
        try {
            // Load the Sign-Up page FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Design_Files/Signup.fxml"));
            Parent root = loader.load();

            // Get the current stage (window)
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 650, 455));
            stage.setTitle("Sign Up");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void adminLog() {
        try {
            // Load the Sign-Up page FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Design_Files/AdminLogin.fxml"));
            Parent root = loader.load();

            // Get the current stage (window)
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 650, 455));
            stage.setTitle("Admin Login");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        String name = nameField.getText();
        String email = emailField_signup.getText();
        String password = passwordField_signup.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (User.handleSignUp(name, email, password, confirmPassword)) {
            backToLogin(event);
        }
    }

    @FXML
    void backToLogin(ActionEvent actionEvent) {
        // Implement navigation back to the login page
        try {
            // Load the Login page FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Design_Files/login.fxml"));
            Parent root = loader.load();

            // Get the current stage (window)
            //Stage stage = (Stage) emailField_signup.getScene().getWindow();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("User Login");
            stage.show();

            Stage previousStage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            previousStage.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private TextField emailField_admin;

    @FXML
    private PasswordField passwordField_admin;

    @FXML
    private void handleLogin_admin() {
        String email = emailField_admin.getText();
        String password = passwordField_admin.getText();

        // Attempt to authenticate the admin and retrieve their name
        Admin admin = Admin.handleLogin(email, password);

        if (admin != null) {
            String name = admin.getName(); // Retrieve the name from the authenticated Admin object

            Alert alert = new Alert(AlertType.INFORMATION, "Login successful! Welcome, " + name + "!");
            alert.showAndWait();

            try {
                // Load the Admin dashboard or main page (e.g., AdminDashboard.fxml)
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Design_Files/AdminDashboard.fxml"));
                Parent root = loader.load();

                // Get the AdminController instance and initialize
                AdminController adminController = loader.getController();
                adminController.initialize();

                // Create a new stage for the Admin dashboard
                Stage adminStage = new Stage();
                adminStage.setTitle("Admin Dashboard");
                adminStage.setScene(new Scene(root, 1025, 620));
                adminStage.setResizable(false);
                adminStage.show();

                // Close the admin login window
                Stage loginStage = (Stage) emailField_admin.getScene().getWindow();
                loginStage.close();

            } catch (Exception e) {
                e.printStackTrace();
                Alert errorAlert = new Alert(AlertType.ERROR, "Failed to load the Admin Dashboard!");
                errorAlert.showAndWait();
            }
        } else {
            Alert alert = new Alert(AlertType.ERROR, "Invalid email or password!");
            alert.showAndWait();
        }
    }
}


