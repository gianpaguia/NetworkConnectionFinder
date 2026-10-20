package com.networkfinder.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;

public final class UiDialogs {

    private UiDialogs() {
    }

    public static void styleDialogPane(DialogPane pane) {
        var stylesheet = UiDialogs.class.getResource("/css/styles.css");
        if (stylesheet != null) {
            pane.getStylesheets().add(stylesheet.toExternalForm());
        }
    }

    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Action Failed");
        alert.setHeaderText("The operation could not be completed.");
        alert.setContentText(message);
        styleDialogPane(alert.getDialogPane());
        alert.showAndWait();
    }

    public static void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Done");
        alert.setHeaderText("The operation completed successfully.");
        alert.setContentText(message);
        styleDialogPane(alert.getDialogPane());
        alert.showAndWait();
    }
}
