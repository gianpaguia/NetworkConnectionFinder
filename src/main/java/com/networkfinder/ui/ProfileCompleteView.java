package com.networkfinder.ui;

import com.networkfinder.model.StudentProfile;
import com.networkfinder.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;

public class ProfileCompleteView extends StackPane {

    public ProfileCompleteView(User user, StudentProfile profile) {
        setAlignment(Pos.CENTER);
        getStyleClass().add("fluent-root-pane");

        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setMaxWidth(520);
        container.setPadding(new Insets(30, 20, 30, 20));

        // Header
        VBox titleBox = new VBox(6);
        titleBox.setAlignment(Pos.CENTER);
        Label heading = new Label("Your profile is ready!");
        heading.getStyleClass().add("screen-heading");
        Label subtitle = new Label("Your information will help us find study connections and groups.");
        subtitle.getStyleClass().add("screen-subtitle");
        titleBox.getChildren().addAll(heading, subtitle);

        // Summary Card
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        // Student Basic Info
        VBox bioBox = new VBox(4);
        Label nameLabel = new Label(user.getFullName());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: -fx-ink;");

        String courseAbbr = profile.getCourse();
        if (courseAbbr != null && courseAbbr.contains("Information Technology")) courseAbbr = "BSIT";
        else if (courseAbbr != null && courseAbbr.contains("Computer Science")) courseAbbr = "BSCS";
        else if (courseAbbr != null && courseAbbr.contains("Information Systems")) courseAbbr = "BSIS";

        Label metaLabel = new Label((courseAbbr != null ? courseAbbr : "Program") + " • " + (profile.getYearLevel() != null ? profile.getYearLevel() : "Year"));
        metaLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-green; -fx-font-weight: 600;");

        Label secLabel = new Label("Section: " + (profile.getSection() != null ? profile.getSection() : "N/A"));
        secLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-ink-soft;");

        bioBox.getChildren().addAll(nameLabel, metaLabel, secLabel);

        // Favorite Subjects section
        VBox subBox = new VBox(6);
        Label subTitle = new Label("Favorite Subjects:");
        subTitle.setStyle("-fx-font-weight: 700; -fx-text-fill: -fx-ink;");
        FlowPane subChips = new FlowPane(6, 6);
        for (String sub : profile.getSubjects()) {
            Label chip = new Label(sub);
            chip.getStyleClass().add("chip-selected");
            subChips.getChildren().add(chip);
        }
        subBox.getChildren().addAll(subTitle, subChips);

        // Study Preferences section
        VBox habBox = new VBox(6);
        Label habTitle = new Label("Study Preferences:");
        habTitle.setStyle("-fx-font-weight: 700; -fx-text-fill: -fx-ink;");
        FlowPane habChips = new FlowPane(6, 6);
        for (String hab : profile.getStudyHabits()) {
            Label chip = new Label(hab);
            chip.getStyleClass().add("chip");
            habChips.getChildren().add(chip);
        }
        habBox.getChildren().addAll(habTitle, habChips);

        // Interests section
        VBox intBox = new VBox(6);
        Label intTitle = new Label("Interests:");
        intTitle.setStyle("-fx-font-weight: 700; -fx-text-fill: -fx-ink;");
        FlowPane intChips = new FlowPane(6, 6);
        for (String interest : profile.getInterests()) {
            Label chip = new Label(interest);
            chip.getStyleClass().add("chip");
            intChips.getChildren().add(chip);
        }
        if (profile.getOtherInterests() != null && !profile.getOtherInterests().isBlank()) {
            Label otherChip = new Label(profile.getOtherInterests());
            otherChip.getStyleClass().add("chip");
            intChips.getChildren().add(otherChip);
        }
        intBox.getChildren().addAll(intTitle, intChips);

        // Go to Dashboard Button
        Button goBtn = new Button("Go to Dashboard");
        goBtn.getStyleClass().add("button-primary");
        goBtn.setMaxWidth(Double.MAX_VALUE);
        goBtn.setOnAction(e -> ScreenNavigator.showStudentDashboard(user));

        card.getChildren().addAll(bioBox, new Separator(), subBox, habBox, intBox, goBtn);
        container.getChildren().addAll(titleBox, card);

        getChildren().add(container);
    }
}
