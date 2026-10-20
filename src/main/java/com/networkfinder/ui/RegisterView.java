package com.networkfinder.ui;

import com.networkfinder.model.User;
import com.networkfinder.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;

public class RegisterView extends StackPane {
    private final AuthService authService = new AuthService();
    private File selectedIdFile = null;

    public RegisterView() {
        setAlignment(Pos.CENTER);
        getStyleClass().add("fluent-root-pane");

        HBox split = new HBox();
        split.setFillHeight(true);

        StackPane heroPanel = new StackPane();
        heroPanel.setMinWidth(460);
        heroPanel.setMaxWidth(640);
        HBox.setHgrow(heroPanel, Priority.ALWAYS);
        GraphConstellationCanvas hero = new GraphConstellationCanvas();
        heroPanel.getChildren().add(hero);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setMaxWidth(520);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        VBox container = new VBox(16);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setMaxWidth(480);
        container.setPadding(new Insets(30, 24, 30, 24));

        VBox titleBox = new VBox(4);
        Label heading = new Label("Create your account");
        heading.getStyleClass().add("screen-heading");
        Label subtitle = new Label("Use your university Student ID to create your account.");
        subtitle.getStyleClass().add("screen-subtitle");
        titleBox.getChildren().addAll(heading, subtitle);

        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: -fx-danger; -fx-font-weight: 500; -fx-font-size: 13px;");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        VBox idBox = new VBox(4);
        Label idLabel = new Label("Student ID");
        idLabel.getStyleClass().add("field-label");
        TextField idField = new TextField();
        idField.setPromptText("Enter Student ID (e.g. 2026-12345)");
        Label idHelper = new Label("Letters, digits, dots, dashes or underscores (max 64 characters).");
        idHelper.getStyleClass().add("helper-text");
        idBox.getChildren().addAll(idLabel, idField, idHelper);

        VBox nameBox = new VBox(4);
        Label nameLabel = new Label("Full Name");
        nameLabel.getStyleClass().add("field-label");
        TextField nameField = new TextField();
        nameField.setPromptText("Enter Full Name");
        nameBox.getChildren().addAll(nameLabel, nameField);

        VBox passBox = new VBox(4);
        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("field-label");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Create Password");
        Label passHelper = new Label("At least 8 characters.");
        passHelper.getStyleClass().add("helper-text");
        passBox.getChildren().addAll(passLabel, passField, passHelper);

        VBox confirmPassBox = new VBox(4);
        Label confirmPassLabel = new Label("Confirm Password");
        confirmPassLabel.getStyleClass().add("field-label");
        PasswordField confirmPassField = new PasswordField();
        confirmPassField.setPromptText("Confirm Password");
        confirmPassBox.getChildren().addAll(confirmPassLabel, confirmPassField);

        VBox verifSection = new VBox(8);
        verifSection.setPadding(new Insets(8, 0, 0, 0));

        Label verifTitle = new Label("Student ID Verification");
        verifTitle.getStyleClass().add("question-title");

        Label verifDesc = new Label("Upload a clear picture of your Student ID. An administrator will review it before your account is approved.");
        verifDesc.getStyleClass().add("helper-text");
        verifDesc.setWrapText(true);

        Button uploadBox = new Button();
        uploadBox.getStyleClass().add("upload-box");
        uploadBox.setMinHeight(130);
        uploadBox.setMaxWidth(Double.MAX_VALUE);
        uploadBox.setAlignment(Pos.CENTER);
        uploadBox.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        VBox uploadContent = new VBox(8);
        uploadContent.setAlignment(Pos.CENTER);
        uploadContent.setFillWidth(true);

        Label camIcon = new Label("📷");
        camIcon.setStyle("-fx-font-size: 26px;");

        Label uploadText = new Label("Upload Student ID");
        uploadText.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -fx-green;");

        Label formatText = new Label("JPG or PNG · max 5 MB");
        formatText.getStyleClass().add("helper-text");

        ImageView previewImage = new ImageView();
        previewImage.setFitWidth(200);
        previewImage.setFitHeight(110);
        previewImage.setPreserveRatio(true);
        previewImage.setVisible(false);
        previewImage.setManaged(false);

        uploadContent.getChildren().addAll(camIcon, uploadText, formatText, previewImage);
        uploadBox.setGraphic(uploadContent);

        uploadBox.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Student ID Photo");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
            );
            File file = chooser.showOpenDialog(ScreenNavigator.getPrimaryStage());
            if (file != null) {
                selectedIdFile = file;
                uploadText.setText("Selected: " + file.getName());
                formatText.setText(String.format("%.1f KB", file.length() / 1024.0));
                try {
                    Image img = new Image(file.toURI().toString());
                    previewImage.setImage(img);
                    previewImage.setVisible(true);
                    previewImage.setManaged(true);
                } catch (Exception ignored) {
                }
            }
        });

        verifSection.getChildren().addAll(verifTitle, verifDesc, uploadBox);

        Button submitBtn = new Button("Create Account");
        submitBtn.getStyleClass().add("button-primary");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setDefaultButton(true);

        submitBtn.setOnAction(e -> {
            String studentId = idField.getText().trim();
            String name = nameField.getText().trim();
            String pass = passField.getText();
            String confirm = confirmPassField.getText();

            if (studentId.isEmpty() || name.isEmpty() || pass.isEmpty()) {
                errorLabel.setText("Please fill in all required fields.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            if (!studentId.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")) {
                errorLabel.setText("Invalid Student ID. Use letters, digits, dots, dashes or underscores only (e.g. 2026-12345).");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            if (pass.length() < 8) {
                errorLabel.setText("Password must be at least 8 characters long.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            if (!pass.equals(confirm)) {
                errorLabel.setText("Passwords do not match.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            if (selectedIdFile == null) {
                errorLabel.setText("Please upload a clear picture of your Student ID for administrator verification.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            if (authService.isStudentIdRegistered(studentId)) {
                errorLabel.setText("An account with this Student ID already exists.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            try {
                User created = authService.registerStudent(studentId, name, pass, selectedIdFile);
                if (created != null) {
                    ScreenNavigator.showRegistrationSubmitted(created);
                } else {
                    errorLabel.setText("Failed to create account. Please try again.");
                    errorLabel.setVisible(true);
                    errorLabel.setManaged(true);
                }
            } catch (Exception ex) {
                errorLabel.setText("Error saving account: " + ex.getMessage());
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });

        HBox loginBox = new HBox(4);
        loginBox.setAlignment(Pos.CENTER_LEFT);
        Label already = new Label("Already have an account?");
        already.setStyle("-fx-text-fill: -fx-ink-soft;");
        Button loginLink = new Button("Login");
        loginLink.getStyleClass().add("button-link");
        loginLink.setOnAction(e -> ScreenNavigator.showStudentLogin());
        loginBox.getChildren().addAll(already, loginLink);

        card.getChildren().addAll(errorLabel, idBox, nameBox, passBox, confirmPassBox, verifSection, submitBtn, loginBox);
        container.getChildren().addAll(titleBox, card);

        scrollPane.setContent(container);

        split.getChildren().addAll(heroPanel, scrollPane);
        getChildren().add(split);
    }
}
