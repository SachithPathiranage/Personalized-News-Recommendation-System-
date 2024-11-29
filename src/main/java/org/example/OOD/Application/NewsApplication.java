package org.example.OOD.Application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.OOD.Article_Fetcher.NewsAPIHandler;

import java.io.IOException;

public class NewsApplication extends Application {
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
        // Cleanup resources if needed
        System.out.println("Application is stopping...");
        Platform.exit();
        System.exit(0); // Ensure JVM termination
    }

    public static void main(String[] args) throws IOException {
        //Login_SignupController.testConnection();
        NewsAPIHandler newsAPIHandler = new NewsAPIHandler();
        newsAPIHandler.fetchAndStoreNews();

        launch();
    }
}