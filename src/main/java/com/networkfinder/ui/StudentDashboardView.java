package com.networkfinder.ui;

import com.networkfinder.dsa.*;
import com.networkfinder.model.StudentProfile;
import com.networkfinder.model.User;
import com.networkfinder.service.NetworkService;
import com.networkfinder.service.AuthService;
import com.networkfinder.service.OperationResult;
import com.networkfinder.service.ValidationException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.*;

public class StudentDashboardView extends BorderPane {
    private final User currentUser;
    private final NetworkService networkService = NetworkService.getInstance();

    private final StackPane contentArea = new StackPane();
    private Button windowBackBtn;
    private Label windowTitleLbl;
    private List<Button> allTiles = new ArrayList<>();

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

        windowBackBtn = new Button("←  Settings");
        windowBackBtn.getStyleClass().add("window-back-btn");
        windowBackBtn.setOnAction(e -> showHomeScreen());
        windowBackBtn.setVisible(false);

        windowTitleLbl = new Label("Settings");
        windowTitleLbl.getStyleClass().add("window-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button settingsBtn = new Button("⚙ Settings · Account");
        settingsBtn.getStyleClass().add("button-secondary");
        settingsBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
        settingsBtn.setOnAction(e -> {
            windowBackBtn.setVisible(true);
            windowTitleLbl.setText("Settings > Account");
            showSettingsPanel();
        });

        Button logoutBtn = new Button("Sign out");
        logoutBtn.getStyleClass().add("button-secondary");
        logoutBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
        logoutBtn.setOnAction(e -> ScreenNavigator.showStudentLogin());

        bar.getChildren().addAll(windowBackBtn, windowTitleLbl, spacer, settingsBtn, logoutBtn);
        return bar;
    }

    // ==================================================
    // 0. HOME SCREEN (WINDOWS SETTINGS HUB)
    // ==================================================
    private void showHomeScreen() {
        windowBackBtn.setVisible(false);
        windowTitleLbl.setText("Settings");

        VBox homeBox = new VBox(0);
        homeBox.setAlignment(Pos.TOP_CENTER);
        homeBox.setPadding(new Insets(10, 0, 40, 0));

        // 1. Hero Profile Banner (Allisia Rose style)
        HBox heroBanner = new HBox(20);
        heroBanner.getStyleClass().add("fluent-profile-hero");
        heroBanner.setAlignment(Pos.CENTER_LEFT);

        // Circular Avatar
        StackPane avatarPane = new StackPane();
        Circle avatarCircle = new Circle(32);
        avatarCircle.getStyleClass().add("fluent-avatar");
        Text initials = new Text(getInitials(currentUser.getFullName()));
        initials.getStyleClass().add("fluent-avatar-text");
        avatarPane.getChildren().addAll(avatarCircle, initials);

        // User Meta
        VBox metaBox = new VBox(2);
        Label nameLbl = new Label(currentUser.getFullName());
        nameLbl.getStyleClass().add("fluent-user-name");

            StudentProfile profile = networkService.getStudentProfile(currentUser.getStudentId());

        String subText = currentUser.getStudentId();
        if (profile != null && profile.getCourse() != null) {
            subText += "  ●  " + profile.getCourse() + (profile.getSection() != null ? " (" + profile.getSection() + ")" : "");
        }
        Label subLbl = new Label(subText);
        subLbl.getStyleClass().add("fluent-user-subtitle");

        Hyperlink editProfileLink = new Hyperlink("Academic Profile");
        editProfileLink.getStyleClass().add("fluent-user-link");
        editProfileLink.setOnAction(e -> ScreenNavigator.showProfileWizard(currentUser));

        metaBox.getChildren().addAll(nameLbl, subLbl, editProfileLink);

        Region heroSpacer = new Region();
        HBox.setHgrow(heroSpacer, Priority.ALWAYS);

        // Right-aligned status widgets
        int connectionCount = networkService.getGraph().getDegree(currentUser.getStudentId());
        HBox statusCards = new HBox(12);
        statusCards.setAlignment(Pos.CENTER_RIGHT);

        VBox networkCard = createStatusWidget("◎", "Your Network", connectionCount + " Connected");
        VBox updateCard = createStatusWidget("↻", "Match Engine", "BFS Ready ✓");
        VBox securityCard = createStatusWidget("▣", "Verification", "Protected ✓");

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
            tileGrid.getColumnConstraints().add(col);
        }

        allTiles.clear();

        Button tileMatcher = createFluentTile("⊕", "Study Group Matcher", "Level-order BFS, subjects, peers", () -> {
            windowBackBtn.setVisible(true);
            windowTitleLbl.setText("Settings > Study Group Matcher");
            showStudyGroupMatcherScreen();
        });

        Button tilePathfinder = createFluentTile("⇄", "Pathfinder (BFS)", "Degrees of separation, shortest chain", () -> {
            windowBackBtn.setVisible(true);
            windowTitleLbl.setText("Settings > Pathfinder (BFS)");
            showPathfinderScreen();
        });

        Button tileConnections = createFluentTile("◍", "My Connections", "Direct peers, mutual friends, requests", () -> {
            windowBackBtn.setVisible(true);
            windowTitleLbl.setText("Settings > My Connections");
            showConnectionsScreen();
        });

        Button tileAnalytics = createFluentTile("▤", "Network Graph & Hubs", "Degree centrality, hubs, constellation", () -> {
            windowBackBtn.setVisible(true);
            windowTitleLbl.setText("Settings > Network Graph & Hubs");
            showAnalyticsScreen();
        });

        Button tileProfile = createFluentTile("✎", "Academic Profile", "Subjects, study habits, group preference", () -> {
            ScreenNavigator.showProfileWizard(currentUser);
        });

        Button tileVerification = createFluentTile("▣", "Account & Verification", "Student credentials, security badge", () -> {
            showAccountVerificationModal();
        });

        allTiles.addAll(Arrays.asList(tileMatcher, tilePathfinder, tileConnections, tileAnalytics, tileProfile, tileVerification));
        layoutTilesOnGrid(tileGrid, allTiles);

        // Live search filter on tiles
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = (newVal == null) ? "" : newVal.toLowerCase().trim();
            List<Button> visibleTiles = new ArrayList<>();
            for (Button t : allTiles) {
                Label tTitle = (Label) ((VBox) ((HBox) t.getGraphic()).getChildren().get(1)).getChildren().get(0);
                Label tDesc = (Label) ((VBox) ((HBox) t.getGraphic()).getChildren().get(1)).getChildren().get(1);
                if (query.isEmpty() || tTitle.getText().toLowerCase().contains(query) || tDesc.getText().toLowerCase().contains(query)) {
                    visibleTiles.add(t);
                }
            }
            layoutTilesOnGrid(tileGrid, visibleTiles);
        });

        homeBox.getChildren().addAll(heroBanner, searchContainer, tileGrid);
        contentArea.getChildren().setAll(homeBox);
    }

    private VBox createStatusWidget(String icon, String title, String sub) {
        VBox card = new VBox(3);
        card.getStyleClass().add("fluent-status-card");

        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("fluent-status-icon");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("fluent-status-title");

        Label subLbl = new Label(sub);
        subLbl.getStyleClass().add("fluent-status-sub");
        if (sub.contains("✓")) {
            subLbl.setStyle("-fx-text-fill: -fx-green;");
        }

        card.getChildren().addAll(iconLbl, titleLbl, subLbl);
        return card;
    }

    /** Places tiles into the stretchy 3-column grid, row by row. */
    private void layoutTilesOnGrid(GridPane grid, List<Button> tiles) {
        grid.getChildren().clear();
        int column = 0;
        for (Button tile : tiles) {
            grid.add(tile, column % 3, column / 3);
            column++;
        }
    }

    private Button createFluentTile(String icon, String title, String desc, Runnable onClick) {
        Button card = new Button();
        card.getStyleClass().add("fluent-tile");
        card.setMaxWidth(Double.MAX_VALUE);

        HBox inner = new HBox(16);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.setMaxWidth(Double.MAX_VALUE);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("fluent-tile-icon-box");
        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("fluent-tile-icon");
        iconBox.getChildren().add(iconLbl);

        VBox textCol = new VBox(4);
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("fluent-tile-title");

        Label descLbl = new Label(desc);
        descLbl.getStyleClass().add("fluent-tile-desc");
        descLbl.setWrapText(true);
        textCol.setMaxWidth(Double.MAX_VALUE);

        textCol.getChildren().addAll(titleLbl, descLbl);
        HBox.setHgrow(textCol, Priority.ALWAYS);
        inner.getChildren().addAll(iconBox, textCol);

        card.setGraphic(inner);
        card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        card.setOnAction(e -> onClick.run());
        return card;
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private void showAccountVerificationModal() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Account & Verification");
        alert.setHeaderText("Student Verification Status");
        alert.setContentText(
                "Student ID: " + currentUser.getStudentId() + "\n" +
                "Full Name: " + currentUser.getFullName() + "\n" +
                "Verification: " + currentUser.getVerificationStatus() + "\n" +
                "Role: " + currentUser.getRole() + "\n\n" +
                "Your account is verified and participating in the campus social network graph."
        );
        UiDialogs.styleDialogPane(alert.getDialogPane());
        alert.showAndWait();
    }

    // ==================================================
    // SETTINGS · ACCOUNT (change password, session)
    // ==================================================
    private void showSettingsPanel() {
        windowBackBtn.setVisible(true);
        windowTitleLbl.setText("Settings > Account");

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

        StackPane avatarPane = new StackPane();
        Circle avatarCircle = new Circle(24);
        avatarCircle.getStyleClass().add("fluent-avatar");
        Text initials = new Text(getInitials(currentUser.getFullName()));
        initials.getStyleClass().add("fluent-avatar-text");
        avatarPane.getChildren().addAll(avatarCircle, initials);

        VBox identityMeta = new VBox(2);
        Label nameLbl = new Label(currentUser.getFullName());
        nameLbl.getStyleClass().add("card-title");
        Label idLbl = new Label("Student ID: " + currentUser.getStudentId());
        idLbl.getStyleClass().add("helper-text");
        identityMeta.getChildren().addAll(nameLbl, idLbl);

        Region identitySpacer = new Region();
        HBox.setHgrow(identitySpacer, Priority.ALWAYS);
        Label statusBadge = new Label("APPROVED");
        statusBadge.getStyleClass().add("badge-approved");

        identityRow.getChildren().addAll(avatarPane, identityMeta, identitySpacer, statusBadge);

        Label memberSince = new Label("Member since "
                + (currentUser.getCreatedAt() != null ? currentUser.getCreatedAt().toLocalDate() : "-"));
        memberSince.getStyleClass().add("helper-text");

        accountCard.getChildren().addAll(identityRow, memberSince);

        // 2. Change password card
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

        // 3. Session card
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

        mainBox.getChildren().addAll(accountCard, passwordCard, sessionCard);
        contentArea.getChildren().setAll(mainBox);
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
        windowBackBtn.setVisible(true);
        windowTitleLbl.setText("Settings > Study Group Matcher");

        VBox mainBox = new VBox(16);
        mainBox.setPadding(new Insets(20, 36, 30, 36));

        Node header = buildSubScreenHeader(
                "Study Group Matcher",
                "Discovers compatible peers using Level-Order BFS traversal grouped by social distance."
        );

        // Filter Bar Card
        HBox filterCard = new HBox(14);
        filterCard.getStyleClass().add("card");
        filterCard.setAlignment(Pos.CENTER_LEFT);
        filterCard.setPadding(new Insets(14, 18, 14, 18));

        ComboBox<String> subjectFilter = new ComboBox<>();
        subjectFilter.setPromptText("Filter by Subject");
        subjectFilter.getItems().addAll("All Subjects", "Programming", "Data Structures", "Database Management",
                "Networking", "Web Development", "Mobile Development", "Mathematics", "Cybersecurity", "Software Engineering");
        subjectFilter.setValue("All Subjects");

        CheckBox sameSectionCheck = new CheckBox("Same Section Only");
        sameSectionCheck.setStyle("-fx-text-fill: -fx-ink; -fx-font-weight: 500;");

        ComboBox<String> groupSizeFilter = new ComboBox<>();
        groupSizeFilter.getItems().addAll("Any Group Size", "Small Group", "Medium Group", "Large Group");
        groupSizeFilter.setValue("Any Group Size");

        Button searchBtn = new Button("Search Peers");
        searchBtn.getStyleClass().add("button-primary");

        filterCard.getChildren().addAll(
                new Label("Filters:"),
                subjectFilter,
                sameSectionCheck,
                groupSizeFilter,
                searchBtn
        );

        VBox resultsBox = new VBox(14);

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
                    for (MatchResult m : tier.getValue()) {
                        resultsBox.getChildren().add(createMatchCard(m));
                    }
                }
            } else {
                VBox infoBox = new VBox(6);
                infoBox.getStyleClass().add("info-card");
                Label infoLbl1 = new Label("⚑ No Extended Network Connections Found (Isolated Node)");
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
                for (MatchResult m : campusMatches) {
                    resultsBox.getChildren().add(createMatchCard(m));
                }
            }
        };

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
                actionBtn = new Button("● Request Sent");
                actionBtn.getStyleClass().add("button-secondary");
                actionBtn.setDisable(true);
            } else {
                actionBtn = new Button("✓ Accept Request");
                actionBtn.getStyleClass().add("button-primary");
                actionBtn.setOnAction(e -> {
                    OperationResult res = networkService.acceptConnectionRequest(targetId, currentId);
                    if (res.isOk()) {
                        showStudyGroupMatcherScreen();
                    } else {
                        UiDialogs.showError(res.getMessage());
                    }
                });
            }
        } else {
            actionBtn = new Button("+ Send Request");
            actionBtn.getStyleClass().add("button-primary");
            actionBtn.setOnAction(e -> {
                OperationResult res = networkService.sendConnectionRequest(currentId, targetId);
                if (res.isOk()) {
                    showStudyGroupMatcherScreen();
                } else {
                    UiDialogs.showError(res.getMessage());
                }
            });
        }
        actionRow.getChildren().add(actionBtn);

        card.getChildren().addAll(topRow, metaRow, sharedRow, actionRow);
        return card;
    }

    // ==================================================
    // 2. QUERY 2 — PATHFINDER (Shortest Path BFS)
    // ==================================================
    private void showPathfinderScreen() {
        windowBackBtn.setVisible(true);
        windowTitleLbl.setText("Settings > Pathfinder (BFS)");

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
        pickers.setAlignment(Pos.CENTER_LEFT);

        // Source
        VBox srcBox = new VBox(6);
        Label srcLabel = new Label("Source Student (Starting Node)");
        srcLabel.getStyleClass().add("field-label");
        ComboBox<StudentNode> srcCombo = new ComboBox<>();
        srcCombo.getItems().addAll(networkService.getGraph().getAllNodes());
        StudentNode currentN = networkService.getGraph().getNode(currentUser.getStudentId());
        if (currentN != null) srcCombo.setValue(currentN);
        srcCombo.setMinWidth(260);
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
        tgtBox.getChildren().addAll(tgtLabel, tgtCombo);

        Button runBfsBtn = new Button("Run BFS Pathfinder");
        runBfsBtn.getStyleClass().add("button-primary");
        runBfsBtn.setStyle("-fx-padding: 10 20;");
        runBfsBtn.setDefaultButton(true);

        pickers.getChildren().addAll(srcBox, tgtBox, runBfsBtn);

        Label hintLabel = new Label("✦ Interactive Canvas: Click any node on the graph below to choose Source or Target.");
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
        metrics.getChildren().addAll(
                createMetricPill("Degrees of Separation", String.valueOf(result.getDegreesOfSeparation()), "-fx-accent"),
                createMetricPill("Path Length (Hops)", String.valueOf(result.getPath().size() - 1), "-fx-green"),
                createMetricPill("Nodes Explored (BFS)", String.valueOf(result.getNodesExplored()), "-fx-ink-soft")
        );

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
                Label arrow = new Label("→");
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
        windowBackBtn.setVisible(true);
        windowTitleLbl.setText("Settings > My Connections");

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

            Label reqTitle = new Label("⚑ Incoming Requests (" + incoming.size() + ")");
            reqTitle.getStyleClass().add("card-title");
            reqTitle.setStyle("-fx-text-fill: -fx-accent;");
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
                    OperationResult res = networkService.acceptConnectionRequest(c.getRequesterId(), c.getReceiverId());
                    if (res.isOk()) {
                        showConnectionsScreen();
                    } else {
                        UiDialogs.showError(res.getMessage());
                    }
                });

                Button declineBtn = new Button("✕ Decline");
                declineBtn.getStyleClass().add("button-secondary");
                declineBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12; -fx-text-fill: -fx-danger;");
                declineBtn.setOnAction(e -> {
                    OperationResult res = networkService.declineConnectionRequest(c.getRequesterId(), c.getReceiverId());
                    if (res.isOk()) {
                        showConnectionsScreen();
                    } else {
                        UiDialogs.showError(res.getMessage());
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
            Label outTitle = new Label("● Outgoing Requests (" + outgoing.size() + ")");
            outTitle.getStyleClass().add("card-title");
            outTitle.setStyle("-fx-text-fill: -fx-amber;");
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
                    OperationResult res = networkService.declineConnectionRequest(currentUser.getStudentId(), c.getReceiverId());
                    if (res.isOk()) {
                        showConnectionsScreen();
                    } else {
                        UiDialogs.showError(res.getMessage());
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
        Label connectTitle = new Label("✚ Connect with Classmates");
        connectTitle.getStyleClass().add("card-title");

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
        classmatePicker.setPromptText(connectable.isEmpty() ? "No new classmates available" : "Select classmate...");
        classmatePicker.setMinWidth(320);
        classmatePicker.setDisable(connectable.isEmpty());

        Button sendReqBtn = new Button("+ Send Request");
        sendReqBtn.getStyleClass().add("button-primary");
        sendReqBtn.setDisable(connectable.isEmpty());

        sendReqBtn.setOnAction(e -> {
            StudentNode target = classmatePicker.getValue();
            if (target != null) {
                OperationResult res = networkService.sendConnectionRequest(currentUser.getStudentId(), target.getStudentId());
                if (res.isOk()) {
                    showConnectionsScreen();
                } else {
                    UiDialogs.showError(res.getMessage());
                }
            }
        });

        connectRow.getChildren().addAll(classmatePicker, sendReqBtn);
        connectCard.getChildren().addAll(connectTitle, connectDesc, connectRow);
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
                    OperationResult res = networkService.removeConnection(currentUser.getStudentId(), nId);
                    if (res.isOk()) {
                        showConnectionsScreen();
                    } else {
                        UiDialogs.showError(res.getMessage());
                    }
                });

                row.getChildren().addAll(name, sec, sp, disconnectBtn);
                connCard.getChildren().add(row);
            }
        }
        mainBox.getChildren().add(connCard);

        // 5. Mutual Connection Analyzer
        VBox mutualCard = new VBox(12);
        mutualCard.getStyleClass().add("card");
        Label mTitle = new Label("Mutual Connections Finder");
        mTitle.getStyleClass().add("card-title");

        HBox mInputRow = new HBox(14);
        mInputRow.setAlignment(Pos.CENTER_LEFT);
        Label pickLabel = new Label("Compare with:");
        ComboBox<StudentNode> compCombo = new ComboBox<>();
        compCombo.getItems().addAll(networkService.getGraph().getAllNodes());
        compCombo.setMinWidth(260);

        Button compBtn = new Button("Find Mutuals");
        compBtn.getStyleClass().add("button-primary");

        mInputRow.getChildren().addAll(pickLabel, compCombo, compBtn);

        VBox mutualResults = new VBox(8);
        mutualResults.setVisible(false);

        compBtn.setOnAction(e -> {
            StudentNode other = compCombo.getValue();
            if (other == null) return;

            List<String> mutuals = networkService.getMutualConnections(currentUser.getStudentId(), other.getStudentId());
            mutualResults.getChildren().clear();

            Label mCount = new Label("Mutual Friends: " + mutuals.size());
            mCount.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-ink;");
            mutualResults.getChildren().add(mCount);

            if (mutuals.isEmpty()) {
                Label noM = new Label("No mutual direct connections with " + other.getFullName() + ".");
                noM.setStyle("-fx-text-fill: -fx-ink-soft;");
                mutualResults.getChildren().add(noM);
            } else {
                FlowPane chips = new FlowPane(8, 8);
                for (String mId : mutuals) {
                    StudentNode mNode = networkService.getGraph().getNode(mId);
                    Label chip = new Label("◉ " + (mNode != null ? mNode.getFullName() : mId));
                    chip.getStyleClass().add("chip-selected");
                    chips.getChildren().add(chip);
                }
                mutualResults.getChildren().add(chips);
            }
            mutualResults.setVisible(true);
        });

        mutualCard.getChildren().addAll(mTitle, mInputRow, mutualResults);
        mainBox.getChildren().add(mutualCard);

        contentArea.getChildren().setAll(mainBox);
    }

    // ==================================================
    // 4. NETWORK ANALYTICS & DEGREE CENTRALITY
    // ==================================================
    private void showAnalyticsScreen() {
        windowBackBtn.setVisible(true);
        windowTitleLbl.setText("Settings > Network Graph & Hubs");

        VBox mainBox = new VBox(18);
        mainBox.setPadding(new Insets(20, 36, 30, 36));

        Node header = buildSubScreenHeader(
                "Network Graph Analytics & Hubs",
                "Calculates Degree Centrality to identify key study group leaders and network coordinators."
        );

        HBox stats = new HBox(14);
        stats.getChildren().addAll(
                createMetricPill("Total Students (V)", String.valueOf(networkService.getGraph().getVertexCount()), "-fx-accent"),
                createMetricPill("Total Connections (E)", String.valueOf(networkService.getGraph().getEdgeCount()), "-fx-green"),
                createMetricPill("Your Degree", String.valueOf(networkService.getGraph().getDegree(currentUser.getStudentId())), "-fx-accent"),
                createMetricPill("Your Centrality", String.format("%.2f", networkService.getCentrality(currentUser.getStudentId())), "-fx-accent-hover")
        );

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
            name.setStyle("-fx-font-weight: 600; -fx-min-width: 240; -fx-text-fill: -fx-ink;");

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
