package org.example.OOD.Application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.OOD.Article_Fetcher.NewsAPIHandler;
import org.example.OOD.Database_Handler.DatabaseHandler;
import org.example.OOD.Recommendation_Engine.Categorization;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NewsApplication extends Application {
    public final static ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(NewsApplication.class.getResource("/Design_Files/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 650, 455);
        stage.setTitle("User Login");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        System.out.println("Application is stopping...");

        // Gracefully shut down the ExecutorService
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("ExecutorService did not terminate in the specified time. Forcing shutdown...");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.out.println("ExecutorService shutdown interrupted. Forcing shutdown...");
            executorService.shutdownNow();
            Thread.currentThread().interrupt(); // Restore the interrupted status
        }

        // Cleanly shut down JavaFX
        Platform.exit();

        // Terminate the JVM as a last resort
        System.exit(0);
    }


    public static void main(String[] args) {
//        //Login_SignupController.testConnection();
//        NewsAPIHandler newsAPIHandler = new NewsAPIHandler();
//
//        // List of categories to fetch
//        String[] categories = {"General",
//                "Tech",
//                "Entertainment",
//                "Business",
//                "Sports",
//                "Politics",
//                "Travel",
//                "Food",
//                "Health"};
//
//        for (String category : categories) {
//            newsAPIHandler.fetchAndSaveNewsArticles(category);
//        }
        try {
            // Step 1: Get the database handler instance
            DatabaseHandler dbHandler = DatabaseHandler.getInstance();

            // Step 2: Create the Categorization instance
            Categorization categorization = new Categorization();

            // Step 3: Call the method to categorize and update articles
            categorization.categorizeAndUpdateArticles(dbHandler);

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        launch();
    }
}