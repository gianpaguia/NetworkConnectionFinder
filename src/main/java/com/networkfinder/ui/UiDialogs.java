package com.networkfinder.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
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

    /** Asks the user to confirm signing out. Returns true only when explicitly confirmed. */
    public static boolean confirmSignOut() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Sign Out");
        alert.setHeaderText("Sign out of Network Connection Finder?");
        alert.setContentText("You will be returned to the login screen and will need to sign in again to continue.");
        styleDialogPane(alert.getDialogPane());
        ButtonType signOutBtn = new ButtonType("Sign out", ButtonBar.ButtonData.OK_DONE);
        alert.getDialogPane().getButtonTypes().setAll(signOutBtn, ButtonType.CANCEL);
        return alert.showAndWait().filter(response -> response == signOutBtn).isPresent();
    }
}
