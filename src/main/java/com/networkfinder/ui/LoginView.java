package com.networkfinder.ui;

import com.networkfinder.database.ProfileDao;
import com.networkfinder.database.UserDao;
import com.networkfinder.model.Role;
import com.networkfinder.model.StudentProfile;
import com.networkfinder.model.User;
import com.networkfinder.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

public class LoginView extends StackPane {
    private final AuthService authService = new AuthService();
    private final ProfileDao profileDao = new ProfileDao();

    public LoginView() {
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

        VBox formPanel = new VBox(20);
        formPanel.setAlignment(Pos.CENTER);
        formPanel.setPadding(new Insets(30));
        HBox.setHgrow(formPanel, Priority.ALWAYS);

        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setMaxWidth(560);

        VBox headerBox = new VBox(6);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Text appTitle = new Text("Social Network\nConnection Finder");
        appTitle.getStyleClass().add("app-title");
        appTitle.setFill(javafx.scene.paint.Color.WHITE);

        Label subtitle = new Label("Connect with classmates. Find study groups.");
        subtitle.getStyleClass().add("app-subtitle");

        headerBox.getChildren().addAll(appTitle, subtitle);

        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label cardHeading = new Label("Student Sign In");
        cardHeading.getStyleClass().add("card-title");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("form-error");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        VBox idBox = new VBox(6);
        Label idLabel = new Label("Student ID");
        idLabel.getStyleClass().add("field-label");
        TextField idField = new TextField();
        idField.setPromptText("Enter your Student ID (e.g. 2026-10001)");
        idBox.getChildren().addAll(idLabel, idField);

        VBox passBox = new VBox(6);
        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("field-label");

        PasswordRevealField passValue = new PasswordRevealField("Enter your password");
        passBox.getChildren().addAll(passLabel, passValue);

        Button loginBtn = new Button("Sign in");
        loginBtn.getStyleClass().add("button-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setDefaultButton(true);

        loginBtn.setOnAction(e -> {
            String id = idField.getText().trim();
            String pass = passValue.getValue();

            AuthService.LoginResult result = authService.authenticate(id, pass);
            switch (result.getStatus()) {
                case SUCCESS -> {
                    User user = result.getUser();
                    if (user.getRole() == Role.ADMIN) {
                        errorLabel.setText("Administrator accounts must sign in through the Admin Portal.");
                        errorLabel.setVisible(true);
                        errorLabel.setManaged(true);
                    } else {
                        StudentProfile profile = profileDao.getProfile(user.getStudentId());
                        if (user.isProfileCompleted() && profile != null) {
                            ScreenNavigator.showStudentDashboard(user);
                        } else {
                            ScreenNavigator.showProfileWizard(user);
                        }
                    }
                }
                case PENDING_VERIFICATION -> {
                    ScreenNavigator.showRegistrationSubmitted(result.getUser());
                }
                default -> {
                    errorLabel.setText(result.getMessage());
                    errorLabel.setVisible(true);
                    errorLabel.setManaged(true);
                }
            }
        });

        HBox registerBox = new HBox(4);
        registerBox.setAlignment(Pos.CENTER_LEFT);
        Label noAccount = new Label("Don't have an account?");
        noAccount.setStyle("-fx-text-fill: -fx-ink-soft;");
        Button registerLink = new Button("Create an Account");
        registerLink.getStyleClass().add("button-link");
        registerLink.setOnAction(e -> ScreenNavigator.showRegister());
        registerBox.getChildren().addAll(noAccount, registerLink);

        Label helperText = new Label("Your Student ID is your username.");
        helperText.getStyleClass().add("helper-text");

        VBox helperBox = new VBox(helperText);
        helperBox.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(cardHeading, errorLabel, idBox, passBox, loginBtn, registerBox, helperBox);

        Button adminBtn = new Button("Admin Portal");
        adminBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -fx-ink-soft; -fx-font-size: 14px; -fx-font-weight: 500; -fx-cursor: hand;");
        adminBtn.setOnAction(e -> ScreenNavigator.showAdminLogin());

        container.getChildren().addAll(headerBox, card, adminBtn);
        formPanel.getChildren().add(container);

        split.getChildren().addAll(heroPanel, formPanel);
        getChildren().add(split);
    }
}
