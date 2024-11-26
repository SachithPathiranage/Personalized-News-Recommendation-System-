package org.example.ood;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.example.ood.DatabaseHandler.getConnection;


public class Login_Signup {
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
    private void handleLogin_User(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        // Attempt to authenticate the user and retrieve their name
        User currentUser = authenticateUserAndRetrieveDetails(email, password, "SELECT id, name FROM users WHERE email = ? AND password = ?");

        if (currentUser != null) {
            Alert alert = new Alert(AlertType.INFORMATION, "Login successful!");
            alert.showAndWait();

            try {
                // Set the logged-in user
                User.setCurrentUser(new User(currentUser.getId(), email, currentUser.getName())); // Set the user's email and name

                // Load the NewsDisplay.fxml file
                FXMLLoader loader = new FXMLLoader(getClass().getResource("NewsDisplay.fxml"));
                Parent root = loader.load();

                // Get the NewsController instance and initialize
                NewsController newsController = loader.getController();
                newsController.initializeNews();

                // Create a new stage for the News page
                Stage newsStage = new Stage();
                newsStage.setTitle("News Articles");
                newsStage.setScene(new Scene(root, 1040, 650));
                newsStage.setResizable(true);
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

    public User authenticateUserAndRetrieveDetails(String email, String password, String selectUserSQL) {
        try (Connection connection = getConnection()) {
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



    @FXML
    private void openSignUp() {
        try {
            // Load the Sign-Up page FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Signup.fxml"));
            Parent root = loader.load();

            // Get the current stage (window)
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root,650,455));
            stage.setTitle("Sign Up");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void adminLog() {
        try {
            // Load the Sign-Up page FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AdminLogin.fxml"));
            Parent root = loader.load();

            // Get the current stage (window)
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root,650,455));
            stage.setTitle("Admin Login");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSignUp(ActionEvent event) throws SQLException {
        String name = nameField.getText();
        String email = emailField_signup.getText();
        String password = passwordField_signup.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (validateSignup(email, password, confirmPassword)){
            // Generate a unique ID for the user
            String unique_code = UUID.randomUUID().toString();
            String ID = generateUserId();

            // Implement sign-up logic here (e.g., save the user details with the unique ID)
            // You can create a User object and save it in the database or a file
            User newUser = new User(ID, unique_code, name, email, password);
            saveUserToDatabase(newUser);

            Alert alert = new Alert(AlertType.INFORMATION, "Sign-up successful! Your ID is: " +  ID + "\n" + "(Verification Code: " + unique_code + ")");
            alert.showAndWait();

            backToLogin(event);
        }
    }

    private void saveUserToDatabase(User user) throws SQLException {
        String email = user.getEmail();
        String ID = user.getId();
        //String unique_code = user.getUnique_code();
        String name = user.getName();
        String password = user.getPassword();

        String insertUserSQL = "INSERT INTO users (id, name, email, password) VALUES (?, ?, ?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertUserSQL)) {

            preparedStatement.setString(1, String.valueOf(ID));
            //preparedStatement.setString(2, String.valueOf(unique_code));
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, email);
            preparedStatement.setString(4, password); // Consider hashing the password

            int rowsInserted = preparedStatement.executeUpdate();
            if (rowsInserted > 0) {
                Alert alert = new Alert(AlertType.INFORMATION, "Sign-up successful!");
                alert.showAndWait();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(AlertType.ERROR, "Sign-up failed! Please try again.");
            alert.showAndWait();
        }
    }

    public static String generateUserId() throws SQLException {
        String lastUserIdQuery = "SELECT id FROM users WHERE id LIKE 'U%' ORDER BY CAST(SUBSTRING(id, 2) AS UNSIGNED) DESC LIMIT 1";
        String newUserId = "U01"; // Default ID if no users exist

        try (PreparedStatement stmt = getConnection().prepareStatement(lastUserIdQuery);
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

    @FXML
    private void backToLogin(ActionEvent actionEvent) {
        // Implement navigation back to the login page
        try {
            // Load the Login page FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
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
    public static boolean validateSignup(String email, String password, String confirmPassword) {
        if (!isValidEmail(email)) {
            System.out.println("Invalid email format.");
            Alert alert = new Alert(AlertType.ERROR, "Invalid email format!");
            alert.showAndWait();
            return false;
        }

        if (isEmailRegistered(email)) {
            System.out.println("Email is already registered. Please use a different email.");
            Alert alert = new Alert(AlertType.ERROR, "Email is already registered. Please use a different email.");
            alert.showAndWait();
            return false;
        }

        if (!isValidPassword(password)) {
            System.out.println("Password must be at least 8 characters long.");
            Alert alert = new Alert(AlertType.ERROR, "Password must be at least 8 characters long.");
            alert.showAndWait();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            System.out.println("Passwords do not match.");
            Alert alert = new Alert(AlertType.ERROR, "Passwords do not match.");
            alert.showAndWait();
            return false;
        }
        return true;
    }

    public static boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    public static boolean isEmailRegistered(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try {
            DatabaseHandler.getInstance();
            try (Connection connection = getConnection();
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

//    public static boolean validateLogin(String email, String password) {
//        if (!isValidEmail(email)) {
//            System.out.println("Invalid email format.");
//            Alert alert = new Alert(AlertType.ERROR, "Invalid email format.");
//            alert.showAndWait();
//            return false;
//        }
//        if (password == null || password.isEmpty()) {
//            System.out.println("Password cannot be empty.");
//            Alert alert = new Alert(AlertType.ERROR, "Password cannot be empty.");
//            alert.showAndWait();
//            return false;
//        }
//        return true;
//    }

    @FXML
    private TextField emailField_admin;

    @FXML
    private Button loginButton_admin;

    @FXML
    private PasswordField passwordField_admin;

    @FXML
    private void handleLogin_admin() {
        String email = emailField_admin.getText();
        String password = passwordField_admin.getText();

        if (authenticate_Admin(email, password,"SELECT * FROM admin WHERE Email = ? AND Password = ?")) {
            Alert alert = new Alert(AlertType.INFORMATION, "Login successful!");
            alert.showAndWait();
            // Proceed to next page or application home
        } else {
            Alert alert = new Alert(AlertType.ERROR, "Invalid email or password!");
            alert.showAndWait();
        }
    }
    private boolean authenticate_Admin(String email, String password, String selectUserSQL) {
        //String selectUserSQL = "SELECT * FROM users WHERE email = ? AND password = ?";

        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectUserSQL)) {

                preparedStatement.setString(1, email);
                preparedStatement.setString(2, password); // Ensure password is hashed similarly if hashed in the database

                ResultSet resultSet = preparedStatement.executeQuery();
                return resultSet.next(); // If a record exists, the login is successful

            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

//
//    @FXML
//    private void handleLogin_admin() {
//        String email = emailField_admin.getText();
//        String password = passwordField_admin.getText();
//
//        // Attempt to authenticate the admin and retrieve their name
//        String adminName = authenticate_User_and_RetrieveName(email, password, "SELECT Name FROM admin WHERE Email = ? AND Password = ?");
//
//        if (adminName != null) {
//            Alert alert = new Alert(AlertType.INFORMATION, "Login successful! Welcome, " + adminName + "!");
//            alert.showAndWait();
//
//            try {
//                // Set the logged-in admin (if you have an Admin class, you can set it similarly)
//                Admin.setCurrentAdmin(new Admin(email, adminName)); // Replace Admin with your admin class if necessary
//
//                // Load the Admin dashboard or main page (e.g., AdminDashboard.fxml)
//                FXMLLoader loader = new FXMLLoader(getClass().getResource("AdminDashboard.fxml"));
//                Parent root = loader.load();
//
//                // Create a new stage for the Admin dashboard
//                Stage adminStage = new Stage();
//                adminStage.setTitle("Admin Dashboard");
//                adminStage.setScene(new Scene(root, 1025, 650));
//                adminStage.setResizable(true);
//                adminStage.show();
//
//                // Close the admin login window
//                Stage loginStage = (Stage) emailField_admin.getScene().getWindow();
//                loginStage.close();
//
//            } catch (Exception e) {
//                e.printStackTrace();
//                Alert errorAlert = new Alert(AlertType.ERROR, "Failed to load the Admin Dashboard!");
//                errorAlert.showAndWait();
//            }
//        } else {
//            Alert alert = new Alert(AlertType.ERROR, "Invalid email or password!");
//            alert.showAndWait();
//        }
//    }


}


