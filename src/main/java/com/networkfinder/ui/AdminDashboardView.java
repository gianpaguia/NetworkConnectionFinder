package com.networkfinder.ui;

import com.networkfinder.database.ProfileDao;
import com.networkfinder.database.UserDao;
import com.networkfinder.model.StudentProfile;
import com.networkfinder.model.User;
import com.networkfinder.model.VerificationStatus;
import com.networkfinder.service.InputValidator;
import com.networkfinder.service.NetworkService;
import com.networkfinder.service.ValidationException;
import com.networkfinder.service.VerificationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.io.File;
import java.util.List;

public class AdminDashboardView extends BorderPane {
    private final VerificationService verificationService = new VerificationService();
    private final UserDao userDao = new UserDao();
    private final ProfileDao profileDao = new ProfileDao();
    private final NetworkService networkService = NetworkService.getInstance();

    private final StackPane contentArea = new StackPane();
    private Button activeNavBtn;

    public AdminDashboardView() {
        getStyleClass().add("fluent-root-pane");

        setLeft(buildSidebar());

        ScrollPane centerScroll = new ScrollPane(contentArea);
        centerScroll.setFitToWidth(true);
        centerScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        setCenter(centerScroll);

        showVerificationQueueScreen();
    }

    private Node buildSidebar() {
        BorderPane sidebar = new BorderPane();
        sidebar.getStyleClass().add("sidebar");

        Text title = new Text("SOCIAL NETWORK\nCONNECTION FINDER");
        title.getStyleClass().add("sidebar-title");

        Label adminBadge = new Label("Admin Panel");
        adminBadge.setStyle("-fx-background-color: -fx-ink; -fx-text-fill: -fx-amber; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 3 8; -fx-background-radius: 6;");

        VBox titleBox = new VBox(6, title, adminBadge);
        titleBox.setPadding(new Insets(0, 0, 16, 0));

        Label userMgmtLabel = new Label("User management");
        userMgmtLabel.getStyleClass().add("sidebar-section-heading");

        Button idVerifBtn = createNavButton("📋  ID Verification", true);
        Button accountsBtn = createNavButton("👥  Student Accounts", false);

        idVerifBtn.setOnAction(e -> {
            setActiveNav(idVerifBtn);
            showVerificationQueueScreen();
        });

        accountsBtn.setOnAction(e -> {
            setActiveNav(accountsBtn);
            showAccountsScreen();
        });

        Label networkLabel = new Label("Network");
        networkLabel.getStyleClass().add("sidebar-section-heading");

        Button netOverviewBtn = createNavButton("🌐  Network Graph", false);
        netOverviewBtn.setOnAction(e -> {
            setActiveNav(netOverviewBtn);
            showNetworkOverviewScreen();
        });

        Button connManagerBtn = createNavButton("🔗  Connections Manager", false);
        connManagerBtn.setOnAction(e -> {
            setActiveNav(connManagerBtn);
            showConnectionsManagerScreen();
        });

        Button logoutBtn = new Button("🚪  Logout");
        logoutBtn.getStyleClass().add("nav-button");
        logoutBtn.setStyle("-fx-text-fill: -fx-danger;");
        logoutBtn.setOnAction(e -> ScreenNavigator.showAdminLogin());

        VBox navBox = new VBox(8, userMgmtLabel, idVerifBtn, accountsBtn, networkLabel, netOverviewBtn, connManagerBtn);
        VBox logoutBox = new VBox(logoutBtn);

        sidebar.setTop(titleBox);
        sidebar.setCenter(navBox);
        sidebar.setBottom(logoutBox);

        return sidebar;
    }

    private Button createNavButton(String text, boolean active) {
        Button btn = new Button(text);
        if (active) {
            btn.getStyleClass().add("nav-button-active");
            activeNavBtn = btn;
        } else {
            btn.getStyleClass().add("nav-button");
        }
        return btn;
    }

    private void setActiveNav(Button btn) {
        if (activeNavBtn != null) {
            activeNavBtn.getStyleClass().remove("nav-button-active");
            activeNavBtn.getStyleClass().add("nav-button");
        }
        btn.getStyleClass().remove("nav-button");
        btn.getStyleClass().add("nav-button-active");
        activeNavBtn = btn;
    }

    // ==================================================
    // SCREEN 5 — ADMIN VERIFICATION DASHBOARD QUEUE
    // ==================================================
    private void showVerificationQueueScreen() {
        VBox mainBox = new VBox(20);
        mainBox.setPadding(new Insets(28, 32, 28, 32));

        // Header
        VBox header = new VBox(4);
        Label heading = new Label("Student ID Verification");
        heading.getStyleClass().add("screen-heading");
        Label subtitle = new Label("Review pending student registration requests.");
        subtitle.getStyleClass().add("screen-subtitle");
        header.getChildren().addAll(heading, subtitle);

        List<User> pendingList = verificationService.getPendingVerifications();

        if (pendingList.isEmpty()) {
            VBox emptyCard = new VBox(12);
            emptyCard.getStyleClass().add("card");
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(40));

            Label checkIcon = new Label("✓");
            checkIcon.setStyle("-fx-font-size: 36px; -fx-text-fill: -fx-green;");
            Label emptyText = new Label("No pending ID verification requests at this time.");
            emptyText.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -fx-ink;");
            Label subEmpty = new Label("All registered students have been reviewed.");
            subEmpty.setStyle("-fx-text-fill: -fx-ink-soft;");

            emptyCard.getChildren().addAll(checkIcon, emptyText, subEmpty);
            mainBox.getChildren().addAll(header, emptyCard);
        } else {
            // Table
            TableView<User> table = new TableView<>();
            table.getStyleClass().add("table-view");
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

            TableColumn<User, String> idCol = new TableColumn<>("Student ID");
            idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStudentId()));

            TableColumn<User, String> nameCol = new TableColumn<>("Student Name");
            nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));

            TableColumn<User, String> courseCol = new TableColumn<>("Course");
            courseCol.setCellValueFactory(data -> {
                StudentProfile p = profileDao.getProfile(data.getValue().getStudentId());
                return new SimpleStringProperty(p != null && p.getCourse() != null ? p.getCourse() : "Pending Setup");
            });

            TableColumn<User, String> statusCol = new TableColumn<>("Status");
            statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getVerificationStatus().name()));
            statusCol.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        Label badge = new Label("🟡 Pending");
                        badge.getStyleClass().add("badge-pending");
                        setGraphic(badge);
                    }
                }
            });

            TableColumn<User, Void> actionCol = new TableColumn<>("Action");
            actionCol.setCellFactory(col -> new TableCell<>() {
                private final Button viewBtn = new Button("View");
                {
                    viewBtn.getStyleClass().add("button-primary");
                    viewBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6 14;");
                    viewBtn.setOnAction(e -> {
                        User user = getTableView().getItems().get(getIndex());
                        showVerificationReviewScreen(user);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : viewBtn);
                }
            });

            table.getColumns().addAll(idCol, nameCol, courseCol, statusCol, actionCol);
            ObservableList<User> items = FXCollections.observableArrayList(pendingList);
            table.setItems(items);
            table.setPrefHeight(380);

            mainBox.getChildren().addAll(header, table);
        }

        contentArea.getChildren().setAll(mainBox);
    }

    // ==================================================
    // SCREEN 6 — ADMIN VIEW VERIFICATION (SIDE-BY-SIDE)
    // ==================================================
    private void showVerificationReviewScreen(User user) {
        VBox mainBox = new VBox(20);
        mainBox.setPadding(new Insets(28, 32, 28, 32));

        // Header with Back button
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = new Button("← Back to List");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setOnAction(e -> showVerificationQueueScreen());

        Label heading = new Label("Student Verification");
        heading.getStyleClass().add("screen-heading");

        topBar.getChildren().addAll(backBtn, heading);

        // Side-by-side card
        HBox splitCard = new HBox(30);
        splitCard.getStyleClass().add("card");
        splitCard.setAlignment(Pos.CENTER_LEFT);

        // Left: Student Information
        VBox infoBox = new VBox(14);
        infoBox.setMinWidth(340);
        infoBox.setMaxWidth(380);

        Label infoHeading = new Label("Student Information");
        infoHeading.getStyleClass().add("card-title");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);

        StudentProfile profile = profileDao.getProfile(user.getStudentId());

        addRow(grid, 0, "Name:", user.getFullName());
        addRow(grid, 1, "Student ID:", user.getStudentId());
        addRow(grid, 2, "Course:", profile != null && profile.getCourse() != null ? profile.getCourse() : "To be set during profile setup");
        addRow(grid, 3, "Year:", profile != null && profile.getYearLevel() != null ? profile.getYearLevel() : "Pending");
        addRow(grid, 4, "Section:", profile != null && profile.getSection() != null ? profile.getSection() : "Pending");

        Label checkQuestion = new Label("Does the submitted ID match the student's information?");
        checkQuestion.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-ink; -fx-padding: 10 0 0 0;");
        checkQuestion.setWrapText(true);

        Label notice = new Label("Manual verification: Compare name and student ID against the submitted physical card picture.");
        notice.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-ink-soft;");
        notice.setWrapText(true);

        // Approve & Reject Buttons
        HBox buttonBox = new HBox(12);
        Button approveBtn = new Button("✓ APPROVE");
        approveBtn.getStyleClass().add("button-primary");
        approveBtn.setOnAction(e -> confirmApproveStudent(user));

        Button rejectBtn = new Button("✕ REJECT");
        rejectBtn.getStyleClass().add("button-danger");
        rejectBtn.setOnAction(e -> {
            if (!verificationService.rejectStudent(user.getStudentId())) {
                UiDialogs.showError("Could not reject this student. Please try again.");
                return;
            }
            showVerificationQueueScreen();
        });

        buttonBox.getChildren().addAll(approveBtn, rejectBtn);

        infoBox.getChildren().addAll(infoHeading, grid, checkQuestion, notice, buttonBox);

        // Right: Submitted Student ID Image
        VBox imageBox = new VBox(10);
        imageBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(imageBox, Priority.ALWAYS);

        Label imgLabel = new Label("Submitted Student ID");
        imgLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: -fx-ink-soft;");

        StackPane imgContainer = new StackPane();
        imgContainer.setStyle("-fx-background-color: -fx-sage-tint; -fx-border-color: -fx-border; -fx-border-radius: 8; -fx-background-radius: 8;");
        imgContainer.setMinHeight(280);
        imgContainer.setMinWidth(420);

        ImageView idImageView = new ImageView();
        idImageView.setFitWidth(440);
        idImageView.setFitHeight(270);
        idImageView.setPreserveRatio(true);

        String imgPath = user.getIdCardImagePath();
        if (imgPath != null && new File(imgPath).exists()) {
            try {
                idImageView.setImage(new Image(new File(imgPath).toURI().toString()));
                imgContainer.getChildren().add(idImageView);
            } catch (Exception ex) {
                Label errImg = new Label("Could not load image: " + ex.getMessage());
                imgContainer.getChildren().add(errImg);
            }
        } else {
            Label placeholder = new Label("📷 [ Placeholder: No Student ID image uploaded ]");
            placeholder.setStyle("-fx-text-fill: -fx-ink-soft; -fx-font-weight: 600;");
            imgContainer.getChildren().add(placeholder);
        }

        imageBox.getChildren().addAll(imgLabel, imgContainer);

        splitCard.getChildren().addAll(infoBox, imageBox);
        mainBox.getChildren().addAll(topBar, splitCard);

        contentArea.getChildren().setAll(mainBox);
    }

    private void addRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-ink-soft;");
        Label v = new Label(value);
        v.setStyle("-fx-font-weight: 700; -fx-text-fill: -fx-ink;");
        grid.addRow(row, l, v);
    }

    // ==================================================
    // STUDENT ACCOUNTS MANAGEMENT (ADMIN CRUD)
    // ==================================================
    private void showAccountsScreen() {
        VBox mainBox = new VBox(16);
        mainBox.setPadding(new Insets(28, 32, 28, 32));

        // Header with Add Student button
        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label heading = new Label("Student Accounts & Directory");
        heading.getStyleClass().add("screen-heading");
        Label subtitle = new Label("Administer registered students: add accounts, edit details, or remove students from the graph.");
        subtitle.getStyleClass().add("screen-subtitle");
        titleBox.getChildren().addAll(heading, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addStudentBtn = new Button("➕ Add New Student");
        addStudentBtn.getStyleClass().add("button-primary");
        addStudentBtn.setOnAction(e -> showAddStudentDialog());

        topRow.getChildren().addAll(titleBox, spacer, addStudentBtn);

        TableView<User> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<User, String> idCol = new TableColumn<>("Student ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStudentId()));

        TableColumn<User, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));

        TableColumn<User, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(data -> {
            StudentProfile p = profileDao.getProfile(data.getValue().getStudentId());
            return new SimpleStringProperty(p != null && p.getCourse() != null ? p.getCourse() : "-");
        });

        TableColumn<User, String> secCol = new TableColumn<>("Section");
        secCol.setCellValueFactory(data -> {
            StudentProfile p = profileDao.getProfile(data.getValue().getStudentId());
            return new SimpleStringProperty(p != null && p.getSection() != null ? p.getSection() : "-");
        });

        TableColumn<User, String> statusCol = new TableColumn<>("Verification");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getVerificationStatus().name()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else if ("APPROVED".equals(item)) {
                    Label b = new Label("✓ Approved");
                    b.setStyle("-fx-background-color: -fx-green-tint; -fx-text-fill: -fx-green; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: 700;");
                    setGraphic(b);
                } else if ("PENDING".equals(item)) {
                    Label b = new Label("🟡 Pending");
                    b.getStyleClass().add("badge-pending");
                    setGraphic(b);
                } else {
                    Label b = new Label("✕ " + item);
                    b.setStyle("-fx-background-color: -fx-danger-tint; -fx-text-fill: -fx-danger; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: 700;");
                    setGraphic(b);
                }
            }
        });

        TableColumn<User, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button resetBtn = new Button("Reset PW");
            private final Button delBtn = new Button("Delete");
            private final HBox btnBox = new HBox(6, editBtn, resetBtn, delBtn);

            {
                btnBox.setAlignment(Pos.CENTER);
                editBtn.getStyleClass().add("button-secondary");
                editBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
                editBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    showEditStudentDialog(u);
                });

                resetBtn.getStyleClass().add("button-secondary");
                resetBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
                resetBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    showResetPasswordDialog(u);
                });

                delBtn.getStyleClass().add("button-secondary");
                delBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10; -fx-text-fill: -fx-danger;");
                delBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    confirmDeleteStudent(u);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User u = getTableView().getItems().get(getIndex());
                    if (u.getRole() == com.networkfinder.model.Role.ADMIN) {
                        Label admLbl = new Label("Admin");
                        admLbl.setStyle("-fx-text-fill: -fx-ink-soft; -fx-font-size: 11px; -fx-font-style: italic;");
                        setGraphic(admLbl);
                    } else {
                        setGraphic(btnBox);
                    }
                }
            }
        });

        table.getColumns().addAll(idCol, nameCol, courseCol, secCol, statusCol, actionCol);
        table.setItems(FXCollections.observableArrayList(userDao.getAllUsers()));
        table.setPrefHeight(450);

        mainBox.getChildren().addAll(topRow, table);
        contentArea.getChildren().setAll(mainBox);
    }

    private void showAddStudentDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Add New Student");
        dialog.setHeaderText("Create and instantly verify a new student node in the network graph");
        UiDialogs.styleDialogPane(dialog.getDialogPane());

        ButtonType saveBtnType = new ButtonType("Add Student", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField idField = new TextField();
        idField.setPromptText("Numbers only (e.g. 202610011)");
        idField.setTextFormatter(TextFieldFilters.studentIdFilter(InputValidator.getMaxIdLength()));

        TextField firstNameField = new TextField();
        firstNameField.setPromptText("e.g. Juan (letters only)");
        firstNameField.setTextFormatter(TextFieldFilters.nameFilter(InputValidator.getMaxNameLength()));

        TextField middleField = new TextField();
        middleField.setPromptText("1-2 letters (e.g. M)");
        middleField.setTextFormatter(TextFieldFilters.shortNameFilter());

        TextField surnameField = new TextField();
        surnameField.setPromptText("e.g. Dela Cruz (letters only)");
        surnameField.setTextFormatter(TextFieldFilters.nameFilter(InputValidator.getMaxNameLength()));

        TextField suffixField = new TextField();
        suffixField.setPromptText("1-2 letters (e.g. Jr)");
        suffixField.setTextFormatter(TextFieldFilters.shortNameFilter());

        ComboBox<String> courseCombo = new ComboBox<>();
        courseCombo.getItems().addAll(
                "Bachelor of Science in Information Technology",
                "Bachelor of Science in Computer Science",
                "Bachelor of Science in Information Systems",
                "Bachelor of Science in Computer Engineering"
        );
        courseCombo.setValue("Bachelor of Science in Information Technology");

        ComboBox<String> yearCombo = new ComboBox<>();
        yearCombo.getItems().addAll("1st Year", "2nd Year", "3rd Year", "4th Year");
        yearCombo.setValue("1st Year");

        TextField sectionField = new TextField();
        sectionField.setPromptText("e.g. BSIT 1A");

        grid.addRow(0, new Label("Student ID:"), idField);
        grid.addRow(1, new Label("First Name:"), firstNameField);
        grid.addRow(2, new Label("Middle Initial:"), middleField);
        grid.addRow(3, new Label("Surname:"), surnameField);
        grid.addRow(4, new Label("Suffix:"), suffixField);
        grid.addRow(5, new Label("Course:"), courseCombo);
        grid.addRow(6, new Label("Year Level:"), yearCombo);
        grid.addRow(7, new Label("Section:"), sectionField);

        dialog.getDialogPane().setContent(grid);

        java.util.concurrent.atomic.AtomicReference<String> validationError = new java.util.concurrent.atomic.AtomicReference<>(null);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                String sId = idField.getText().trim();
                String sFirst = firstNameField.getText().trim();
                String sMiddle = middleField.getText().trim();
                String sSurname = surnameField.getText().trim();
                String sSuffix = suffixField.getText().trim();
                String sCourse = courseCombo.getValue();
                String sYear = yearCombo.getValue();
                String sSec = sectionField.getText().trim();

                if (!sId.isEmpty() && !sFirst.isEmpty() && !sSurname.isEmpty()) {
                    try {
                        return networkService.adminAddStudent(sId, sFirst, sMiddle, sSurname, sSuffix, sCourse, sYear, sSec);
                    } catch (ValidationException ve) {
                        validationError.set(ve.getMessage());
                        return false;
                    }
                }
            }
            return false;
        });

        dialog.showAndWait().ifPresent(success -> {
            if (success) {
                showAccountsScreen();
            } else if (validationError.get() != null) {
                UiDialogs.showError(validationError.get());
            } else {
                UiDialogs.showError("Could not add the student. The Student ID may already exist or some fields are invalid.");
            }
        });
    }

    private void showEditStudentDialog(User user) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit Student - " + user.getStudentId());
        dialog.setHeaderText("Modify student information and academic attributes");
        UiDialogs.styleDialogPane(dialog.getDialogPane());

        ButtonType saveBtnType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 10));

        String[] legacyParts = splitLegacyName(user);
        String first = user.getFirstName() != null ? user.getFirstName() : legacyParts[0];
        String middle = user.getMiddleInitial() != null ? user.getMiddleInitial() : "";
        String surname = user.getSurname() != null ? user.getSurname() : legacyParts[1];
        String suffix = user.getSuffix() != null ? user.getSuffix() : "";

        TextField firstNameField = new TextField(first);
        firstNameField.setPromptText("e.g. Juan (letters only)");
        firstNameField.setTextFormatter(TextFieldFilters.nameFilter(InputValidator.getMaxNameLength()));

        TextField middleField = new TextField(middle);
        middleField.setPromptText("1-2 letters (e.g. M)");
        middleField.setTextFormatter(TextFieldFilters.shortNameFilter());

        TextField surnameField = new TextField(surname);
        surnameField.setPromptText("e.g. Dela Cruz (letters only)");
        surnameField.setTextFormatter(TextFieldFilters.nameFilter(InputValidator.getMaxNameLength()));

        TextField suffixField = new TextField(suffix);
        suffixField.setPromptText("1-2 letters (e.g. Jr)");
        suffixField.setTextFormatter(TextFieldFilters.shortNameFilter());

        StudentProfile profile = profileDao.getProfile(user.getStudentId());
        String currentCourse = (profile != null && profile.getCourse() != null) ? profile.getCourse() : "Bachelor of Science in Information Technology";
        String currentYear = (profile != null && profile.getYearLevel() != null) ? profile.getYearLevel() : "1st Year";
        String currentSec = (profile != null && profile.getSection() != null) ? profile.getSection() : "";

        ComboBox<String> courseCombo = new ComboBox<>();
        courseCombo.getItems().addAll(
                "Bachelor of Science in Information Technology",
                "Bachelor of Science in Computer Science",
                "Bachelor of Science in Information Systems",
                "Bachelor of Science in Computer Engineering"
        );
        courseCombo.setValue(currentCourse);

        ComboBox<String> yearCombo = new ComboBox<>();
        yearCombo.getItems().addAll("1st Year", "2nd Year", "3rd Year", "4th Year");
        yearCombo.setValue(currentYear);

        TextField sectionField = new TextField(currentSec);

        grid.addRow(0, new Label("First Name:"), firstNameField);
        grid.addRow(1, new Label("Middle Initial:"), middleField);
        grid.addRow(2, new Label("Surname:"), surnameField);
        grid.addRow(3, new Label("Suffix:"), suffixField);
        grid.addRow(4, new Label("Course:"), courseCombo);
        grid.addRow(5, new Label("Year Level:"), yearCombo);
        grid.addRow(6, new Label("Section:"), sectionField);

        dialog.getDialogPane().setContent(grid);

        java.util.concurrent.atomic.AtomicReference<String> validationError = new java.util.concurrent.atomic.AtomicReference<>(null);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                String sFirst = firstNameField.getText().trim();
                String sMiddle = middleField.getText().trim();
                String sSurname = surnameField.getText().trim();
                String sSuffix = suffixField.getText().trim();
                String sCourse = courseCombo.getValue();
                String sYear = yearCombo.getValue();
                String sSec = sectionField.getText().trim();
                if (!sFirst.isEmpty() && !sSurname.isEmpty()) {
                    try {
                        return networkService.adminEditStudent(user.getStudentId(), sFirst, sMiddle, sSurname, sSuffix, sCourse, sYear, sSec);
                    } catch (ValidationException ve) {
                        validationError.set(ve.getMessage());
                        return false;
                    }
                }
            }
            return false;
        });

        dialog.showAndWait().ifPresent(success -> {
            if (success) {
                showAccountsScreen();
            } else if (validationError.get() != null) {
                UiDialogs.showError(validationError.get());
            } else {
                UiDialogs.showError("Could not save changes. Please review the fields and try again.");
            }
        });
    }

    /**
     * Admin can hand out temporary passwords to students who lost theirs
     * (the app's recovery path). Input rules are enforced by the service and
     * surfaced through a themed error dialog.
     */
    private void showResetPasswordDialog(User user) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Reset Password - " + user.getStudentId());
        dialog.setHeaderText("Set a temporary password for " + user.getFullName());
        UiDialogs.styleDialogPane(dialog.getDialogPane());

        ButtonType resetBtnType = new ButtonType("Set Password", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(resetBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 10));

        PasswordRevealField passwordValue = new PasswordRevealField("At least 8 characters, letter + number");
        passwordValue.setMaxWidth(Double.MAX_VALUE);

        Label hintLabel = new Label("Give this temporary password to the student. They can change it on their Account Settings page. Use the eye button to double-check what you typed.");
        hintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-ink-soft; -fx-wrap-text: true;");

        grid.addRow(0, new Label("Temporary Password:"), passwordValue);

        VBox contentBox = new VBox(10, grid, hintLabel);
        dialog.getDialogPane().setContent(contentBox);

        java.util.concurrent.atomic.AtomicReference<String> validationError = new java.util.concurrent.atomic.AtomicReference<>(null);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == resetBtnType) {
                try {
                    return networkService.adminResetPassword(user.getStudentId(), passwordValue.getValue());
                } catch (ValidationException ve) {
                    validationError.set(ve.getMessage());
                    return false;
                }
            }
            return false;
        });

        dialog.showAndWait().ifPresent(success -> {
            if (success) {
                UiDialogs.showSuccess("The password for " + user.getStudentId() + " was reset. Sign-in with the temporary password now works.");
            } else if (validationError.get() != null) {
                UiDialogs.showError(validationError.get());
            } else {
                UiDialogs.showError("Could not reset the password for this account.");
            }
        });
    }
    /**
     * Users created before the name split only store a composed full name;
     * split it as [first token, remaining tokens] so the edit dialog can
     * prefill editable parts.
     */
    private String[] splitLegacyName(User user) {
        String full = user.getFullName() == null ? "" : user.getFullName().trim();
        int index = full.indexOf(' ');
        if (index > 0) {
            return new String[]{full.substring(0, index), full.substring(index + 1)};
        }
        return new String[]{full, ""};
    }

    /**
     * approving a student grants sign-in and adds them to the campus network
     * graph, so a final confirmation prevents one accidental click from
     * verifying the wrong account.
     */
    private void confirmApproveStudent(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Accept Student");
        alert.setHeaderText("Are you sure you want to accept this student?");
        alert.setContentText(
                user.getFullName() + " (" + user.getStudentId() + ")\n\n" +
                "Verify the name and Student ID against the card image before continuing. " +
                "Once accepted, the student can sign in and appears in the campus network graph."
        );
        UiDialogs.styleDialogPane(alert.getDialogPane());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (!verificationService.approveStudent(user.getStudentId())) {
                    UiDialogs.showError("Could not approve this student. Please try again.");
                    return;
                }
                networkService.reloadGraph();
                showVerificationQueueScreen();
            }
        });
    }

    private void confirmDeleteStudent(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Student");
        alert.setHeaderText("Remove " + user.getFullName() + " (" + user.getStudentId() + ")");
        alert.setContentText("Are you sure you want to permanently delete this student? All of their incident connections, profile data, and graph vertex entries will be removed.");
        UiDialogs.styleDialogPane(alert.getDialogPane());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (!networkService.adminDeleteStudent(user.getStudentId())) {
                    UiDialogs.showError("Could not delete this student. Administrator accounts cannot be removed here.");
                }
                showAccountsScreen();
            }
        });
    }

    // ==================================================
    // CONNECTIONS MANAGER (ADMIN EDGE CONTROL)
    // ==================================================
    private void showConnectionsManagerScreen() {
        VBox mainBox = new VBox(20);
        mainBox.setPadding(new Insets(28, 32, 28, 32));

        Label heading = new Label("Connections Manager");
        heading.getStyleClass().add("screen-heading");

        Label subtitle = new Label("Administer graph edges: add or remove friendship connections between verified students.");
        subtitle.getStyleClass().add("screen-subtitle");

        // Card 1: Add Connection
        VBox addCard = new VBox(12);
        addCard.getStyleClass().add("card");
        Label addTitle = new Label("➕ Add Graph Edge (Connection)");
        addTitle.getStyleClass().add("card-title");

        HBox formRow = new HBox(14);
        formRow.setAlignment(Pos.CENTER_LEFT);

        ComboBox<com.networkfinder.dsa.StudentNode> s1Combo = new ComboBox<>();
        s1Combo.getItems().addAll(networkService.getGraph().getAllNodes());
        s1Combo.setPromptText("Select Student A...");
        s1Combo.setMinWidth(240);

        Label arrow = new Label("⇄");
        arrow.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -fx-green;");

        ComboBox<com.networkfinder.dsa.StudentNode> s2Combo = new ComboBox<>();
        s2Combo.getItems().addAll(networkService.getGraph().getAllNodes());
        s2Combo.setPromptText("Select Student B...");
        s2Combo.setMinWidth(240);

        Button addConnBtn = new Button("+ Create Edge");
        addConnBtn.getStyleClass().add("button-primary");

        addConnBtn.setOnAction(e -> {
            com.networkfinder.dsa.StudentNode n1 = s1Combo.getValue();
            com.networkfinder.dsa.StudentNode n2 = s2Combo.getValue();
            if (n1 != null && n2 != null && !n1.getStudentId().equals(n2.getStudentId())) {
                if (!networkService.addConnection(n1.getStudentId(), n2.getStudentId())) {
                    UiDialogs.showError("Could not create the edge. Both students must be verified and distinct.");
                    return;
                }
                showConnectionsManagerScreen();
            }
        });

        formRow.getChildren().addAll(new Label("Student A:"), s1Combo, arrow, new Label("Student B:"), s2Combo, addConnBtn);
        addCard.getChildren().addAll(addTitle, formRow);

        // Card 2: Active Connections Table
        VBox listCard = new VBox(12);
        listCard.getStyleClass().add("card");

        List<String[]> pairs = networkService.getAllAcceptedConnections();
        Label listTitle = new Label("Active Network Edges (" + pairs.size() + " connections)");
        listTitle.getStyleClass().add("card-title");
        listCard.getChildren().add(listTitle);

        TableView<String[]> connTable = new TableView<>();
        connTable.getStyleClass().add("table-view");
        connTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<String[], String> colA = new TableColumn<>("Student A");
        colA.setCellValueFactory(data -> {
            String id = data.getValue()[0];
            com.networkfinder.dsa.StudentNode node = networkService.getGraph().getNode(id);
            return new SimpleStringProperty(node != null ? node.getFullName() + " (" + id + ")" : id);
        });

        TableColumn<String[], String> colB = new TableColumn<>("Student B");
        colB.setCellValueFactory(data -> {
            String id = data.getValue()[1];
            com.networkfinder.dsa.StudentNode node = networkService.getGraph().getNode(id);
            return new SimpleStringProperty(node != null ? node.getFullName() + " (" + id + ")" : id);
        });

        TableColumn<String[], Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button delBtn = new Button("Disconnect");
            {
                delBtn.getStyleClass().add("button-secondary");
                delBtn.setStyle("-fx-text-fill: -fx-danger; -fx-font-size: 11px;");
                delBtn.setOnAction(e -> {
                    String[] pair = getTableView().getItems().get(getIndex());
                    if (networkService.removeConnection(pair[0], pair[1])) {
                        showConnectionsManagerScreen();
                    } else {
                        UiDialogs.showError("Could not remove this connection.");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : delBtn);
            }
        });

        connTable.getColumns().addAll(colA, colB, actionCol);
        connTable.setItems(FXCollections.observableArrayList(pairs));
        connTable.setPrefHeight(340);

        listCard.getChildren().add(connTable);

        mainBox.getChildren().addAll(new VBox(4, heading, subtitle), addCard, listCard);
        contentArea.getChildren().setAll(mainBox);
    }

    // Secondary screen: Network Graph overview
    private void showNetworkOverviewScreen() {
        VBox mainBox = new VBox(20);
        mainBox.setPadding(new Insets(28, 32, 28, 32));

        Label heading = new Label("Network Graph Overview");
        heading.getStyleClass().add("screen-heading");

        HBox statsRow = new HBox(16);
        statsRow.getChildren().addAll(
                createStatCard("Total Vertices (Students)", String.valueOf(networkService.getGraph().getVertexCount()), "-fx-green"),
                createStatCard("Total Edges (Connections)", String.valueOf(networkService.getGraph().getEdgeCount()), "-fx-orange"),
                createStatCard("Graph Representation", "Adjacency List", "-fx-ink-soft")
        );

        VBox listCard = new VBox(10);
        listCard.getStyleClass().add("card");
        Label listTitle = new Label("Adjacency List Dump (DSA Inspection)");
        listTitle.getStyleClass().add("card-title");

        TextArea area = new TextArea();
        area.setEditable(false);
        area.setPrefHeight(360);
        StringBuilder sb = new StringBuilder();
        for (String v : networkService.getGraph().getAllVertexIds()) {
            sb.append("Node [").append(v).append("] (").append(networkService.getGraph().getNode(v).getFullName()).append(")\n");
            sb.append("   Edges -> ").append(networkService.getGraph().getNeighbors(v)).append("\n\n");
        }
        area.setText(sb.toString());

        VBox canvasCard = new VBox(10);
        canvasCard.getStyleClass().add("card");
        Label canvasTitle = new Label("Visual Graph Canvas");
        canvasTitle.getStyleClass().add("card-title");
        GraphVisualizerCanvas canvas = new GraphVisualizerCanvas(networkService.getGraph());
        canvasCard.getChildren().addAll(canvasTitle, canvas);

        mainBox.getChildren().addAll(heading, statsRow, canvasCard, listCard);
        contentArea.getChildren().setAll(mainBox);
    }

    private VBox createStatCard(String label, String value, String color) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");
        card.setMinWidth(240);
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: " + color + ";");
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-ink-soft;");
        card.getChildren().addAll(v, l);
        return card;
    }
}
