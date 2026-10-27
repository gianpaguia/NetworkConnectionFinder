package com.networkfinder.ui;

import com.networkfinder.database.DatabaseManager;
import com.networkfinder.database.ProfileDao;
import com.networkfinder.dsa.*;
import com.networkfinder.model.StudentProfile;
import com.networkfinder.model.User;
import com.networkfinder.service.NetworkService;
import com.networkfinder.service.AuthService;
import com.networkfinder.service.ValidationException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class StudentDashboardView extends BorderPane {
    private final User currentUser;
    private final NetworkService networkService = NetworkService.getInstance();
    private final ProfileDao profileDao = new ProfileDao();
    private final Map<String, Integer> matcherPageState = new HashMap<>();

    private final StackPane contentArea = new StackPane();
    private Button windowBackBtn;
    private Label windowTitleLbl;
    private Label brandTitleLbl;
    private List<VBox> allTiles = new ArrayList<>();

    public StudentDashboardView(User user) {
        this.currentUser = user;
        getStyleClass().add("fluent-root-pane");

        setTop(buildTopWindowBar());

        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        setCenter(scrollPane);

        showHomeScreen();
    }

    private HBox buildTopWindowBar() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("window-top-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        windowBackBtn = new Button("←  Return");
        windowBackBtn.getStyleClass().add("window-back-btn");
        windowBackBtn.setOnAction(e -> showHomeScreen());
        windowBackBtn.setVisible(false);

        windowTitleLbl = new Label();
        windowTitleLbl.getStyleClass().add("window-title");

        brandTitleLbl = new Label("Network Connection Finder");
        brandTitleLbl.getStyleClass().add("window-brand");

        HBox leftBox = new HBox(12, windowBackBtn, brandTitleLbl, windowTitleLbl);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        leftBox.setMinWidth(240);

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        Button notifBtn = buildStudentNotificationButton();

        Button settingsBtn = new Button("Settings · Account");
        settingsBtn.getStyleClass().add("button-secondary");
        settingsBtn.setGraphic(SvgIcons.icon(SvgIcons.Glyph.SETTINGS, 15, "svg-icon-strong"));
        settingsBtn.setOnAction(e -> {
            setBreadcrumb("Settings > Account");
            showSettingsPanel();
        });

        Button logoutBtn = new Button("Sign out");
        logoutBtn.getStyleClass().add("button-secondary");
        logoutBtn.setGraphic(SvgIcons.icon(SvgIcons.Glyph.LOG_OUT, 15, "svg-icon-strong"));
        logoutBtn.setOnAction(e -> ScreenNavigator.showStudentLogin());

        HBox rightBox = new HBox(10, notifBtn, settingsBtn, logoutBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.setMinWidth(240);

        bar.getChildren().addAll(leftBox, leftSpacer, rightBox);
        return bar;
    }

    private void setBreadcrumb(String title) {
        boolean atHome = title == null;
        windowBackBtn.setVisible(!atHome);
        windowBackBtn.setManaged(!atHome);
        brandTitleLbl.setVisible(atHome);
        brandTitleLbl.setManaged(atHome);
        windowTitleLbl.setVisible(!atHome);
        windowTitleLbl.setManaged(!atHome);
        if (!atHome) {
            windowTitleLbl.setText(title);
        }
    }

    private Button buildStudentNotificationButton() {
        Button btn = new Button();
        btn.getStyleClass().add("notification-bell-btn");

        List<com.networkfinder.model.Connection> incoming = networkService.getIncomingRequests(currentUser.getStudentId());
        int count = incoming.size();

        HBox content = new HBox(6);
        content.setAlignment(Pos.CENTER);

        Node bell = SvgIcons.icon(SvgIcons.Glyph.BELL, 15, count > 0 ? "svg-icon-warn" : "svg-icon");
        content.getChildren().add(bell);

        if (count > 0) {
            Label badge = new Label(String.valueOf(count));
            badge.getStyleClass().add("notification-badge");
            content.getChildren().add(badge);
        }

        btn.setGraphic(content);
        btn.setTooltip(new Tooltip(count + " pending incoming invitation(s)"));
        btn.setOnAction(e -> showStudentNotificationsModal());
        return btn;
    }

    private void showStudentNotificationsModal() {
        List<com.networkfinder.model.Connection> incoming = networkService.getIncomingRequests(currentUser.getStudentId());
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Notifications");
        dialog.setHeaderText("Connection Invitations & Network Activity");
        UiDialogs.styleDialogPane(dialog.getDialogPane());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox box = new VBox(12);
        box.setPrefWidth(420);
        box.setPadding(new Insets(10, 0, 10, 0));

        if (incoming.isEmpty()) {
            VBox emptyCard = new VBox(8);
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(20));
            Node checkIcon = SvgIcons.icon(SvgIcons.Glyph.CHECK, 28, "svg-icon-success");
            Label emptyLbl = new Label("No new notifications");
            emptyLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -fx-ink;");
            Label subLbl = new Label("You have no pending incoming connection requests.");
            subLbl.setStyle("-fx-text-fill: -fx-ink-soft; -fx-font-size: 12px;");
            emptyCard.getChildren().addAll(checkIcon, emptyLbl, subLbl);
            box.getChildren().add(emptyCard);
        } else {
            Label header = new Label(incoming.size() + " Incoming Connection Request" + (incoming.size() > 1 ? "s" : ""));
            header.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: -fx-accent;");
            box.getChildren().add(header);

            for (com.networkfinder.model.Connection c : incoming) {
                StudentNode sNode = networkService.getGraph().getNode(c.getRequesterId());
                HBox item = new HBox(12);
                item.getStyleClass().add("notification-item");
                item.setAlignment(Pos.CENTER_LEFT);

                Node icon = SvgIcons.icon(SvgIcons.Glyph.USER, 16, "svg-icon-accent");
                VBox textCol = new VBox(2);
                Label name = new Label(sNode != null ? sNode.getFullName() : c.getRequesterId());
                name.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: -fx-ink;");
                Label sub = new Label("Wants to connect on campus");
                sub.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-ink-soft;");
                textCol.getChildren().addAll(name, sub);
                HBox.setHgrow(textCol, Priority.ALWAYS);

                Button acceptBtn = new Button("Accept");
                acceptBtn.getStyleClass().add("button-primary");
                acceptBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
                acceptBtn.setOnAction(ev -> {
                    networkService.acceptConnectionRequest(c.getRequesterId(), c.getReceiverId());
                    dialog.close();
                    setTop(buildTopWindowBar());
                    showHomeScreen();
                });

                Button declineBtn = new Button("Decline");
                declineBtn.getStyleClass().add("button-secondary");
                declineBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10; -fx-text-fill: -fx-danger;");
                declineBtn.setOnAction(ev -> {
                    networkService.declineConnectionRequest(c.getRequesterId(), c.getReceiverId());
                    dialog.close();
                    setTop(buildTopWindowBar());
                    showHomeScreen();
                });

                item.getChildren().addAll(icon, textCol, acceptBtn, declineBtn);
                box.getChildren().add(item);
            }
        }

        dialog.getDialogPane().setContent(box);
        dialog.showAndWait();
    }

    // ==================================================
    // 0. HOME SCREEN (WINDOWS SETTINGS HUB)
    // ==================================================
    private void showHomeScreen() {
        setBreadcrumb(null);

        VBox homeBox = new VBox(0);
        homeBox.setAlignment(Pos.TOP_CENTER);
        homeBox.setPadding(new Insets(10, 0, 40, 0));

        // 1. Hero Profile Banner (Allisia Rose style)
        HBox heroBanner = new HBox(20);
        heroBanner.getStyleClass().add("fluent-profile-hero");
        heroBanner.setAlignment(Pos.CENTER_LEFT);

        // Circular Avatar
        StackPane avatarPane = buildClickableAvatar(42);

        // User Meta
        VBox metaBox = new VBox(2);
        Label nameLbl = new Label(currentUser.getFullName());
        nameLbl.getStyleClass().add("fluent-user-name");

        StudentProfile profile = profileDao.getProfile(currentUser.getStudentId());

        String subText = currentUser.getStudentId();
        if (profile != null && profile.getCourse() != null) {
            subText += "  •  " + profile.getCourse() + (profile.getSection() != null ? " (" + profile.getSection() + ")" : "");
        }
        Label subLbl = new Label(subText);
        subLbl.getStyleClass().add("fluent-user-subtitle");

        metaBox.getChildren().addAll(nameLbl, subLbl);

        Region heroSpacer = new Region();
        HBox.setHgrow(heroSpacer, Priority.ALWAYS);

        // Right-aligned status widgets
        int connectionCount = networkService.getGraph().getDegree(currentUser.getStudentId());
        HBox statusCards = new HBox(12);
        statusCards.setAlignment(Pos.CENTER_RIGHT);

        VBox networkCard = createStatusWidget(SvgIcons.icon(SvgIcons.Glyph.SHARE_2, 18, "svg-icon-accent"), "Your Network", connectionCount + " Connected");
        VBox updateCard = createStatusWidget(SvgIcons.icon(SvgIcons.Glyph.REFRESH_CW, 18, "svg-icon-accent"), "Match Engine", "BFS Ready ✓");
        VBox securityCard = createStatusWidget(SvgIcons.icon(SvgIcons.Glyph.SHIELD_CHECK, 18, "svg-icon-accent"), "Verification", "Protected ✓");

        statusCards.getChildren().addAll(networkCard, updateCard, securityCard);
        heroBanner.getChildren().addAll(avatarPane, metaBox, heroSpacer, statusCards);

        // 2. Centered Search Box
        VBox searchContainer = new VBox(8);
        searchContainer.getStyleClass().add("fluent-search-box");

        TextField searchField = new TextField();
        searchField.setPromptText("Find a setting or student");
        searchField.getStyleClass().add("fluent-search-field");

        searchContainer.getChildren().add(searchField);

        // 3. Category Grid (Fluent Tiles) - stretches to fill maximized windows
        GridPane tileGrid = new GridPane();
        tileGrid.getStyleClass().add("fluent-grid-container");
        tileGrid.setHgap(16);
        tileGrid.setVgap(16);
        tileGrid.setMaxWidth(Double.MAX_VALUE);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setHgrow(Priority.ALWAYS);
            col.setFillWidth(true);
            col.setPercentWidth(100.0 / 3);
            tileGrid.getColumnConstraints().add(col);
        }
        RowConstraints rowA = new RowConstraints(118, 118, Region.USE_PREF_SIZE);
        rowA.setFillHeight(true);
        tileGrid.getRowConstraints().add(rowA);
        RowConstraints rowB = new RowConstraints(118, 118, Region.USE_PREF_SIZE);
        rowB.setFillHeight(true);
        tileGrid.getRowConstraints().add(rowB);
        RowConstraints rowC = new RowConstraints(118, 118, Region.USE_PREF_SIZE);
        rowC.setFillHeight(true);
        tileGrid.getRowConstraints().add(rowC);

        allTiles.clear();

        VBox tileMatcher = createFluentTile(SvgIcons.icon(SvgIcons.Glyph.TARGET, 26, "svg-icon-accent"), "Study Group Matcher", "Level-order BFS, subjects, peers", () -> {
            setBreadcrumb("Settings > Study Group Matcher");
            showStudyGroupMatcherScreen();
        });

        VBox tilePathfinder = createFluentTile(SvgIcons.icon(SvgIcons.Glyph.ROUTE, 26, "svg-icon-accent"), "Pathfinder (BFS)", "Degrees of separation, shortest chain", () -> {
            setBreadcrumb("Settings > Pathfinder (BFS)");
            showPathfinderScreen();
        });

        VBox tileConnections = createFluentTile(SvgIcons.icon(SvgIcons.Glyph.USERS, 26, "svg-icon-accent"), "My Connections", "Direct peers, mutual friends, requests", () -> {
            setBreadcrumb("Settings > My Connections");
            showConnectionsScreen();
        });

        VBox tileAnalytics = createFluentTile(SvgIcons.icon(SvgIcons.Glyph.SHARE_2, 26, "svg-icon-accent"), "Network Graph & Hubs", "Degree centrality, hubs, constellation", () -> {
            setBreadcrumb("Settings > Network Graph & Hubs");
            showAnalyticsScreen();
        });

        VBox tileProfile = createFluentTile(SvgIcons.icon(SvgIcons.Glyph.GRADUATION_CAP, 26, "svg-icon-accent"), "Academic Profile", "Subjects, study habits, group preference", () -> {
            ScreenNavigator.showProfileWizard(currentUser);
        });

        allTiles.addAll(Arrays.asList(tileMatcher, tilePathfinder, tileConnections, tileAnalytics, tileProfile));
        layoutTilesOnGrid(tileGrid, allTiles);

        // Live search filter on tiles
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = (newVal == null) ? "" : newVal.toLowerCase().trim();
            List<VBox> visibleTiles = new ArrayList<>();
            for (VBox t : allTiles) {
                Label tTitle = (Label) ((VBox) ((HBox) t.getChildren().get(0)).getChildren().get(1)).getChildren().get(0);
                Label tDesc = (Label) ((VBox) ((HBox) t.getChildren().get(0)).getChildren().get(1)).getChildren().get(1);
                if (query.isEmpty() || tTitle.getText().toLowerCase().contains(query) || tDesc.getText().toLowerCase().contains(query)) {
                    visibleTiles.add(t);
                }
            }
            layoutTilesOnGrid(tileGrid, visibleTiles);
        });

        homeBox.getChildren().addAll(heroBanner, searchContainer, tileGrid);
        contentArea.getChildren().setAll(homeBox);
    }

    private VBox createStatusWidget(Node icon, String title, String sub) {
        VBox card = new VBox(3);
        card.getStyleClass().add("fluent-status-card");

        StackPane iconHolder = new StackPane();
        iconHolder.getStyleClass().add("fluent-status-icon");
        iconHolder.getChildren().add(icon);

        Label titleLbl = new Label(title);

        Label subLbl = new Label(sub);
        subLbl.getStyleClass().add("fluent-status-sub");
        if (sub.contains("✓")) {
            subLbl.setStyle("-fx-text-fill: -fx-green;");
        }

        card.getChildren().addAll(iconHolder, titleLbl, subLbl);
        return card;
    }

    /** Places tiles into the stretchy 3-column grid, row by row (always within the 3 constrained columns). */
    private void layoutTilesOnGrid(GridPane grid, List<VBox> tiles) {
        grid.getChildren().clear();
        int column = 0;
        for (VBox tile : tiles) {
            grid.add(tile, column % 3, column / 3);
            column++;
        }
    }

    private VBox createFluentTile(Node icon, String title, String desc, Runnable onClick) {
        VBox card = new VBox();
        card.getStyleClass().add("fluent-tile");
        card.setMaxWidth(Double.MAX_VALUE);

        HBox inner = new HBox(16);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.setMaxWidth(Double.MAX_VALUE);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("fluent-tile-icon-box");
        iconBox.getChildren().add(icon);

        VBox textCol = new VBox(4);
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("fluent-tile-title");

        Label descLbl = new Label(desc);
        descLbl.getStyleClass().add("fluent-tile-desc");
        descLbl.setWrapText(false);
        descLbl.setTextOverrun(OverrunStyle.ELLIPSIS);
        descLbl.setMaxWidth(Double.MAX_VALUE);
        Tooltip.install(descLbl, new Tooltip(desc));
        textCol.setMaxWidth(Double.MAX_VALUE);

        textCol.getChildren().addAll(titleLbl, descLbl);
        HBox.setHgrow(textCol, Priority.ALWAYS);
        inner.getChildren().addAll(iconBox, textCol);
        card.getChildren().add(inner);

        card.setOnMouseClicked(e -> onClick.run());
        return card;
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    // ==================================================
    // SETTINGS · ACCOUNT (change password, session)
    // ==================================================
    private void showSettingsPanel() {
        setBreadcrumb("Settings > Account");

        VBox mainBox = new VBox(16);
        mainBox.setPadding(new Insets(20, 36, 30, 36));

        mainBox.getChildren().add(buildSubScreenHeader(
                "Account Settings",
                "Manage your sign-in credentials and end your session."
        ));

        // 1. Account summary card
        VBox accountCard = new VBox(10);
        accountCard.getStyleClass().add("card");
        accountCard.setPadding(new Insets(18, 22, 16, 22));

        HBox identityRow = new HBox(14);
        identityRow.setAlignment(Pos.CENTER_LEFT);

        StackPane avatarPane = buildClickableAvatar(24);

        VBox identityMeta = new VBox(2);
        Label nameLbl = new Label(currentUser.getFullName());
        nameLbl.getStyleClass().add("card-title");
        Label idLbl = new Label("Student ID: " + currentUser.getStudentId());
        idLbl.getStyleClass().add("helper-text");
        identityMeta.getChildren().addAll(nameLbl, idLbl);

        Region identitySpacer = new Region();
        HBox.setHgrow(identitySpacer, Priority.ALWAYS);
        Label statusBadge = createVerificationBadge();

        identityRow.getChildren().addAll(avatarPane, identityMeta, identitySpacer, statusBadge);

        Label memberSince = new Label("Member since "
                + (currentUser.getCreatedAt() != null ? currentUser.getCreatedAt().toLocalDate() : "-"));
        memberSince.getStyleClass().add("helper-text");

        accountCard.getChildren().addAll(identityRow, memberSince);

        // 2. Verification card
        VBox verificationCard = new VBox(10);
        verificationCard.getStyleClass().add("card");
        verificationCard.setPadding(new Insets(18, 22, 16, 22));

        Label verifTitle = new Label("Account Verification");
        verifTitle.getStyleClass().add("card-title");
        Label verifDesc = new Label("Your student credentials and security badge.");
        verifDesc.getStyleClass().add("screen-subtitle");

        GridPane verifGrid = new GridPane(18, 8);
        String[] verifKeys = {"Student ID", "Full Name", "Role"};
        String[] verifValues = {
                currentUser.getStudentId(),
                currentUser.getFullName(),
                currentUser.getRole() == null ? null : currentUser.getRole().toString()
        };
        for (int i = 0; i < verifKeys.length; i++) {
            Label key = new Label(verifKeys[i]);
            key.getStyleClass().add("helper-text");
            Label val = new Label(verifValues[i] == null || verifValues[i].isBlank() ? "-" : verifValues[i]);
            val.setStyle("-fx-text-fill: -fx-ink; -fx-font-weight: 600;");
            verifGrid.add(key, 0, i);
            verifGrid.add(val, 1, i);
        }
        Label verifKey = new Label("Verification");
        verifKey.getStyleClass().add("helper-text");
        verifGrid.add(verifKey, 0, 3);
        verifGrid.add(createVerificationBadge(), 1, 3);

        Label verifNote = new Label();
        verifNote.getStyleClass().add("helper-text");
        verifNote.setWrapText(true);
        if (currentUser.getVerificationStatus() == com.networkfinder.model.VerificationStatus.APPROVED) {
            verifNote.setText("Your account is verified and participating in the campus social network graph.");
        } else if (currentUser.getVerificationStatus() == com.networkfinder.model.VerificationStatus.PENDING) {
            verifNote.setText("Your account is awaiting admin verification.");
        } else {
            verifNote.setText("Your account is not verified. Please contact the administrator.");
        }

        verificationCard.getChildren().addAll(verifTitle, verifDesc, verifGrid, verifNote);

        // 3. Change password card
        VBox passwordCard = new VBox(14);
        passwordCard.getStyleClass().add("card");
        passwordCard.setPadding(new Insets(18, 22, 20, 22));

        Label pwTitle = new Label("Change Password");
        pwTitle.getStyleClass().add("card-title");
        Label pwHint = new Label("At least 8 characters, with at least one letter and one number.");
        pwHint.getStyleClass().add("helper-text");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("form-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        VBox currentBox = new VBox(6);
        Label currentLbl = new Label("Current Password");
        currentLbl.getStyleClass().add("field-label");
        PasswordRevealField currentField = new PasswordRevealField("Enter your current password");
        currentBox.getChildren().addAll(currentLbl, currentField);

        VBox newBox = new VBox(6);
        Label newLbl = new Label("New Password");
        newLbl.getStyleClass().add("field-label");
        PasswordRevealField newField = new PasswordRevealField("At least 8 characters, letter + number");
        newBox.getChildren().addAll(newLbl, newField);

        VBox confirmBox = new VBox(6);
        Label confirmLbl = new Label("Confirm New Password");
        confirmLbl.getStyleClass().add("field-label");
        PasswordRevealField confirmField = new PasswordRevealField("Repeat your new password");
        confirmBox.getChildren().addAll(confirmLbl, confirmField);

        Button saveBtn = new Button("Update Password");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        Label successLabel = new Label("Your password was updated.");
        successLabel.setStyle("-fx-text-fill: -fx-green; -fx-font-weight: 600;");
        successLabel.setVisible(false);
        successLabel.setManaged(false);

        saveBtn.setOnAction(e -> {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            successLabel.setVisible(false);
            successLabel.setManaged(false);

            String current = currentField.getValue();
            String next = newField.getValue();
            String confirm = confirmField.getValue();

            if (current.isEmpty() || next.isEmpty() || confirm.isEmpty()) {
                setFormError(errorLabel, "Please fill in all password fields.");
                return;
            }
            if (!next.equals(confirm)) {
                setFormError(errorLabel, "The new passwords do not match.");
                return;
            }

            try {
                AuthService auth = new AuthService();
                AuthService.PasswordChangeResult result =
                        auth.changePassword(currentUser.getStudentId(), current, next);
                switch (result) {
                    case SUCCESS:
                        currentField.clear();
                        newField.clear();
                        confirmField.clear();
                        successLabel.setVisible(true);
                        successLabel.setManaged(true);
                        UiDialogs.showSuccess("Your password has been updated. Use it the next time you sign in.");
                        break;
                    case WRONG_CURRENT_PASSWORD:
                        setFormError(errorLabel, "Your current password is incorrect.");
                        break;
                    case UNKNOWN_USER:
                        setFormError(errorLabel, "Account not found. Please sign in again.");
                        break;
                    default:
                        UiDialogs.showError("Could not update the password. Please try again.");
                        break;
                }
            } catch (ValidationException ve) {
                setFormError(errorLabel, ve.getMessage());
            }
        });

        passwordCard.getChildren().addAll(pwTitle, pwHint, errorLabel, currentBox, newBox, confirmBox, saveBtn, successLabel);

        // 4. Session card
        VBox sessionCard = new VBox(12);
        sessionCard.getStyleClass().add("card");
        sessionCard.setPadding(new Insets(18, 22, 20, 22));

        Label sessionTitle = new Label("Session");
        sessionTitle.getStyleClass().add("card-title");
        Label sessionDesc = new Label("Signing out returns you to the student login screen. Unconfirmed connection requests stay saved in the network.");
        sessionDesc.getStyleClass().add("screen-subtitle");

        Button signOutBtn = new Button("Sign out");
        signOutBtn.getStyleClass().add("button-danger");
        signOutBtn.setOnAction(e -> ScreenNavigator.showStudentLogin());

        sessionCard.getChildren().addAll(sessionTitle, sessionDesc, signOutBtn);

        mainBox.getChildren().addAll(accountCard, verificationCard, passwordCard, sessionCard);
        contentArea.getChildren().setAll(mainBox);
    }

    private Label createVerificationBadge() {
        Label badge = new Label(currentUser.getVerificationStatus() == null
                ? "UNKNOWN" : currentUser.getVerificationStatus().name());
        if (currentUser.getVerificationStatus() == com.networkfinder.model.VerificationStatus.APPROVED) {
            badge.getStyleClass().add("badge-approved");
        } else if (currentUser.getVerificationStatus() == com.networkfinder.model.VerificationStatus.PENDING) {
            badge.getStyleClass().add("badge-pending");
        } else {
            badge.setStyle("-fx-padding: 3 10; -fx-background-radius: 999; -fx-font-size: 11px; "
                    + "-fx-font-weight: 700; -fx-text-fill: -fx-danger; -fx-background-color: rgba(239, 68, 68, 0.16);");
        }
        return badge;
    }

    private StackPane buildClickableAvatar(double radius) {
        StackPane pane = new StackPane();
        pane.setMaxSize(radius * 2, radius * 2);

        File photo = avatarFile();
        boolean photoLoaded = false;
        if (photo != null) {
            Image img = new Image(photo.toURI().toString(), radius * 2, radius * 2, true, true);
            if (!img.isError()) {
                ImageView view = new ImageView(img);
                view.setFitWidth(radius * 2);
                view.setFitHeight(radius * 2);
                view.setSmooth(true);
                view.setClip(new Circle(radius, radius, radius));
                Circle ring = new Circle(radius);
                ring.setFill(Color.TRANSPARENT);
                ring.setStroke(Color.color(1.0, 1.0, 1.0, 0.25));
                ring.setStrokeWidth(2);
                pane.getChildren().addAll(view, ring);
                photoLoaded = true;
            }
        }
        if (!photoLoaded) {
            Circle circle = new Circle(radius);
            circle.getStyleClass().add("fluent-avatar");
            Text initials = new Text(getInitials(currentUser.getFullName()));
            initials.getStyleClass().add("fluent-avatar-text");
            if (radius < 30) {
                initials.setStyle("-fx-font-size: 14px;");
            }
            pane.getChildren().addAll(circle, initials);
        }

        StackPane badge = new StackPane();
        badge.getStyleClass().add("fluent-avatar-badge");
        int badgeIcon = radius >= 36 ? 10 : 8;
        badge.getChildren().add(SvgIcons.icon(SvgIcons.Glyph.CAMERA, badgeIcon, "svg-icon-strong"));
        badge.setPrefSize(badgeIcon + 10, badgeIcon + 10);
        badge.setMaxSize(badgeIcon + 10, badgeIcon + 10);
        StackPane.setAlignment(badge, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(badge, new Insets(0, 3, 3, 0));
        pane.getChildren().add(badge);

        pane.getStyleClass().add("fluent-avatar-clickable");
        pane.setCursor(javafx.scene.Cursor.HAND);
        Tooltip.install(pane, new Tooltip("Click to add a profile photo (optional)"));
        pane.setOnMouseClicked(e -> chooseAvatarPhoto());
        return pane;
    }

    private void chooseAvatarPhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Profile Photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(ScreenNavigator.getPrimaryStage());
        if (file == null) return;

        String error = AuthService.validateIdCardImage(file);
        if (error != null) {
            UiDialogs.showError(error);
            return;
        }

        String name = file.getName().toLowerCase();
        String ext = name.endsWith(".png") ? "png" : (name.endsWith(".jpeg") ? "jpeg" : "jpg");
        Path target = Path.of(DatabaseManager.getUploadsDir(), "avatars",
                currentUser.getStudentId() + "." + ext);
        try {
            Files.createDirectories(target.getParent());
            clearAvatarFiles();
            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            showHomeScreen();
        } catch (Exception ex) {
            UiDialogs.showError("Could not save the photo. Please choose a different image and try again.");
        }
    }

    private File avatarFile() {
        File dir = new File(DatabaseManager.getUploadsDir(), "avatars");
        File[] matches = dir.listFiles((d, name) ->
                name.startsWith(currentUser.getStudentId() + ".")
                        && (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")));
        return (matches != null && matches.length > 0) ? matches[0] : null;
    }

    private void clearAvatarFiles() {
        File dir = new File(DatabaseManager.getUploadsDir(), "avatars");
        File[] existing = dir.listFiles((d, name) -> name.startsWith(currentUser.getStudentId() + "."));
        if (existing != null) {
            for (File f : existing) {
                f.delete();
            }
        }
    }

    private StringConverter<StudentNode> studentNodeConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(StudentNode node) {
                if (node == null) return null;
                String section = node.getProfile() != null && node.getProfile().getSection() != null
                        ? node.getProfile().getSection() : "";
                return node.getFullName() + " [" + node.getStudentId() + "]"
                        + (section.isBlank() ? "" : "  •  " + section);
            }

            @Override
            public StudentNode fromString(String string) {
                return null;
            }
        };
    }

    private void setFormError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private Node buildSubScreenHeader(String title, String subtitle) {
        VBox header = new VBox(6);
        header.setPadding(new Insets(10, 0, 16, 0));

        Label heading = new Label(title);
        heading.getStyleClass().add("screen-heading");

        Label sub = new Label(subtitle);
        sub.getStyleClass().add("screen-subtitle");

        header.getChildren().addAll(heading, sub);
        return header;
    }

    // ==================================================
    // 1. QUERY 1 — STUDY GROUP MATCHER (Level-Order BFS)
    // ==================================================
    private void showStudyGroupMatcherScreen() {
        setBreadcrumb("Settings > Study Group Matcher");

        VBox mainBox = new VBox(16);
        mainBox.setPadding(new Insets(20, 36, 30, 36));

        Node header = buildSubScreenHeader(
                "Study Group Matcher",
                "Discovers compatible peers using Level-Order BFS traversal grouped by social distance."
        );

        // Filter Bar Card
        HBox filterCard = new HBox(12);
        filterCard.getStyleClass().add("card");
        filterCard.setAlignment(Pos.CENTER_LEFT);
        filterCard.setPadding(new Insets(14, 18, 14, 18));

        TextField nameSearchField = new TextField();
        nameSearchField.setPromptText("Filter peers by name or ID...");
        nameSearchField.getStyleClass().add("realtime-search-field");
        HBox.setHgrow(nameSearchField, Priority.ALWAYS);

        ComboBox<String> subjectFilter = new ComboBox<>();
        subjectFilter.setPromptText("Filter by Subject");
        subjectFilter.getItems().addAll("All Subjects", "Programming", "Data Structures", "Database Management",
                "Networking", "Web Development", "Mobile Development", "Mathematics", "Cybersecurity", "Software Engineering");
        subjectFilter.setValue("All Subjects");

        CheckBox sameSectionCheck = new CheckBox("Same Section");
        sameSectionCheck.setStyle("-fx-text-fill: -fx-ink; -fx-font-weight: 500;");

        ComboBox<String> groupSizeFilter = new ComboBox<>();
        groupSizeFilter.getItems().addAll("Any Group Size", "Small Group", "Medium Group", "Large Group");
        groupSizeFilter.setValue("Any Group Size");

        Button searchBtn = new Button("Search Peers");
        searchBtn.getStyleClass().add("button-primary");

        filterCard.getChildren().addAll(
                nameSearchField,
                new Label("Filters:"),
                subjectFilter,
                sameSectionCheck,
                groupSizeFilter,
                searchBtn
        );

        VBox resultsBox = new VBox(14);

        Runnable[] searchRef = new Runnable[1];
        Runnable executeSearch = () -> {
            resultsBox.getChildren().clear();

            MatchFilter filter = new MatchFilter();
            if (!"All Subjects".equals(subjectFilter.getValue())) {
                filter.setRequiredSubject(subjectFilter.getValue());
            }
            if (sameSectionCheck.isSelected()) {
                filter.setSameSectionOnly(true);
            }
            if (!"Any Group Size".equals(groupSizeFilter.getValue())) {
                filter.setRequiredGroupSize(groupSizeFilter.getValue());
            }

            List<MatchResult> networkMatches = networkService.matchStudyGroups(currentUser.getStudentId(), filter);
            List<MatchResult> campusMatches = networkService.matchCampusPeers(currentUser.getStudentId(), filter);

            String nameQuery = (nameSearchField.getText() == null) ? "" : nameSearchField.getText().trim().toLowerCase();
            if (!nameQuery.isEmpty()) {
                networkMatches = networkMatches.stream()
                        .filter(m -> (m.getStudent().getFullName() != null && m.getStudent().getFullName().toLowerCase().contains(nameQuery))
                                || (m.getStudent().getStudentId() != null && m.getStudent().getStudentId().toLowerCase().contains(nameQuery)))
                        .toList();
                campusMatches = campusMatches.stream()
                        .filter(m -> (m.getStudent().getFullName() != null && m.getStudent().getFullName().toLowerCase().contains(nameQuery))
                                || (m.getStudent().getStudentId() != null && m.getStudent().getStudentId().toLowerCase().contains(nameQuery)))
                        .toList();
            }

            if (networkMatches.isEmpty() && campusMatches.isEmpty()) {
                VBox empty = new VBox(10);
                empty.setAlignment(Pos.CENTER);
                empty.getStyleClass().add("card");
                empty.setPadding(new Insets(30));
                Label emptyLbl = new Label("No matching study partners found on campus with these criteria.");
                emptyLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-ink-soft;");
                empty.getChildren().add(emptyLbl);
                resultsBox.getChildren().add(empty);
                return;
            }

            if (!networkMatches.isEmpty()) {
                Label netCountLabel = new Label("Network Connections (" + networkMatches.size() + " matches by BFS traversal tier):");
                netCountLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-ink; -fx-font-size: 13px;");
                resultsBox.getChildren().add(netCountLabel);

                Map<String, List<MatchResult>> tiers = new LinkedHashMap<>();
                for (MatchResult m : networkMatches) {
                    tiers.computeIfAbsent(m.getDegreeLabel(), k -> new ArrayList<>()).add(m);
                }
                for (Map.Entry<String, List<MatchResult>> tier : tiers.entrySet()) {
                    Label tierHeader = new Label(tier.getKey());
                    tierHeader.getStyleClass().add("badge-degree");
                    tierHeader.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
                    resultsBox.getChildren().add(tierHeader);
                    List<Node> tierCards = new ArrayList<>();
                    for (MatchResult m : tier.getValue()) {
                        tierCards.add(createMatchCard(m));
                    }
                    addPagedMatchCards(resultsBox, "tier:" + tier.getKey(), tierCards, 3, searchRef[0]);
                }
            } else {
                VBox infoBox = new VBox(6);
                infoBox.getStyleClass().add("info-card");
                Label infoLbl1 = new Label("No Extended Network Connections Found (Isolated Node)");
                infoLbl1.setGraphic(SvgIcons.icon(SvgIcons.Glyph.INFO, 15, "svg-icon-accent"));
                infoLbl1.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-accent;");
                Label infoLbl2 = new Label("Connect with recommended campus classmates below to start expanding your graph.");
                infoLbl2.setStyle("-fx-text-fill: -fx-ink-soft; -fx-font-size: 12px;");
                infoBox.getChildren().addAll(infoLbl1, infoLbl2);
                resultsBox.getChildren().add(infoBox);
            }

            if (!campusMatches.isEmpty()) {
                Label campusCountLabel = new Label("Campus Peer Recommendations (" + campusMatches.size() + " potential partners by profile match):");
                campusCountLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-ink; -fx-font-size: 13px; -fx-padding: 10 0 0 0;");
                resultsBox.getChildren().add(campusCountLabel);
                List<Node> campusCards = new ArrayList<>();
                for (MatchResult m : campusMatches) {
                    campusCards.add(createMatchCard(m));
                }
                addPagedMatchCards(resultsBox, "campus", campusCards, 3, searchRef[0]);
            }
        };
        searchRef[0] = executeSearch;

        nameSearchField.textProperty().addListener((obs, oldVal, newVal) -> executeSearch.run());
        subjectFilter.setOnAction(e -> executeSearch.run());
        sameSectionCheck.setOnAction(e -> executeSearch.run());
        groupSizeFilter.setOnAction(e -> executeSearch.run());
        searchBtn.setOnAction(e -> executeSearch.run());
        executeSearch.run();

        mainBox.getChildren().addAll(header, filterCard, resultsBox);
        contentArea.getChildren().setAll(mainBox);
    }

    private VBox createMatchCard(MatchResult match) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(match.getStudent().getFullName());
        name.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -fx-ink;");

        Label id = new Label("(" + match.getStudent().getStudentId() + ")");
        id.setStyle("-fx-text-fill: -fx-ink-soft;");

        Label degreeBadge = new Label(match.getDegreeLabel());
        degreeBadge.getStyleClass().add("badge-degree");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label scoreBadge = new Label("Score: " + match.getCompatibilityScore() + " pts");
        scoreBadge.getStyleClass().add("badge-score");

        topRow.getChildren().addAll(name, id, degreeBadge, spacer, scoreBadge);

        StudentProfile p = match.getStudent().getProfile();
        HBox metaRow = new HBox(16);
        Label courseLbl = new Label("Course: " + (p.getCourse() != null ? p.getCourse() : "N/A"));
        courseLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-ink-soft;");

        Label secLbl = new Label("Section: " + (p.getSection() != null ? p.getSection() : "N/A") + (match.isSameSection() ? " (Classmate!)" : ""));
        secLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (match.isSameSection() ? "-fx-accent;" : "-fx-ink-soft;") + "-fx-font-weight: " + (match.isSameSection() ? "600;" : "400;"));

        Label sizeLbl = new Label("Group Size: " + (p.getGroupSizePreference() != null ? p.getGroupSizePreference() : "Flexible"));
        sizeLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-ink-soft;");

        metaRow.getChildren().addAll(courseLbl, secLbl, sizeLbl);

        HBox sharedRow = new HBox(10);
        sharedRow.setAlignment(Pos.CENTER_LEFT);

        Label sharedTitle = new Label("Shared Subjects:");
        sharedTitle.setStyle("-fx-font-weight: 600; -fx-font-size: 12px; -fx-text-fill: -fx-ink;");

        FlowPane chips = new FlowPane(6, 6);
        for (String sub : match.getCommonSubjects()) {
            Label chip = new Label(sub);
            chip.getStyleClass().add("chip-selected");
            chip.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
            chips.getChildren().add(chip);
        }

        sharedRow.getChildren().addAll(sharedTitle, chips);

        HBox actionRow = new HBox();
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        String currentId = currentUser.getStudentId();
        String targetId = match.getStudent().getStudentId();
        boolean isDirect = networkService.hasConnection(currentId, targetId);

        com.networkfinder.model.Connection connRecord = networkService.getConnectionBetween(currentId, targetId);

        Button actionBtn;
        if (isDirect) {
            actionBtn = new Button("✓ Connected");
            actionBtn.getStyleClass().add("button-secondary");
            actionBtn.setDisable(true);
        } else if (connRecord != null && connRecord.getStatus() == com.networkfinder.model.ConnectionStatus.PENDING) {
            if (currentId.equals(connRecord.getRequesterId())) {
                actionBtn = new Button("Request Sent");
                actionBtn.setGraphic(SvgIcons.icon(SvgIcons.Glyph.HOURGLASS, 13, "svg-icon"));
                actionBtn.getStyleClass().add("button-secondary");
                actionBtn.setDisable(true);
            } else {
                actionBtn = new Button("✓ Accept Request");
                actionBtn.getStyleClass().add("button-primary");
                actionBtn.setOnAction(e -> {
                    if (networkService.acceptConnectionRequest(targetId, currentId)) {
                        showStudyGroupMatcherScreen();
                    } else {
                        UiDialogs.showError("Could not accept the request. It may have been withdrawn by the sender.");
                    }
                });
            }
        } else {
            actionBtn = new Button("+ Send Request");
            actionBtn.getStyleClass().add("button-primary");
            actionBtn.setOnAction(e -> {
                if (networkService.sendConnectionRequest(currentId, targetId)) {
                    showStudyGroupMatcherScreen();
                } else {
                    UiDialogs.showError("Could not send the request. This student may already be connected to you, already have a pending invitation, or is not available.");
                }
            });
        }
        actionRow.getChildren().add(actionBtn);

        card.getChildren().addAll(topRow, metaRow, sharedRow, actionRow);
        return card;
    }

    private void addPagedMatchCards(VBox container, String stateKey, List<Node> cards, int pageSize, Runnable refresh) {
        int totalPages = Math.max(1, (int) Math.ceil((double) cards.size() / pageSize));
        int page = Math.max(1, Math.min(matcherPageState.getOrDefault(stateKey, 1), totalPages));
        matcherPageState.put(stateKey, page);

        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, cards.size());
        if (from < to) {
            container.getChildren().addAll(cards.subList(from, to));
        }

        if (totalPages > 1) {
            HBox bar = new HBox(12);
            bar.getStyleClass().add("pagination-container");
            bar.setAlignment(Pos.CENTER);

            Button prevBtn = new Button("← Previous");
            prevBtn.getStyleClass().add("pagination-btn");
            Label pageInfo = new Label("Page " + page + " of " + totalPages + " (" + cards.size() + " total)");
            pageInfo.getStyleClass().add("pagination-info");
            Button nextBtn = new Button("Next →");
            nextBtn.getStyleClass().add("pagination-btn");

            prevBtn.setDisable(page <= 1);
            nextBtn.setDisable(page >= totalPages);
            prevBtn.setOnAction(e -> {
                matcherPageState.put(stateKey, page - 1);
                refresh.run();
            });
            nextBtn.setOnAction(e -> {
                matcherPageState.put(stateKey, page + 1);
                refresh.run();
            });

            bar.getChildren().addAll(prevBtn, pageInfo, nextBtn);
            container.getChildren().add(bar);
        }
    }

    // ==================================================
    // 2. QUERY 2 — PATHFINDER (Shortest Path BFS)
    // ==================================================
    private void showPathfinderScreen() {
        setBreadcrumb("Settings > Pathfinder (BFS)");

        VBox mainBox = new VBox(18);
        mainBox.setPadding(new Insets(20, 36, 30, 36));

        Node header = buildSubScreenHeader(
                "Pathfinder (Shortest Path BFS)",
                "Traces the shortest introduction chain and degrees of separation between students using BFS traversal."
        );

        // Selector Card
        VBox selectorCard = new VBox(14);
        selectorCard.getStyleClass().add("card");

        HBox pickers = new HBox(20);
        pickers.setAlignment(Pos.BOTTOM_LEFT);

        // Source
        VBox srcBox = new VBox(6);
        Label srcLabel = new Label("Source Student (Starting Node)");
        srcLabel.getStyleClass().add("field-label");
        ComboBox<StudentNode> srcCombo = new ComboBox<>();
        srcCombo.getItems().addAll(networkService.getGraph().getAllNodes());
        StudentNode currentN = networkService.getGraph().getNode(currentUser.getStudentId());
        if (currentN != null) srcCombo.setValue(currentN);
        srcCombo.setMinWidth(260);
        srcCombo.setMaxWidth(Double.MAX_VALUE);
        srcBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(srcBox, Priority.ALWAYS);
        srcBox.getChildren().addAll(srcLabel, srcCombo);

        // Target
        VBox tgtBox = new VBox(6);
        Label tgtLabel = new Label("Target Student (Destination Node)");
        tgtLabel.getStyleClass().add("field-label");
        ComboBox<StudentNode> tgtCombo = new ComboBox<>();
        tgtCombo.getItems().addAll(networkService.getGraph().getAllNodes());
        for (StudentNode n : networkService.getGraph().getAllNodes()) {
            if (!n.getStudentId().equals(currentUser.getStudentId())) {
                tgtCombo.setValue(n);
                break;
            }
        }
        tgtCombo.setMinWidth(260);
        tgtCombo.setMaxWidth(Double.MAX_VALUE);
        tgtBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tgtBox, Priority.ALWAYS);
        tgtBox.getChildren().addAll(tgtLabel, tgtCombo);

        Button runBfsBtn = new Button("Run BFS Pathfinder");
        runBfsBtn.getStyleClass().add("button-primary");
        runBfsBtn.setStyle("-fx-padding: 10 20;");
        runBfsBtn.setDefaultButton(true);

        pickers.getChildren().addAll(srcBox, tgtBox, runBfsBtn);

        Label hintLabel = new Label("Interactive Canvas: Click any node on the graph below to choose Source or Target.");
        hintLabel.setGraphic(SvgIcons.icon(SvgIcons.Glyph.LIGHTBULB, 14, "svg-icon-accent"));
        hintLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-accent; -fx-font-weight: 500;");

        selectorCard.getChildren().addAll(pickers, hintLabel);

        // Results Card
        VBox resultsCard = new VBox(14);
        resultsCard.getStyleClass().add("card");
        resultsCard.setVisible(false);

        // Interactive Canvas Card
        VBox canvasCard = new VBox(10);
        canvasCard.getStyleClass().add("card");
        Label canvasTitle = new Label("Interactive Network Graph");
        canvasTitle.getStyleClass().add("card-title");
        GraphVisualizerCanvas canvas = new GraphVisualizerCanvas(networkService.getGraph());
        canvasCard.getChildren().addAll(canvasTitle, canvas);

        canvas.setOnNodeClicked(clickedNode -> {
            StudentNode currentSrc = srcCombo.getValue();
            if (currentSrc == null || currentSrc.getStudentId().equals(clickedNode.getStudentId())) {
                srcCombo.setValue(clickedNode);
            } else {
                tgtCombo.setValue(clickedNode);
                runBfsBtn.fire();
            }
        });

        runBfsBtn.setOnAction(e -> {
            StudentNode src = srcCombo.getValue();
            StudentNode tgt = tgtCombo.getValue();
            if (src == null || tgt == null) return;

            PathResult result = networkService.findShortestPath(src.getStudentId(), tgt.getStudentId());
            renderBfsResult(resultsCard, src, tgt, result);
            canvas.setHighlightedPath(result.getPath());
            resultsCard.setVisible(true);
        });

        mainBox.getChildren().addAll(header, selectorCard, resultsCard, canvasCard);
        contentArea.getChildren().setAll(mainBox);
    }

    private void renderBfsResult(VBox card, StudentNode src, StudentNode tgt, PathResult result) {
        card.getChildren().clear();

        Label resTitle = new Label("BFS Execution Results");
        resTitle.getStyleClass().add("card-title");

        if (!result.isReachable()) {
            Label unreach = new Label("No connection path exists between " + src.getFullName() + " and " + tgt.getFullName() + ".");
            unreach.setStyle("-fx-text-fill: -fx-danger; -fx-font-weight: 600;");
            Label stats = new Label("Total nodes explored during BFS traversal: " + result.getNodesExplored());
            stats.setStyle("-fx-text-fill: -fx-ink-soft;");
            card.getChildren().addAll(resTitle, unreach, stats);
            return;
        }

        HBox metrics = new HBox(14);
        VBox pillSep = createMetricPill("Degrees of Separation", String.valueOf(result.getDegreesOfSeparation()), "-fx-accent");
        VBox pillHops = createMetricPill("Path Length (Hops)", String.valueOf(result.getPath().size() - 1), "-fx-green");
        VBox pillExplored = createMetricPill("Nodes Explored (BFS)", String.valueOf(result.getNodesExplored()), "-fx-ink-soft");
        for (VBox pill : new VBox[]{pillSep, pillHops, pillExplored}) {
            HBox.setHgrow(pill, Priority.ALWAYS);
            pill.setMaxWidth(Double.MAX_VALUE);
        }
        metrics.getChildren().addAll(pillSep, pillHops, pillExplored);

        VBox chainBox = new VBox(8);
        Label chainTitle = new Label("Shortest Introduction Chain:");
        chainTitle.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-ink;");

        HBox nodesRow = new HBox(8);
        nodesRow.setAlignment(Pos.CENTER_LEFT);

        List<String> path = result.getPath();
        for (int i = 0; i < path.size(); i++) {
            String sid = path.get(i);
            StudentNode node = networkService.getGraph().getNode(sid);
            String labelText = (node != null ? node.getFullName() : sid) + "\n(" + sid + ")";

            Button nodePill = new Button(labelText);
            if (i == 0) {
                nodePill.getStyleClass().addAll("node-pill", "node-pill-source");
            } else if (i == path.size() - 1) {
                nodePill.getStyleClass().addAll("node-pill", "node-pill-target");
            } else {
                nodePill.getStyleClass().addAll("node-pill", "node-pill-mid");
            }

            nodesRow.getChildren().add(nodePill);

            if (i < path.size() - 1) {
                Label arrow = new Label("➔");
                arrow.setStyle("-fx-font-size: 16px; -fx-text-fill: -fx-accent; -fx-font-weight: 700;");
                nodesRow.getChildren().add(arrow);
            }
        }

        Label explanation = new Label("Algorithm Note: Standard BFS guarantees the minimum degree of separation in an unweighted graph with time complexity O(V + E).");
        explanation.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-ink-soft; -fx-font-style: italic;");

        card.getChildren().addAll(resTitle, metrics, chainTitle, nodesRow, explanation);
    }

    private VBox createMetricPill(String title, String value, String colorStyle) {
        VBox box = new VBox(2);
        box.getStyleClass().add("metric-pill");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: " + colorStyle + ";");
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-ink-soft; -fx-font-weight: 500;");
        box.getChildren().addAll(v, t);
        return box;
    }

    // ==================================================
    // 3. QUERY 3 — MY CONNECTIONS & MUTUAL CONNECTIONS
    // ==================================================
    private void showConnectionsScreen() {
        setBreadcrumb("Settings > My Connections");

        VBox mainBox = new VBox(18);
        mainBox.setPadding(new Insets(20, 36, 30, 36));

        Node header = buildSubScreenHeader(
                "My Connections & Mutuals",
                "Manage your network links, accept pending connection invitations, and inspect mutual peers."
        );
        mainBox.getChildren().add(header);

        Set<String> neighbors = networkService.getGraph().getNeighbors(currentUser.getStudentId());

        // 1. Incoming Requests
        List<com.networkfinder.model.Connection> incoming = networkService.getIncomingRequests(currentUser.getStudentId());
        if (!incoming.isEmpty()) {
            VBox reqCard = new VBox(12);
            reqCard.getStyleClass().add("card");

            Label reqTitle = new Label("Incoming Requests (" + incoming.size() + ")");
            reqTitle.getStyleClass().add("card-title");
            reqTitle.setStyle("-fx-text-fill: -fx-accent;");
            reqTitle.setGraphic(SvgIcons.icon(SvgIcons.Glyph.INBOX, 16, "svg-icon-accent"));
            reqCard.getChildren().add(reqTitle);

            for (com.networkfinder.model.Connection c : incoming) {
                StudentNode sNode = networkService.getGraph().getNode(c.getRequesterId());
                HBox reqRow = new HBox(12);
                reqRow.setAlignment(Pos.CENTER_LEFT);
                reqRow.getStyleClass().add("request-row");

                Label sName = new Label(sNode != null ? sNode.getFullName() : c.getRequesterId());
                sName.setStyle("-fx-font-weight: 600; -fx-min-width: 180; -fx-text-fill: -fx-ink;");

                Label sSec = new Label(sNode != null && sNode.getProfile() != null ? sNode.getProfile().getSection() : "");
                sSec.setStyle("-fx-text-fill: -fx-ink-soft; -fx-min-width: 120;");

                Region rSpacer = new Region();
                HBox.setHgrow(rSpacer, Priority.ALWAYS);

                Button acceptBtn = new Button("✓ Accept");
                acceptBtn.getStyleClass().add("button-primary");
                acceptBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12;");
                acceptBtn.setOnAction(e -> {
                    if (networkService.acceptConnectionRequest(c.getRequesterId(), c.getReceiverId())) {
                        showConnectionsScreen();
                    } else {
                        UiDialogs.showError("Could not accept this request. It may have already been handled.");
                    }
                });

                Button declineBtn = new Button("✕ Decline");
                declineBtn.getStyleClass().add("button-secondary");
                declineBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12; -fx-text-fill: -fx-danger;");
                declineBtn.setOnAction(e -> {
                    if (networkService.declineConnectionRequest(c.getRequesterId(), c.getReceiverId())) {
                        showConnectionsScreen();
                    } else {
                        UiDialogs.showError("Could not decline this request. It may have already been handled.");
                    }
                });

                reqRow.getChildren().addAll(sName, sSec, rSpacer, acceptBtn, declineBtn);
                reqCard.getChildren().add(reqRow);
            }
            mainBox.getChildren().add(reqCard);
        }

        // 2. Outgoing Requests
        List<com.networkfinder.model.Connection> outgoing = networkService.getOutgoingRequests(currentUser.getStudentId());
        Set<String> outgoingIds = new HashSet<>();
        for (com.networkfinder.model.Connection c : outgoing) {
            outgoingIds.add(c.getReceiverId());
        }

        if (!outgoing.isEmpty()) {
            VBox outCard = new VBox(12);
            outCard.getStyleClass().add("card");
            Label outTitle = new Label("Outgoing Requests (" + outgoing.size() + ")");
            outTitle.getStyleClass().add("card-title");
            outTitle.setStyle("-fx-text-fill: -fx-amber;");
            outTitle.setGraphic(SvgIcons.icon(SvgIcons.Glyph.HOURGLASS, 16, "svg-icon-warn"));
            outCard.getChildren().add(outTitle);

            for (com.networkfinder.model.Connection c : outgoing) {
                StudentNode sNode = networkService.getGraph().getNode(c.getReceiverId());
                HBox outRow = new HBox(12);
                outRow.setAlignment(Pos.CENTER_LEFT);
                outRow.getStyleClass().add("request-row-outgoing");

                Label sName = new Label(sNode != null ? sNode.getFullName() : c.getReceiverId());
                sName.setStyle("-fx-font-weight: 600; -fx-min-width: 180; -fx-text-fill: -fx-ink;");

                Label sSec = new Label(sNode != null && sNode.getProfile() != null ? sNode.getProfile().getSection() : "");
                sSec.setStyle("-fx-text-fill: -fx-ink-soft; -fx-min-width: 120;");

                Label statusLbl = new Label("Awaiting student confirmation");
                statusLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-accent; -fx-font-style: italic;");

                Region rSpacer = new Region();
                HBox.setHgrow(rSpacer, Priority.ALWAYS);

                Button cancelBtn = new Button("✕ Cancel");
                cancelBtn.getStyleClass().add("button-secondary");
                cancelBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12;");
                cancelBtn.setOnAction(e -> {
                    if (networkService.declineConnectionRequest(currentUser.getStudentId(), c.getReceiverId())) {
                        showConnectionsScreen();
                    } else {
                        UiDialogs.showError("Could not cancel this request. It may have already been handled.");
                    }
                });

                outRow.getChildren().addAll(sName, sSec, statusLbl, rSpacer, cancelBtn);
                outCard.getChildren().add(outRow);
            }
            mainBox.getChildren().add(outCard);
        }

        // 3. Connect with Classmates Card
        VBox connectCard = new VBox(12);
        connectCard.getStyleClass().add("card");
        Label connectTitle = new Label("Connect with Classmates");
        connectTitle.getStyleClass().add("card-title");
        connectTitle.setGraphic(SvgIcons.icon(SvgIcons.Glyph.PLUS, 16, "svg-icon-accent"));

        Label connectDesc = new Label("Select any registered classmate on campus to send a connection request and link your graphs:");
        connectDesc.setStyle("-fx-text-fill: -fx-ink-soft; -fx-font-size: 13px;");

        List<StudentNode> connectable = new ArrayList<>();
        for (StudentNode n : networkService.getGraph().getAllNodes()) {
            String nid = n.getStudentId();
            if (!nid.equals(currentUser.getStudentId()) && !neighbors.contains(nid) && !outgoingIds.contains(nid)) {
                connectable.add(n);
            }
        }

        HBox connectRow = new HBox(12);
        connectRow.setAlignment(Pos.CENTER_LEFT);

        ComboBox<StudentNode> classmatePicker = new ComboBox<>();
        classmatePicker.getItems().addAll(connectable);
        classmatePicker.setConverter(studentNodeConverter());
        classmatePicker.setPromptText(connectable.isEmpty() ? "No new classmates available" : "Select classmate...");
        classmatePicker.setMinWidth(320);
        classmatePicker.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(classmatePicker, Priority.ALWAYS);
        classmatePicker.setDisable(connectable.isEmpty());

        Button sendReqBtn = new Button("+ Send Request");
        sendReqBtn.getStyleClass().add("button-primary");
        sendReqBtn.setDisable(connectable.isEmpty());

        sendReqBtn.setOnAction(e -> {
            StudentNode target = classmatePicker.getValue();
            if (target != null) {
                if (networkService.sendConnectionRequest(currentUser.getStudentId(), target.getStudentId())) {
                    showConnectionsScreen();
                } else {
                    UiDialogs.showError("Could not send the request. This classmate may already be connected to you, already have a pending invitation, or is not available.");
                }
            }
        });

        connectRow.getChildren().addAll(classmatePicker, sendReqBtn);

        Separator mutualSeparator = new Separator();

        Label mTitle = new Label("Mutual Connections Finder");
        mTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -fx-ink;");

        Label mDesc = new Label("Pick any student on campus, then click Find Mutuals — we list the direct connections you both share. "
                + "Example: if you and Maria both know Ana, Ana shows up as a mutual friend and can introduce you.");
        mDesc.setStyle("-fx-text-fill: -fx-ink-soft; -fx-font-size: 12px;");
        mDesc.setWrapText(true);

        HBox mInputRow = new HBox(12);
        mInputRow.setAlignment(Pos.CENTER_LEFT);
        Label pickLabel = new Label("Compare with:");
        ComboBox<StudentNode> compCombo = new ComboBox<>();
        for (StudentNode n : networkService.getGraph().getAllNodes()) {
            if (!n.getStudentId().equals(currentUser.getStudentId())) {
                compCombo.getItems().add(n);
            }
        }
        compCombo.setConverter(studentNodeConverter());
        compCombo.setMinWidth(260);
        compCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(compCombo, Priority.ALWAYS);

        Button compBtn = new Button("Find Mutuals");
        compBtn.getStyleClass().add("button-primary");

        mInputRow.getChildren().addAll(pickLabel, compCombo, compBtn);

        VBox mutualResults = new VBox(8);
        mutualResults.setVisible(false);
        mutualResults.setManaged(false);

        compBtn.setOnAction(e -> {
            StudentNode other = compCombo.getValue();
            if (other == null) return;

            List<String> mutuals = networkService.getMutualConnections(currentUser.getStudentId(), other.getStudentId());
            mutualResults.getChildren().clear();

            if (mutuals.isEmpty()) {
                Label noM = new Label("No mutual friends with " + other.getFullName() + " yet.");
                noM.setStyle("-fx-text-fill: -fx-ink-soft;");
                mutualResults.getChildren().add(noM);
            } else {
                Label mCount = new Label("Mutual Friends (" + mutuals.size() + "):");
                mCount.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-ink;");
                mutualResults.getChildren().add(mCount);

                FlowPane chips = new FlowPane(8, 8);
                for (String mId : mutuals) {
                    StudentNode mNode = networkService.getGraph().getNode(mId);
                    Label chip = new Label(mNode != null ? mNode.getFullName() : mId);
                    chip.setGraphic(SvgIcons.icon(SvgIcons.Glyph.USER, 12, "svg-icon"));
                    chip.getStyleClass().add("chip-selected");
                    chips.getChildren().add(chip);
                }
                mutualResults.getChildren().add(chips);
            }
            mutualResults.setVisible(true);
            mutualResults.setManaged(true);
        });

        connectCard.getChildren().addAll(connectTitle, connectDesc, connectRow, mutualSeparator, mTitle, mDesc, mInputRow, mutualResults);
        mainBox.getChildren().add(connectCard);

        // 4. Direct Connections List Card
        VBox connCard = new VBox(12);
        connCard.getStyleClass().add("card");
        Label connTitle = new Label("Direct Connections (" + neighbors.size() + ")");
        connTitle.getStyleClass().add("card-title");
        connCard.getChildren().add(connTitle);

        if (neighbors.isEmpty()) {
            Label noConn = new Label("You have no direct connections yet. Use the picker above or the Matcher to connect!");
            noConn.setStyle("-fx-text-fill: -fx-ink-soft;");
            connCard.getChildren().add(noConn);
        } else {
            for (String nId : neighbors) {
                StudentNode n = networkService.getGraph().getNode(nId);
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-border-color: -fx-border; -fx-border-width: 0 0 1 0; -fx-padding: 8 0;");

                Label name = new Label(n != null ? n.getFullName() : nId);
                name.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-min-width: 200; -fx-text-fill: -fx-ink;");

                Label sec = new Label(n != null && n.getProfile() != null ? n.getProfile().getSection() : "");
                sec.setStyle("-fx-text-fill: -fx-ink-soft; -fx-min-width: 120;");

                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);

                Button disconnectBtn = new Button("Disconnect");
                disconnectBtn.getStyleClass().add("button-secondary");
                disconnectBtn.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-danger;");
                disconnectBtn.setOnAction(e -> {
                    if (networkService.removeConnection(currentUser.getStudentId(), nId)) {
                        showConnectionsScreen();
                    } else {
                        UiDialogs.showError("Could not remove this connection.");
                    }
                });

                row.getChildren().addAll(name, sec, sp, disconnectBtn);
                connCard.getChildren().add(row);
            }
        }
        mainBox.getChildren().add(connCard);

        contentArea.getChildren().setAll(mainBox);
    }

    // ==================================================
    // 4. NETWORK ANALYTICS & DEGREE CENTRALITY
    // ==================================================
    private void showAnalyticsScreen() {
        setBreadcrumb("Settings > Network Graph & Hubs");

        VBox mainBox = new VBox(18);
        mainBox.setPadding(new Insets(20, 36, 30, 36));

        Node header = buildSubScreenHeader(
                "Network Graph Analytics & Hubs",
                "Calculates Degree Centrality to identify key study group leaders and network coordinators."
        );

        HBox stats = new HBox(14);
        VBox pillV = createMetricPill("Total Students (V)", String.valueOf(networkService.getGraph().getVertexCount()), "-fx-accent");
        VBox pillE = createMetricPill("Total Connections (E)", String.valueOf(networkService.getGraph().getEdgeCount()), "-fx-green");
        VBox pillDeg = createMetricPill("Your Degree", String.valueOf(networkService.getGraph().getDegree(currentUser.getStudentId())), "-fx-accent");
        VBox pillCent = createMetricPill("Your Centrality", String.format("%.2f", networkService.getCentrality(currentUser.getStudentId())), "-fx-accent-hover");
        for (VBox pill : new VBox[]{pillV, pillE, pillDeg, pillCent}) {
            HBox.setHgrow(pill, Priority.ALWAYS);
            pill.setMaxWidth(Double.MAX_VALUE);
        }
        stats.getChildren().addAll(pillV, pillE, pillDeg, pillCent);

        // Top Coordinators
        VBox hubsCard = new VBox(12);
        hubsCard.getStyleClass().add("card");
        Label hubsTitle = new Label("Top Study Group Coordinators (Highest Degree Centrality)");
        hubsTitle.getStyleClass().add("card-title");

        List<StudentNode> topNodes = networkService.getTopCoordinators(5);
        VBox topList = new VBox(8);
        for (int i = 0; i < topNodes.size(); i++) {
            StudentNode node = topNodes.get(i);
            int deg = networkService.getGraph().getDegree(node.getStudentId());
            double cent = networkService.getCentrality(node.getStudentId());

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color: -fx-border; -fx-border-width: 0 0 1 0; -fx-padding: 8 0;");

            Label rank = new Label("#" + (i + 1));
            rank.setStyle("-fx-font-weight: 700; -fx-text-fill: -fx-accent; -fx-min-width: 30;");

            Label name = new Label(node.getFullName() + " (" + node.getStudentId() + ")");
            name.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-ink;");
            HBox.setHgrow(name, Priority.ALWAYS);
            name.setMaxWidth(Double.MAX_VALUE);

            Label degLbl = new Label(deg + " connection(s)");
            degLbl.setStyle("-fx-text-fill: -fx-accent; -fx-font-weight: 500; -fx-min-width: 140;");

            Label centLbl = new Label("Centrality: " + String.format("%.3f", cent));
            centLbl.setStyle("-fx-text-fill: -fx-ink-soft;");

            row.getChildren().addAll(rank, name, degLbl, centLbl);
            topList.getChildren().add(row);
        }

        hubsCard.getChildren().addAll(hubsTitle, topList);

        // Interactive 2D Graph Visualizer Card
        VBox canvasCard = new VBox(10);
        canvasCard.getStyleClass().add("card");
        Label canvasTitle = new Label("Interactive 2D Graph Visualizer");
        canvasTitle.getStyleClass().add("card-title");
        Label canvasSub = new Label("Visual representation of student vertices and established edges.");
        canvasSub.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-ink-soft;");

        GraphVisualizerCanvas canvas = new GraphVisualizerCanvas(networkService.getGraph());
        canvasCard.getChildren().addAll(canvasTitle, canvasSub, canvas);

        mainBox.getChildren().addAll(header, stats, hubsCard, canvasCard);
        contentArea.getChildren().setAll(mainBox);
    }
}
