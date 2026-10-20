package com.networkfinder.ui;

import com.networkfinder.model.User;
import com.networkfinder.service.AuthService;
import com.networkfinder.service.InputValidator;
import com.networkfinder.service.ValidationException;
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
    private Label errorLabel;

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
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        VBox container = new VBox(16);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setMaxWidth(560);
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
        errorLabel.getStyleClass().add("form-error");
        errorLabel.setWrapText(true);
        this.errorLabel = errorLabel;
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        VBox idBox = new VBox(4);
        Label idLabel = new Label("Student ID");
        idLabel.getStyleClass().add("field-label");
        TextField idField = new TextField();
        idField.setPromptText("Numbers only (e.g. 202612345)");
        idField.setTextFormatter(TextFieldFilters.studentIdFilter(InputValidator.getMaxIdLength()));
        idBox.getChildren().addAll(idLabel, idField);

        // Row 1: First Name + Surname (both required)
        HBox requiredNamesRow = new HBox(12);
        VBox firstNameBox = new VBox(4);
        Label firstNameLabel = new Label("First Name");
        firstNameLabel.getStyleClass().add("field-label");
        TextField firstNameField = new TextField();
        firstNameField.setPromptText("e.g. Juan (letters only)");
        firstNameField.setTextFormatter(TextFieldFilters.nameFilter(InputValidator.getMaxNameLength()));
        firstNameBox.getChildren().addAll(firstNameLabel, firstNameField);

        VBox surnameBox = new VBox(4);
        Label surnameLabel = new Label("Surname");
        surnameLabel.getStyleClass().add("field-label");
        TextField surnameField = new TextField();
        surnameField.setPromptText("e.g. Dela Cruz (letters only)");
        surnameField.setTextFormatter(TextFieldFilters.nameFilter(InputValidator.getMaxNameLength()));
        surnameBox.getChildren().addAll(surnameLabel, surnameField);
        firstNameBox.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        surnameBox.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        requiredNamesRow.getChildren().addAll(firstNameBox, surnameBox);

        // Row 2: Middle Initial + Suffix (both optional)
        HBox optionalNamesRow = new HBox(12);
        VBox middleBox = new VBox(4);
        Label middleLabel = new Label("Middle Initial (Optional)");
        middleLabel.getStyleClass().add("field-label");
        TextField middleField = new TextField();
        middleField.setPromptText("1-2 letters (e.g. M)");
        middleField.setTextFormatter(TextFieldFilters.shortNameFilter());
        middleBox.getChildren().addAll(middleLabel, middleField);

        VBox suffixBox = new VBox(4);
        Label suffixLabel = new Label("Suffix (Optional)");
        suffixLabel.getStyleClass().add("field-label");
        TextField suffixField = new TextField();
        suffixField.setPromptText("1-2 letters (e.g. Jr)");
        suffixField.setTextFormatter(TextFieldFilters.shortNameFilter());
        suffixBox.getChildren().addAll(suffixLabel, suffixField);
        middleBox.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        suffixBox.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        optionalNamesRow.getChildren().addAll(middleBox, suffixBox);

        VBox passBox = new VBox(4);
        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("field-label");
        PasswordRevealField passField = new PasswordRevealField("Create Password");
        passBox.getChildren().addAll(passLabel, passField);

        VBox confirmPassBox = new VBox(4);
        Label confirmPassLabel = new Label("Confirm Password");
        confirmPassLabel.getStyleClass().add("field-label");
        PasswordRevealField confirmPassField = new PasswordRevealField("Confirm Password");
        confirmPassBox.getChildren().addAll(confirmPassLabel, confirmPassField);

        VBox verifSection = new VBox(8);
        verifSection.setPadding(new Insets(8, 0, 0, 0));

        Label verifTitle = new Label("Student ID Verification");
        verifTitle.getStyleClass().add("question-title");

        Label verifDesc = new Label("Upload a clear picture of your Student ID. An administrator will review it before your account is approved.");
        verifDesc.getStyleClass().add("helper-text");
        verifDesc.setWrapText(true);

        VBox uploadBox = new VBox(8);
        uploadBox.getStyleClass().add("upload-box");
        uploadBox.setMinHeight(130);

        Label camIcon = new Label("📷");
        camIcon.setStyle("-fx-font-size: 26px;");

        Label uploadText = new Label("Upload Student ID");
        uploadText.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -fx-green;");

        Label formatText = new Label("JPG or PNG");
        formatText.getStyleClass().add("helper-text");

        ImageView previewImage = new ImageView();
        previewImage.setFitWidth(200);
        previewImage.setFitHeight(110);
        previewImage.setPreserveRatio(true);
        previewImage.setVisible(false);
        previewImage.setManaged(false);

        uploadBox.getChildren().addAll(camIcon, uploadText, formatText, previewImage);

        uploadBox.setOnMouseClicked(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Student ID Photo");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
            );
            File file = chooser.showOpenDialog(ScreenNavigator.getPrimaryStage());
            if (file != null) {
                String imageError = AuthService.validateIdCardImage(file);
                if (imageError != null) {
                    showError(imageError);
                    resetUploadSelection(uploadText, formatText, previewImage);
                    return;
                }

                Image img = new Image(file.toURI().toString());
                if (img.isError()) {
                    showError("This file is not a readable image. Please choose a different file.");
                    resetUploadSelection(uploadText, formatText, previewImage);
                    return;
                }

                selectedIdFile = file;
                uploadText.setText("Selected: " + file.getName());
                formatText.setText(String.format("%.1f KB", file.length() / 1024.0));
                previewImage.setImage(img);
                previewImage.setVisible(true);
                previewImage.setManaged(true);
            }
        });

        verifSection.getChildren().addAll(verifTitle, verifDesc, uploadBox);

        Button submitBtn = new Button("Create Account");
        submitBtn.getStyleClass().add("button-primary");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setDefaultButton(true);

        submitBtn.setOnAction(e -> {
            String studentId = idField.getText().trim();
            String firstName = firstNameField.getText().trim();
            String middleInitial = middleField.getText().trim();
            String surname = surnameField.getText().trim();
            String suffix = suffixField.getText().trim();
            String pass = passField.getValue();
            String confirm = confirmPassField.getValue();

            if (studentId.isEmpty() || firstName.isEmpty() || surname.isEmpty() || pass.isEmpty()) {
                showError("Please fill in all required fields.");
                return;
            }

            if (!pass.equals(confirm)) {
                showError("Passwords do not match.");
                return;
            }

            if (selectedIdFile == null) {
                showError("Please upload a clear picture of your Student ID for administrator verification.");
                return;
            }

            try {
                User created = authService.registerStudent(studentId, firstName, middleInitial,
                        surname, suffix, pass, selectedIdFile);
                if (created != null) {
                    ScreenNavigator.showRegistrationSubmitted(created);
                } else {
                    showError("Failed to create account. Please try again.");
                }
            } catch (ValidationException ve) {
                showError(ve.getMessage());
            } catch (Exception ex) {
                showError("Error saving account: " + ex.getMessage());
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

        card.getChildren().addAll(errorLabel, idBox, requiredNamesRow, optionalNamesRow, passBox, confirmPassBox, verifSection, submitBtn, loginBox);
        container.getChildren().addAll(titleBox, card);

        StackPane centeredWrap = new StackPane(container);
        centeredWrap.setAlignment(Pos.CENTER);

        scrollPane.setContent(centeredWrap);

        split.getChildren().addAll(heroPanel, scrollPane);
        getChildren().add(split);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void resetUploadSelection(Label uploadText, Label formatText, ImageView previewImage) {
        selectedIdFile = null;
        uploadText.setText("Upload Student ID");
        formatText.setText("JPG or PNG");
        previewImage.setImage(null);
        previewImage.setVisible(false);
        previewImage.setManaged(false);
    }
}
