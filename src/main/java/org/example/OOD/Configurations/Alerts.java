package org.example.OOD.Configurations;

import javafx.scene.control.Alert;

public class Alerts {
    public static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
