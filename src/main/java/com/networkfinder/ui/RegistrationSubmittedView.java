package com.networkfinder.ui;

import com.networkfinder.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class RegistrationSubmittedView extends StackPane {

    public RegistrationSubmittedView(User user) {
        setAlignment(Pos.CENTER);
        getStyleClass().add("fluent-root-pane");

        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setMaxWidth(460);
        container.setPadding(new Insets(30));

        // Heading
        Label heading = new Label("Account submitted!");
        heading.getStyleClass().add("screen-heading");

        // Status Card
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        // Status Badge container
        HBox badgeBox = new HBox(8);
        badgeBox.setAlignment(Pos.CENTER_LEFT);
        Label statusBadge = new Label("🟡 Pending Verification");
        statusBadge.getStyleClass().add("badge-pending");
        statusBadge.setStyle("-fx-font-size: 13px; -fx-padding: 6px 14px;");
        badgeBox.getChildren().add(statusBadge);

        // Details grid
        GridPane details = new GridPane();
        details.setHgap(16);
        details.setVgap(10);

        Label idLabel = new Label("Student ID:");
        idLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-ink-soft;");
        Label idVal = new Label(user != null ? user.getStudentId() : "2026-12345");
        idVal.setStyle("-fx-font-weight: 700; -fx-text-fill: -fx-ink;");

        Label nameLabel = new Label("Name:");
        nameLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-ink-soft;");
        Label nameVal = new Label(user != null ? user.getFullName() : "Student Name");
        nameVal.setStyle("-fx-font-weight: 700; -fx-text-fill: -fx-ink;");

        details.addRow(0, idLabel, idVal);
        details.addRow(1, nameLabel, nameVal);

        // Message
        Label message = new Label("Your account has been submitted for administrator verification. Please wait until your Student ID has been verified.");
        message.setStyle("-fx-text-fill: -fx-ink-soft; -fx-line-spacing: 2px; -fx-font-size: 13px;");
        message.setWrapText(true);

        // Back to Login Button
        Button backBtn = new Button("Back to Login");
        backBtn.getStyleClass().add("button-primary");
        backBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setOnAction(e -> ScreenNavigator.showStudentLogin());

        card.getChildren().addAll(badgeBox, details, message, backBtn);
        container.getChildren().addAll(heading, card);

        getChildren().add(container);
    }
}
