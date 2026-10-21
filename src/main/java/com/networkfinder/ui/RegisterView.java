package com.networkfinder.ui;

import com.networkfinder.model.User;
import com.networkfinder.service.AuthService;
import com.networkfinder.service.InputValidator;
import com.networkfinder.service.ValidationException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;

public class RegisterView extends StackPane {
    private final AuthService authService = new AuthService();
    private File selectedIdFile = null;
    private Label idErrorLabel;
    private Label firstNameErrorLabel;
    private Label surnameErrorLabel;
    private Label middleErrorLabel;
    private Label suffixErrorLabel;
    private Label passErrorLabel;
    private Label confirmPassErrorLabel;
    private Label uploadErrorLabel;
    private Label genericErrorLabel;

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

        VBox idBox = new VBox(4);
        Label idLabel = new Label("Student ID");
        idLabel.getStyleClass().add("field-label");
        TextField idField = new TextField();
        idField.setPromptText("e.g. 20261001-A (8 digits, dash, one letter)");
        idField.setTextFormatter(TextFieldFilters.studentIdFilter());
        idErrorLabel = fieldError();
        idBox.getChildren().addAll(idLabel, idField, idErrorLabel);

        // Row 1: First Name + Surname (both required)
        HBox requiredNamesRow = new HBox(12);
        VBox firstNameBox = new VBox(4);
        Label firstNameLabel = new Label("First Name");
        firstNameLabel.getStyleClass().add("field-label");
        TextField firstNameField = new TextField();
        firstNameField.setPromptText("e.g. Juan (letters only)");
        firstNameField.setTextFormatter(TextFieldFilters.nameFilter(InputValidator.getMaxNameLength()));
        firstNameErrorLabel = fieldError();
        firstNameBox.getChildren().addAll(firstNameLabel, firstNameField, firstNameErrorLabel);

        VBox surnameBox = new VBox(4);
        Label surnameLabel = new Label("Surname");
        surnameLabel.getStyleClass().add("field-label");
        TextField surnameField = new TextField();
        surnameField.setPromptText("e.g. Dela Cruz (letters only)");
        surnameField.setTextFormatter(TextFieldFilters.nameFilter(InputValidator.getMaxNameLength()));
        surnameErrorLabel = fieldError();
        surnameBox.getChildren().addAll(surnameLabel, surnameField, surnameErrorLabel);
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
        middleErrorLabel = fieldError();
        middleBox.getChildren().addAll(middleLabel, middleField, middleErrorLabel);

        VBox suffixBox = new VBox(4);
        Label suffixLabel = new Label("Suffix (Optional)");
        suffixLabel.getStyleClass().add("field-label");
        TextField suffixField = new TextField();
        suffixField.setPromptText("1-2 letters (e.g. Jr)");
        suffixField.setTextFormatter(TextFieldFilters.shortNameFilter());
        suffixErrorLabel = fieldError();
        suffixBox.getChildren().addAll(suffixLabel, suffixField, suffixErrorLabel);
        middleBox.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        suffixBox.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        optionalNamesRow.getChildren().addAll(middleBox, suffixBox);

        VBox passBox = new VBox(4);
        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("field-label");
        PasswordRevealField passField = new PasswordRevealField("Create Password");
        passErrorLabel = fieldError();
        passBox.getChildren().addAll(passLabel, passField, passErrorLabel);

        VBox confirmPassBox = new VBox(4);
        Label confirmPassLabel = new Label("Confirm Password");
        confirmPassLabel.getStyleClass().add("field-label");
        PasswordRevealField confirmPassField = new PasswordRevealField("Confirm Password");
        confirmPassErrorLabel = fieldError();
        confirmPassBox.getChildren().addAll(confirmPassLabel, confirmPassField, confirmPassErrorLabel);

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

        Node camIcon = SvgIcons.icon(SvgIcons.Glyph.CAMERA, 32, "svg-icon-success");

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
                    showFieldError(uploadErrorLabel, imageError);
                    resetUploadSelection(uploadText, formatText, previewImage);
                    return;
                }

                Image img = new Image(file.toURI().toString());
                if (img.isError()) {
                    showFieldError(uploadErrorLabel, "This file is not a readable image. Please choose a different file.");
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

        uploadErrorLabel = fieldError();
        verifSection.getChildren().addAll(verifTitle, verifDesc, uploadBox, uploadErrorLabel);

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
            String pass = passField.getValue().trim();
            String confirm = confirmPassField.getValue().trim();

            clearFieldErrors();

            boolean valid = true;

            String idError = InputValidator.checkStudentId(studentId);
            if (idError != null) {
                showFieldError(idErrorLabel, idError);
                valid = false;
            }
            String firstNameError = InputValidator.checkFirstName(firstName);
            if (firstNameError != null) {
                showFieldError(firstNameErrorLabel, firstNameError);
                valid = false;
            }
            String surnameError = InputValidator.checkSurname(surname);
            if (surnameError != null) {
                showFieldError(surnameErrorLabel, surnameError);
                valid = false;
            }
            String middleError = InputValidator.checkMiddleInitial(middleInitial);
            if (middleError != null) {
                showFieldError(middleErrorLabel, middleError);
                valid = false;
            }
            String suffixError = InputValidator.checkSuffix(suffix);
            if (suffixError != null) {
                showFieldError(suffixErrorLabel, suffixError);
                valid = false;
            }
            String passwordError = InputValidator.checkPassword(pass);
            if (passwordError != null) {
                showFieldError(passErrorLabel, passwordError);
                valid = false;
            } else if (!pass.equals(confirm)) {
                showFieldError(confirmPassErrorLabel, "Passwords do not match.");
                valid = false;
            }
            if (selectedIdFile == null) {
                showFieldError(uploadErrorLabel, "Please upload a clear picture of your Student ID for administrator verification.");
                valid = false;
            }

            if (!valid) {
                return;
            }

            try {
                User created = authService.registerStudent(studentId, firstName, middleInitial,
                        surname, suffix, pass, selectedIdFile);
                if (created != null) {
                    ScreenNavigator.showRegistrationSubmitted(created);
                } else {
                    showFieldError(genericErrorLabel, "Failed to create account. Please try again.");
                }
            } catch (ValidationException ve) {
                routeValidationError(ve.getMessage());
            } catch (Exception ex) {
                showFieldError(genericErrorLabel, "Error saving account: " + ex.getMessage());
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

        genericErrorLabel = fieldError();
        card.getChildren().addAll(idBox, requiredNamesRow, optionalNamesRow, passBox, confirmPassBox, verifSection, submitBtn, genericErrorLabel, loginBox);
        container.getChildren().addAll(titleBox, card);

        StackPane centeredWrap = new StackPane(container);
        centeredWrap.setAlignment(Pos.CENTER);

        scrollPane.setContent(centeredWrap);

        split.getChildren().addAll(heroPanel, scrollPane);
        getChildren().add(split);
    }

    private static Label fieldError() {
        Label label = new Label();
        label.getStyleClass().add("form-error");
        label.setWrapText(true);
        label.setVisible(false);
        label.setManaged(false);
        return label;
    }

    private static void showFieldError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearFieldErrors() {
        Label[] labels = {idErrorLabel, firstNameErrorLabel, surnameErrorLabel, middleErrorLabel,
                suffixErrorLabel, passErrorLabel, confirmPassErrorLabel, uploadErrorLabel, genericErrorLabel};
        for (Label label : labels) {
            label.setText(null);
            label.setVisible(false);
            label.setManaged(false);
        }
    }

    private void routeValidationError(String message) {
        String msg = message == null ? "" : message.toLowerCase();
        if (msg.contains("password")) {
            showFieldError(passErrorLabel, message);
        } else if (msg.contains("student id")) {
            showFieldError(idErrorLabel, message);
        } else if (msg.contains("first name")) {
            showFieldError(firstNameErrorLabel, message);
        } else if (msg.contains("surname")) {
            showFieldError(surnameErrorLabel, message);
        } else if (msg.contains("middle")) {
            showFieldError(middleErrorLabel, message);
        } else if (msg.contains("suffix")) {
            showFieldError(suffixErrorLabel, message);
        } else {
            showFieldError(genericErrorLabel, message);
        }
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
