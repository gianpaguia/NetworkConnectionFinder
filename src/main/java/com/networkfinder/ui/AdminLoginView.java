package com.networkfinder.ui;

import com.networkfinder.model.Role;
import com.networkfinder.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

public class AdminLoginView extends StackPane {
    private final AuthService authService = new AuthService();

    public AdminLoginView() {
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

        Label subtitle = new Label("Administrator Portal");
        subtitle.getStyleClass().add("app-subtitle");

        headerBox.getChildren().addAll(appTitle, subtitle);

        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label cardHeading = new Label("Admin Sign In");
        cardHeading.getStyleClass().add("card-title");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("form-error");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        VBox userBox = new VBox(6);
        Label userLabel = new Label("Admin Username");
        userLabel.getStyleClass().add("field-label");
        TextField userField = new TextField();
        userField.setPromptText("Enter your administrator username");
        userBox.getChildren().addAll(userLabel, userField);

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
            String username = userField.getText().trim();
            String password = passValue.getValue();

            AuthService.LoginResult res = authService.authenticate(username, password);
            if (res.getStatus() == AuthService.AuthStatus.SUCCESS) {
                if (res.getUser().getRole() == Role.ADMIN) {
                    ScreenNavigator.showAdminDashboard();
                } else {
                    errorLabel.setText("Access denied: Not an administrator account.");
                    errorLabel.setVisible(true);
                    errorLabel.setManaged(true);
                }
            } else {
                errorLabel.setText(res.getMessage());
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });

        Button studentBtn = new Button("Back to Student Login");
        studentBtn.getStyleClass().add("button-link");
        studentBtn.setOnAction(e -> ScreenNavigator.showStudentLogin());

        card.getChildren().addAll(cardHeading, errorLabel, userBox, passBox, loginBtn, studentBtn);
        container.getChildren().addAll(headerBox, card);
        formPanel.getChildren().add(container);

        split.getChildren().addAll(heroPanel, formPanel);
        getChildren().add(split);
    }
}
