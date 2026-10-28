package com.networkfinder.ui;

import com.networkfinder.database.ProfileDao;
import com.networkfinder.database.UserDao;
import com.networkfinder.model.ArchiveEntry;
import com.networkfinder.model.StudentProfile;
import com.networkfinder.model.User;
import com.networkfinder.service.InputValidator;
import com.networkfinder.service.NetworkService;
import com.networkfinder.service.ValidationException;
import com.networkfinder.service.VerificationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
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
    private Button idVerifNavBtn;

    private int verificationPage = 1;
    private int verificationPageSize = 8;
    private String verificationSearch = "";

    private int accountsPage = 1;
    private int accountsPageSize = 16;
    private String accountsSearch = "";

    private int archivePage = 1;
    private int archivePageSize = 16;
    private String archiveSearch = "";
    private boolean verificationSortNewest = false;
    private boolean accountsSortNewest = false;
    private boolean archiveSortNewest = false;

    private int connPage = 1;
    private int connPageSize = 10;
    private String connSearch = "";

    private int adjPage = 1;
    private int adjPageSize = 8;
    private String adjSearch = "";

    /**
     * Sidebar label for the verification queue includes the live pending count
     * so the admin notices new submissions without visiting the screen.
     */
    private String verificationNavText() {
        int pending = verificationService.getPendingVerifications().size();
        return pending == 0 ? "ID Verification" : "ID Verification (" + pending + ")";
    }

    private void refreshPendingBadge() {
        if (idVerifNavBtn != null) {
            idVerifNavBtn.setText(verificationNavText());
        }
    }

    @SuppressWarnings("this-escape")

    public AdminDashboardView() {
        getStyleClass().add("fluent-root-pane");

        setLeft(buildSidebar());

        ScrollPane centerScroll = new ScrollPane(contentArea);
        centerScroll.setFitToWidth(true);
        centerScroll.setFitToHeight(true);
        centerScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        setCenter(centerScroll);

        showVerificationQueueScreen();
    }

    private HBox buildScreenHeader(String titleText, String subtitleText) {
        return buildScreenHeader(titleText, subtitleText, null);
    }

    private HBox buildScreenHeader(String titleText, String subtitleText, Node rightExtra) {
        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label heading = new Label(titleText);
        heading.getStyleClass().add("screen-heading");
        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("screen-subtitle");
        titleBox.getChildren().addAll(heading, subtitle);
        titleBox.setMinWidth(280);

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        Button notifBtn = buildNotificationButton();

        HBox rightBox = new HBox(10);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        if (rightExtra != null) {
            rightBox.getChildren().add(rightExtra);
            topRow.getChildren().addAll(titleBox, leftSpacer, notifBtn, rightBox);
            return topRow;
        }

        topRow.getChildren().addAll(titleBox, leftSpacer, notifBtn);
        return topRow;
    }

    private Button buildNotificationButton() {
        Button btn = new Button();
        btn.getStyleClass().add("notification-bell-btn");

        int pendingCount = verificationService.getPendingVerifications().size();
        HBox content = new HBox(6);
        content.setAlignment(Pos.CENTER);

        Node bell = SvgIcons.icon(SvgIcons.Glyph.BELL, 17, pendingCount > 0 ? "svg-icon-warn" : "svg-icon");
        content.getChildren().add(bell);

        if (pendingCount > 0) {
            Label badge = new Label(String.valueOf(pendingCount));
            badge.getStyleClass().add("notification-badge");
            content.getChildren().add(badge);
        }

        btn.setGraphic(content);
        btn.setTooltip(new Tooltip(pendingCount + " pending notification(s)"));
        btn.setOnAction(e -> showNotificationsModal());
        return btn;
    }

    private void showNotificationsModal() {
        List<User> pendingList = verificationService.getPendingVerifications();
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Notifications Center");
        dialog.setHeaderText("Administrator Notifications & System Alerts");
        UiDialogs.styleDialogPane(dialog.getDialogPane());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox box = new VBox(14);
        box.setPrefWidth(520);
        box.setPadding(new Insets(8, 2, 10, 2));

        if (pendingList.isEmpty()) {
            VBox emptyCard = new VBox(10);
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(26, 20, 26, 20));
            Node checkIcon = SvgIcons.icon(SvgIcons.Glyph.CHECK, 34, "svg-icon-success");
            Label emptyLbl = new Label("All caught up!");
            emptyLbl.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: -fx-ink;");
            Label subLbl = new Label("There are no pending student verification requests.");
            subLbl.setStyle("-fx-text-fill: -fx-ink-soft; -fx-font-size: 14px;");
            emptyCard.getChildren().addAll(checkIcon, emptyLbl, subLbl);
            box.getChildren().add(emptyCard);
        } else {
            Label header = new Label(pendingList.size() + " Pending ID Verification Request" + (pendingList.size() > 1 ? "s" : ""));
            header.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: -fx-amber;");
            box.getChildren().add(header);

            for (User u : pendingList) {
                HBox item = new HBox(14);
                item.getStyleClass().add("notification-item");
                item.setAlignment(Pos.CENTER_LEFT);

                StackPane iconBox = new StackPane();
                iconBox.getStyleClass().add("fluent-status-icon");
                iconBox.setPadding(new Insets(9));
                iconBox.setStyle("-fx-background-color: -fx-amber-tint; -fx-background-radius: 10px; -fx-border-color: rgba(245, 158, 11, 0.35); -fx-border-radius: 10px;");
                iconBox.getChildren().add(SvgIcons.icon(SvgIcons.Glyph.CLIPBOARD_LIST, 20, "svg-icon-warn"));

                VBox textCol = new VBox(3);
                Label name = new Label(u.getFullName() + "  (" + u.getStudentId() + ")");
                name.setStyle("-fx-font-weight: 700; -fx-font-size: 15px; -fx-text-fill: -fx-ink;");
                Label sub = new Label("Submitted an ID card verification request");
                sub.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-ink-soft;");
                textCol.getChildren().addAll(name, sub);
                HBox.setHgrow(textCol, Priority.ALWAYS);

                Button reviewBtn = new Button("Review");
                reviewBtn.getStyleClass().add("button-primary");
                reviewBtn.setStyle("-fx-font-size: 13px; -fx-padding: 7px 18px;");
                reviewBtn.setOnAction(ev -> {
                    dialog.close();
                    showVerificationReviewScreen(u);
                });

                item.getChildren().addAll(iconBox, textCol, reviewBtn);
                box.getChildren().add(item);
            }
        }

        VBox sysCard = new VBox(5);
        sysCard.getStyleClass().add("info-card");
        sysCard.setPadding(new Insets(14, 16, 14, 16));
        Label sysTitle = new Label("System Network Status");
        sysTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: -fx-accent;");
        Label sysDesc = new Label("Vertices: " + networkService.getGraph().getVertexCount() + " students  |  Edges: " + networkService.getGraph().getEdgeCount() + " active connections.");
        sysDesc.setStyle("-fx-font-size: 13.5px; -fx-text-fill: -fx-ink-soft;");
        sysCard.getChildren().addAll(sysTitle, sysDesc);
        box.getChildren().add(sysCard);

        dialog.getDialogPane().setContent(box);
        dialog.showAndWait();
    }

    private Node buildSidebar() {
        BorderPane sidebar = new BorderPane();
        sidebar.getStyleClass().add("sidebar");

        Text title = new Text("SOCIAL NETWORK\nCONNECTION FINDER");
        title.getStyleClass().add("sidebar-title");
        title.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label adminBadge = new Label("ADMIN PANEL");
        adminBadge.setStyle("-fx-background-color: -fx-amber; -fx-text-fill: #16161C; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 5 14; -fx-background-radius: 10;");

        Region brandAccent = new Region();
        brandAccent.setPrefSize(56, 3);
        brandAccent.setStyle("-fx-background-color: -fx-accent; -fx-background-radius: 2;");

        VBox titleBox = new VBox(8, title, adminBadge, brandAccent);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 0, 16, 0));

        Label userMgmtLabel = new Label("USER MANAGEMENT");
        userMgmtLabel.getStyleClass().add("sidebar-section-heading");

        Button idVerifBtn = createNavButton(verificationNavText(), true, SvgIcons.Glyph.CLIPBOARD_LIST);
        idVerifNavBtn = idVerifBtn;

        Button accountsBtn = createNavButton("Student Accounts", false, SvgIcons.Glyph.USERS);
        Button archiveBtn = createNavButton("Archive", false, SvgIcons.Glyph.ARCHIVE);

        idVerifBtn.setOnAction(e -> {
            setActiveNav(idVerifBtn);
            showVerificationQueueScreen();
        });

        accountsBtn.setOnAction(e -> {
            setActiveNav(accountsBtn);
            showAccountsScreen();
        });

        archiveBtn.setOnAction(e -> {
            setActiveNav(archiveBtn);
            showArchiveScreen();
        });

        Label networkLabel = new Label("NETWORK");
        networkLabel.getStyleClass().add("sidebar-section-heading");

        Button netOverviewBtn = createNavButton("Network Graph", false, SvgIcons.Glyph.SHARE_2);
        netOverviewBtn.setOnAction(e -> {
            setActiveNav(netOverviewBtn);
            showNetworkOverviewScreen();
        });

        Button connManagerBtn = createNavButton("Connections Manager", false, SvgIcons.Glyph.LINK);
        connManagerBtn.setOnAction(e -> {
            setActiveNav(connManagerBtn);
            showConnectionsManagerScreen();
        });

        Button logoutBtn = createNavButton("Logout", false, SvgIcons.Glyph.LOG_OUT);
        logoutBtn.setStyle("-fx-text-fill: -fx-danger;");
        logoutBtn.setOnAction(e -> {
            if (UiDialogs.confirmSignOut()) {
                ScreenNavigator.showStudentLogin();
            }
        });

        VBox navBox = new VBox(8, userMgmtLabel, idVerifBtn, accountsBtn, archiveBtn, networkLabel, netOverviewBtn, connManagerBtn);
        VBox logoutBox = new VBox(logoutBtn);

        sidebar.setTop(titleBox);
        sidebar.setCenter(navBox);
        sidebar.setBottom(logoutBox);

        return sidebar;
    }

    private Button createNavButton(String text, boolean active) {
        return createNavButton(text, active, null);
    }

    private Button createNavButton(String text, boolean active, SvgIcons.Glyph glyph) {
        Button btn = new Button(text);
        if (glyph != null) {
            if (glyph == SvgIcons.Glyph.LOG_OUT) {
                btn.setGraphic(SvgIcons.icon(glyph, 17, "svg-icon-danger"));
            } else {
                btn.setGraphic(SvgIcons.icon(glyph, 17));
            }
        }
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
        refreshPendingBadge();

        VBox mainBox = new VBox(18);
        mainBox.setPadding(new Insets(24, 32, 28, 32));
        VBox.setVgrow(mainBox, Priority.ALWAYS);

        // Header with title, subtitle and notification bell
        HBox header = buildScreenHeader("Student ID Verification", "Review pending student registration requests.");

        List<User> pendingList = verificationService.getPendingVerifications();

        if (pendingList.isEmpty() && (verificationSearch == null || verificationSearch.isBlank())) {
            VBox emptyCard = new VBox(14);
            emptyCard.getStyleClass().add("card");
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(48));
            VBox.setVgrow(emptyCard, Priority.ALWAYS);

            Node checkIcon = SvgIcons.icon(SvgIcons.Glyph.CHECK, 42, "svg-icon-success");
            Label emptyText = new Label("No pending ID verification requests at this time.");
            emptyText.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -fx-ink;");
            Label subEmpty = new Label("All registered students have been reviewed and approved.");
            subEmpty.setStyle("-fx-text-fill: -fx-ink-soft; -fx-font-size: 13px;");

            emptyCard.getChildren().addAll(checkIcon, emptyText, subEmpty);
            mainBox.getChildren().addAll(header, emptyCard);
        } else {
            // Real-Time Search Bar
            HBox filterBar = new HBox(14);
            filterBar.setAlignment(Pos.CENTER_LEFT);

            TextField searchField = new TextField(verificationSearch);
            searchField.setPromptText("Search pending students by name or ID...");
            searchField.getStyleClass().add("realtime-search-field");
            HBox.setHgrow(searchField, Priority.ALWAYS);

            Label totalBadge = new Label();
            totalBadge.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-ink-soft;");

            Button oldestBtn = new Button("↑ Oldest first");
            Button newestBtn = new Button("↓ Newest first");
            oldestBtn.getStyleClass().add("button-secondary");
            newestBtn.getStyleClass().add("button-secondary");
            oldestBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
            newestBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
            applySortSelection(oldestBtn, newestBtn, verificationSortNewest);

            filterBar.getChildren().addAll(searchField, oldestBtn, newestBtn, totalBadge);

            // Table
            TableView<User> table = new TableView<>();
            table.getStyleClass().add("table-view");
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            VBox.setVgrow(table, Priority.ALWAYS);
            table.setMaxHeight(Double.MAX_VALUE);
            table.setMinHeight(280);

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
                        Label badge = new Label("Pending");
                        badge.setGraphic(new Circle(3.5));
                        badge.setGraphicTextGap(6);
                        badge.getStyleClass().add("badge-pending");
                        setGraphic(badge);
                    }
                }
            });

            TableColumn<User, String> submittedCol = new TableColumn<>("Submitted");
            submittedCol.setCellValueFactory(data -> new SimpleStringProperty(formatTimestamp(data.getValue().getCreatedAt())));

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
                    setAlignment(Pos.CENTER);
                    setGraphic(empty ? null : viewBtn);
                }
            });

            // Fixed structural widths: pinned columns never move, flexible
            // ones share the remaining width proportionally.
            pinColumn(idCol, 112);
            flexColumn(nameCol, 160, 210);
            flexColumn(courseCol, 110, 150);
            pinColumn(statusCol, 150);
            flexColumn(submittedCol, 150, 175);
            pinColumn(actionCol, 104);

            table.getColumns().addAll(List.of(idCol, nameCol, courseCol, statusCol, submittedCol, actionCol));
            table.getColumns().forEach(col -> col.setSortable(false));
            lockColumnOrder(table);
            table.setPlaceholder(new Label("No pending ID verification requests across the entire network."));

            // Pagination Controls Bar
            HBox paginationBar = new HBox(12);
            paginationBar.getStyleClass().add("pagination-container");
            paginationBar.setAlignment(Pos.CENTER);

            Label pageSizeLabel = new Label("Show per page:");
            pageSizeLabel.getStyleClass().add("helper-text");

            ComboBox<Integer> pageSizeCombo = new ComboBox<>();
            pageSizeCombo.getItems().addAll(5, 8, 10, 20);
            pageSizeCombo.setValue(verificationPageSize);
            pageSizeCombo.setStyle("-fx-pref-width: 80px; -fx-padding: 2 6;");

            Button prevBtn = new Button("← Previous");
            prevBtn.getStyleClass().add("pagination-btn");

            Label pageInfo = new Label();
            pageInfo.getStyleClass().add("pagination-info");

            Button nextBtn = new Button("Next →");
            nextBtn.getStyleClass().add("pagination-btn");

            paginationBar.getChildren().addAll(pageSizeLabel, pageSizeCombo, prevBtn, pageInfo, nextBtn);

            // Real-time Update & Pagination Renderer
            Runnable updateTable = () -> {
                List<User> currentPending = verificationService.getPendingVerifications();
                String query = (searchField.getText() == null) ? "" : searchField.getText().trim().toLowerCase();
                java.util.Comparator<User> createdAtOrder = java.util.Comparator.comparing(
                        User::getCreatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
                if (verificationSortNewest) {
                    createdAtOrder = createdAtOrder.reversed();
                }
                List<User> filtered = currentPending.stream()
                        .filter(u -> query.isEmpty()
                                || (u.getFullName() != null && u.getFullName().toLowerCase().contains(query))
                                || (u.getStudentId() != null && u.getStudentId().toLowerCase().contains(query)))
                        .sorted(createdAtOrder)
                        .toList();

                totalBadge.setText(filtered.size() + " of " + currentPending.size() + " pending");

                int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / verificationPageSize));
                if (verificationPage > totalPages) {
                    verificationPage = totalPages;
                }
                if (verificationPage < 1) {
                    verificationPage = 1;
                }

                int from = (verificationPage - 1) * verificationPageSize;
                int to = Math.min(from + verificationPageSize, filtered.size());
                List<User> paged = (from <= to && from < filtered.size()) ? filtered.subList(from, to) : List.of();

                table.setItems(FXCollections.observableArrayList(paged));

                pageInfo.setText("Page " + verificationPage + " of " + totalPages + " (" + filtered.size() + " total)");
                prevBtn.setDisable(verificationPage <= 1);
                nextBtn.setDisable(verificationPage >= totalPages || filtered.isEmpty());
            };

            // Real-time search: Automatically loads on typing without clicking button or enter
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                verificationSearch = (newVal == null) ? "" : newVal;
                verificationPage = 1;
                updateTable.run();
            });

            prevBtn.setOnAction(e -> {
                if (verificationPage > 1) {
                    verificationPage--;
                    updateTable.run();
                }
            });

            nextBtn.setOnAction(e -> {
                verificationPage++;
                updateTable.run();
            });

            pageSizeCombo.setOnAction(e -> {
                if (pageSizeCombo.getValue() != null) {
                    verificationPageSize = pageSizeCombo.getValue();
                    verificationPage = 1;
                    updateTable.run();
                }
            });

            oldestBtn.setOnAction(e -> {
                if (!verificationSortNewest) return;
                verificationSortNewest = false;
                applySortSelection(oldestBtn, newestBtn, false);
                verificationPage = 1;
                updateTable.run();
            });

            newestBtn.setOnAction(e -> {
                if (verificationSortNewest) return;
                verificationSortNewest = true;
                applySortSelection(oldestBtn, newestBtn, true);
                verificationPage = 1;
                updateTable.run();
            });

            updateTable.run();

            mainBox.getChildren().addAll(header, filterBar, table, paginationBar);
        }

        contentArea.getChildren().setAll(mainBox);
    }

    // ==================================================
    // SCREEN 6 — ADMIN VIEW VERIFICATION (SIDE-BY-SIDE)
    // ==================================================
    private void showVerificationReviewScreen(User user) {
        VBox mainBox = new VBox(20);
        mainBox.setPadding(new Insets(24, 32, 28, 32));
        VBox.setVgrow(mainBox, Priority.ALWAYS);

        // Header with Back button & notification bell
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = new Button("← Back to List");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setOnAction(e -> showVerificationQueueScreen());

        Label heading = new Label("Student Verification");
        heading.getStyleClass().add("screen-heading");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Button notifBtn = buildNotificationButton();

        topBar.getChildren().addAll(backBtn, heading, topSpacer, notifBtn);

        // Side-by-side card
        HBox splitCard = new HBox(30);
        splitCard.getStyleClass().add("card");
        splitCard.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(splitCard, Priority.ALWAYS);

        // Left: Student Information
        VBox infoBox = new VBox(14);
        infoBox.setMinWidth(360);
        infoBox.setMaxWidth(440);

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
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setMaxWidth(Double.MAX_VALUE);
        Button approveBtn = new Button("✓ APPROVE");
        approveBtn.getStyleClass().add("button-primary");
        approveBtn.getStyleClass().add("button-lg");
        approveBtn.setPrefWidth(150);
        approveBtn.setOnAction(e -> confirmApproveStudent(user));

        Button rejectBtn = new Button("✕ REJECT");
        rejectBtn.getStyleClass().add("button-danger");
        rejectBtn.getStyleClass().add("button-lg");
        rejectBtn.setPrefWidth(150);
        rejectBtn.setOnAction(e -> confirmRejectStudent(user));

        buttonBox.getChildren().addAll(approveBtn, rejectBtn);

        infoBox.getChildren().addAll(infoHeading, grid, checkQuestion, notice, buttonBox);

        // Right: Submitted Student ID Image
        VBox imageBox = new VBox(10);
        imageBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(imageBox, Priority.ALWAYS);
        VBox.setVgrow(imageBox, Priority.ALWAYS);

        Label imgLabel = new Label("Submitted Student ID");
        imgLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: -fx-ink-soft;");

        StackPane imgContainer = new StackPane();
        imgContainer.setStyle("-fx-background-color: -fx-sage-tint; -fx-border-color: -fx-border; -fx-border-radius: 8; -fx-background-radius: 8;");
        imgContainer.setMinHeight(360);
        imgContainer.setMinWidth(460);
        VBox.setVgrow(imgContainer, Priority.ALWAYS);

        ImageView idImageView = new ImageView();
        idImageView.setFitWidth(560);
        idImageView.setFitHeight(360);
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
            Label placeholder = new Label("[ Placeholder: No Student ID image uploaded ]");
            placeholder.setGraphic(SvgIcons.icon(SvgIcons.Glyph.CAMERA, 18, "svg-icon"));
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
        refreshPendingBadge();

        VBox mainBox = new VBox(16);
        mainBox.setPadding(new Insets(24, 32, 28, 32));
        VBox.setVgrow(mainBox, Priority.ALWAYS);

        Button addStudentBtn = new Button("Add New Student");
        addStudentBtn.getStyleClass().add("button-primary");
        addStudentBtn.setGraphic(SvgIcons.icon(SvgIcons.Glyph.PLUS, 14, "svg-icon-primary"));
        addStudentBtn.setOnAction(e -> showAddStudentDialog());

        // Header with centered notification icon and Add Student button on the right
        HBox topRow = buildScreenHeader(
                "Student Accounts & Directory",
                "Administer registered students: add accounts, edit details, or remove students from the graph.",
                addStudentBtn
        );

        // Real-Time Search Bar
        HBox searchRow = new HBox(14);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField(accountsSearch);
        searchField.setPromptText("Search accounts by name or ID...");
        searchField.getStyleClass().add("realtime-search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Label countBadge = new Label();
        countBadge.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-ink-soft;");

        Button oldestBtn = new Button("↑ Oldest first");
        Button newestBtn = new Button("↓ Newest first");
        oldestBtn.getStyleClass().add("button-secondary");
        newestBtn.getStyleClass().add("button-secondary");
        oldestBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
        newestBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
        applySortSelection(oldestBtn, newestBtn, accountsSortNewest);

        searchRow.getChildren().addAll(searchField, oldestBtn, newestBtn, countBadge);

        TableView<User> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setMaxHeight(Double.MAX_VALUE);
        table.setMinHeight(300);

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
                    b.setGraphic(new Circle(3.5));
                    b.setGraphicTextGap(6);
                    b.setStyle("-fx-background-color: -fx-green-tint; -fx-text-fill: -fx-green; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: 700; -fx-font-size: 12.5px;");
                    setGraphic(b);
                } else if ("PENDING".equals(item)) {
                    Label b = new Label("Pending");
                    b.setGraphic(new Circle(3.5));
                    badgePending(b);
                    setGraphic(b);
                } else {
                    Label b = new Label("✕ " + item);
                    b.setStyle("-fx-background-color: -fx-danger-tint; -fx-text-fill: -fx-danger; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: 700; -fx-font-size: 12.5px;");
                    setGraphic(b);
                }
            }

            private void badgePending(Label b) {
                b.setGraphic(new Circle(3.5));
                b.setGraphicTextGap(6);
                b.getStyleClass().add("badge-pending");
            }
        });

        TableColumn<User, String> registeredCol = new TableColumn<>("Registered");
        registeredCol.setCellValueFactory(data -> new SimpleStringProperty(formatTimestamp(data.getValue().getCreatedAt())));

        TableColumn<User, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button approveBtn = new Button("Approve");
            private final Button resetBtn = new Button("Reset PW");
            private final Button delBtn = new Button("Delete");
            private final HBox btnBox = new HBox(6, editBtn, approveBtn, resetBtn, delBtn);

            {
                btnBox.setAlignment(Pos.CENTER);
                editBtn.getStyleClass().add("button-secondary");
                editBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
                editBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    showEditStudentDialog(u);
                });

                approveBtn.getStyleClass().add("button-primary");
                approveBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
                approveBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    confirmApproveFromAccounts(u);
                });

                resetBtn.getStyleClass().add("button-secondary");
                resetBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
                resetBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    showResetPasswordDialog(u);
                });

                delBtn.getStyleClass().add("button-ghost-danger");
                delBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
                delBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    confirmDeleteStudent(u);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty) {
                    setGraphic(null);
                } else {
                    User u = getTableView().getItems().get(getIndex());
                    if (u.getRole() == com.networkfinder.model.Role.ADMIN) {
                        Label admLbl = new Label("Admin");
                        admLbl.setStyle("-fx-text-fill: -fx-ink-soft; -fx-font-size: 12px; -fx-font-style: italic;");
                        setGraphic(admLbl);
                    } else {
                        boolean needsApproval = u.getVerificationStatus() != com.networkfinder.model.VerificationStatus.APPROVED;
                        approveBtn.setVisible(needsApproval);
                        approveBtn.setManaged(needsApproval);
                        setGraphic(btnBox);
                    }
                }
            }
        });

        // Fixed structural widths: pinned columns never move, flexible ones
        // share the remaining width proportionally (no more crushed columns).
        pinColumn(idCol, 112);
        flexColumn(nameCol, 150, 195);
        flexColumn(courseCol, 110, 150);
        flexColumn(secCol, 85, 105);
        pinColumn(statusCol, 150);
        flexColumn(registeredCol, 125, 155);
        flexColumn(actionCol, 268, 292);

        table.getColumns().addAll(List.of(idCol, nameCol, courseCol, secCol, statusCol, registeredCol, actionCol));
        table.getColumns().forEach(col -> col.setSortable(false));
        lockColumnOrder(table);
        table.setPlaceholder(new Label("No student accounts found. Add a new student or refine the search."));

        // Pagination Bar
        HBox paginationBar = new HBox(12);
        paginationBar.getStyleClass().add("pagination-container");
        paginationBar.setAlignment(Pos.CENTER);

        Button prevBtn = new Button("← Previous");
        prevBtn.getStyleClass().add("pagination-btn");

        Label pageInfo = new Label();
        pageInfo.getStyleClass().add("pagination-info");

        Button nextBtn = new Button("Next →");
        nextBtn.getStyleClass().add("pagination-btn");

        paginationBar.getChildren().addAll(prevBtn, pageInfo, nextBtn);

        // Real-Time Table and Pagination Renderer
        Runnable updateAccountsTable = () -> {
            List<User> allUsers = userDao.getAllUsers();
            String query = (searchField.getText() == null) ? "" : searchField.getText().trim().toLowerCase();
            java.util.Comparator<User> createdAtOrder = java.util.Comparator.comparing(
                    User::getCreatedAt,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            if (accountsSortNewest) {
                createdAtOrder = createdAtOrder.reversed();
            }
            List<User> filtered = allUsers.stream()
                    .filter(u -> query.isEmpty()
                            || (u.getFullName() != null && u.getFullName().toLowerCase().contains(query))
                            || (u.getStudentId() != null && u.getStudentId().toLowerCase().contains(query)))
                    .sorted(createdAtOrder)
                    .toList();

            countBadge.setText(filtered.size() + " of " + allUsers.size() + " accounts");

            int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / accountsPageSize));
            if (accountsPage > totalPages) accountsPage = totalPages;
            if (accountsPage < 1) accountsPage = 1;

            int from = (accountsPage - 1) * accountsPageSize;
            int to = Math.min(from + accountsPageSize, filtered.size());
            List<User> paged = (from <= to && from < filtered.size()) ? filtered.subList(from, to) : List.of();

            table.setItems(FXCollections.observableArrayList(paged));
            pageInfo.setText("Page " + accountsPage + " of " + totalPages + " (" + filtered.size() + " total)");
            prevBtn.setDisable(accountsPage <= 1);
            nextBtn.setDisable(accountsPage >= totalPages || filtered.isEmpty());
        };

        // Real-time automatic search on typing: no enter or button click needed
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            accountsSearch = (newVal == null) ? "" : newVal;
            accountsPage = 1;
            updateAccountsTable.run();
        });

        prevBtn.setOnAction(e -> {
            if (accountsPage > 1) {
                accountsPage--;
                updateAccountsTable.run();
            }
        });

        nextBtn.setOnAction(e -> {
            accountsPage++;
            updateAccountsTable.run();
        });

        oldestBtn.setOnAction(e -> {
            if (!accountsSortNewest) return;
            accountsSortNewest = false;
            applySortSelection(oldestBtn, newestBtn, false);
            accountsPage = 1;
            updateAccountsTable.run();
        });

        newestBtn.setOnAction(e -> {
            if (accountsSortNewest) return;
            accountsSortNewest = true;
            applySortSelection(oldestBtn, newestBtn, true);
            accountsPage = 1;
            updateAccountsTable.run();
        });

        updateAccountsTable.run();

        mainBox.getChildren().addAll(topRow, searchRow, table, paginationBar);
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
        idField.setPromptText("e.g. 20261001-A (8 digits, dash, one letter)");
        idField.setTextFormatter(TextFieldFilters.studentIdFilter());

        TextField firstNameField = new TextField();
        firstNameField.setPromptText("e.g. Juan (3+ letters, letters only)");
        firstNameField.setTextFormatter(TextFieldFilters.nameFilter(InputValidator.getMaxNameLength()));

        TextField middleField = new TextField();
        middleField.setPromptText("1-2 letters (e.g. M)");
        middleField.setTextFormatter(TextFieldFilters.shortNameFilter());

        TextField surnameField = new TextField();
        surnameField.setPromptText("e.g. Dela Cruz (3+ letters, letters only)");
        surnameField.setTextFormatter(TextFieldFilters.nameFilter(InputValidator.getMaxNameLength()));

        TextField suffixField = new TextField();
        suffixField.setPromptText("1-2 letters (e.g. Jr)");
        suffixField.setTextFormatter(TextFieldFilters.shortNameFilter());

        ComboBox<String> courseCombo = new ComboBox<>();
        courseCombo.getItems().addAll(
                "Bachelor of Science in Information Technology",
                "Bachelor of Science in Computer Science",
                "Bachelor of Science in Information Systems",
                "Bachelor of Science in Computer Engineering",
                "Bachelor of Science in Business Administration"
        );
        courseCombo.setValue("Bachelor of Science in Information Technology");

        ComboBox<String> yearCombo = new ComboBox<>();
        yearCombo.getItems().addAll("1st Year", "2nd Year", "3rd Year", "4th Year");
        yearCombo.setValue("1st Year");

        TextField sectionField = new TextField();
        sectionField.setPromptText("e.g. BSIT 1A");

        Label idLbl = new Label("Student ID");
        idLbl.getStyleClass().add("field-label");
        Label firstLbl = new Label("First Name");
        firstLbl.getStyleClass().add("field-label");
        Label midLbl = new Label("Middle Initial (Optional)");
        midLbl.getStyleClass().add("field-label");
        Label surLbl = new Label("Surname");
        surLbl.getStyleClass().add("field-label");
        Label sfxLbl = new Label("Suffix (Optional)");
        sfxLbl.getStyleClass().add("field-label");
        Label crsLbl = new Label("Course");
        crsLbl.getStyleClass().add("field-label");
        Label yrLbl = new Label("Year Level");
        yrLbl.getStyleClass().add("field-label");
        Label secLbl = new Label("Section");
        secLbl.getStyleClass().add("field-label");

        grid.addRow(0, idLbl, idField);
        grid.addRow(1, firstLbl, firstNameField);
        grid.addRow(2, midLbl, middleField);
        grid.addRow(3, surLbl, surnameField);
        grid.addRow(4, sfxLbl, suffixField);
        grid.addRow(5, crsLbl, courseCombo);
        grid.addRow(6, yrLbl, yearCombo);
        grid.addRow(7, secLbl, sectionField);

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

                if (sId.isEmpty() || sFirst.isEmpty() || sSurname.isEmpty()) {
                    validationError.set("Student ID, first name and surname are required.");
                    return false;
                }
                try {
                    boolean created = networkService.adminAddStudent(sId, sFirst, sMiddle, sSurname, sSuffix, sCourse, sYear, sSec);
                    if (!created) {
                        validationError.set("This Student ID may already exist, or the account could not be created.");
                    }
                    return created;
                } catch (ValidationException ve) {
                    validationError.set(ve.getMessage());
                    return false;
                } catch (Exception e) {
                    validationError.set(e.getMessage() == null ? "Unexpected error while saving." : e.getMessage());
                    return false;
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
        firstNameField.setPromptText("e.g. Juan (3+ letters, letters only)");
        firstNameField.setTextFormatter(TextFieldFilters.nameFilter(InputValidator.getMaxNameLength()));

        TextField middleField = new TextField(middle);
        middleField.setPromptText("1-2 letters (e.g. M)");
        middleField.setTextFormatter(TextFieldFilters.shortNameFilter());

        TextField surnameField = new TextField(surname);
        surnameField.setPromptText("e.g. Dela Cruz (3+ letters, letters only)");
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
                "Bachelor of Science in Computer Engineering",
                "Bachelor of Science in Business Administration"
        );
        if (currentCourse != null && !courseCombo.getItems().contains(currentCourse)) {
            courseCombo.getItems().add(currentCourse);
        }
        courseCombo.setValue(currentCourse);

        ComboBox<String> yearCombo = new ComboBox<>();
        yearCombo.getItems().addAll("1st Year", "2nd Year", "3rd Year", "4th Year");
        yearCombo.setValue(currentYear);

        TextField sectionField = new TextField(currentSec);

        Label efirstLbl = new Label("First Name");
        efirstLbl.getStyleClass().add("field-label");
        Label emidLbl = new Label("Middle Initial (Optional)");
        emidLbl.getStyleClass().add("field-label");
        Label esurLbl = new Label("Surname");
        esurLbl.getStyleClass().add("field-label");
        Label esfxLbl = new Label("Suffix (Optional)");
        esfxLbl.getStyleClass().add("field-label");
        Label ecrsLbl = new Label("Course");
        ecrsLbl.getStyleClass().add("field-label");
        Label eyrLbl = new Label("Year Level");
        eyrLbl.getStyleClass().add("field-label");
        Label esecLbl = new Label("Section");
        esecLbl.getStyleClass().add("field-label");

        grid.addRow(0, efirstLbl, firstNameField);
        grid.addRow(1, emidLbl, middleField);
        grid.addRow(2, esurLbl, surnameField);
        grid.addRow(3, esfxLbl, suffixField);
        grid.addRow(4, ecrsLbl, courseCombo);
        grid.addRow(5, eyrLbl, yearCombo);
        grid.addRow(6, esecLbl, sectionField);

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
                if (sFirst.isEmpty() || sSurname.isEmpty()) {
                    validationError.set("First name and surname are required.");
                    return false;
                }
                try {
                    boolean saved = networkService.adminEditStudent(user.getStudentId(), sFirst, sMiddle, sSurname, sSuffix, sCourse, sYear, sSec);
                    if (!saved) {
                        validationError.set("Student record not found. It may have been archived or deleted.");
                    }
                    return saved;
                } catch (ValidationException ve) {
                    validationError.set(ve.getMessage());
                    return false;
                } catch (Exception e) {
                    validationError.set(e.getMessage() == null ? "Unexpected error while saving." : e.getMessage());
                    return false;
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
    private static final java.time.format.DateTimeFormatter TIMESTAMP_FMT =
            java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private static final java.security.SecureRandom TEMP_RANDOM = new java.security.SecureRandom();

    private static String formatTimestamp(java.time.LocalDateTime value) {
        return value != null ? value.format(TIMESTAMP_FMT) : "-";
    }

    private static java.time.LocalDateTime parseTimestamp(String raw) {
        try {
            return raw == null ? null : java.time.LocalDateTime.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private void applySortSelection(Button oldestBtn, Button newestBtn, boolean newestFirst) {
        oldestBtn.getStyleClass().remove("sort-btn-active");
        newestBtn.getStyleClass().remove("sort-btn-active");
        (newestFirst ? newestBtn : oldestBtn).getStyleClass().add("sort-btn-active");
    }

    private String generateTempPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnpqrstuvwxyz";
        String digits = "23456789";
        String all = upper + lower + digits;
        StringBuilder password = new StringBuilder(12);
        password.append(upper.charAt(TEMP_RANDOM.nextInt(upper.length())));
        password.append(lower.charAt(TEMP_RANDOM.nextInt(lower.length())));
        password.append(digits.charAt(TEMP_RANDOM.nextInt(digits.length())));
        for (int i = password.length(); i < 12; i++) {
            password.append(all.charAt(TEMP_RANDOM.nextInt(all.length())));
        }
        for (int i = password.length() - 1; i > 0; i--) {
            int j = TEMP_RANDOM.nextInt(i + 1);
            char tmp = password.charAt(i);
            password.setCharAt(i, password.charAt(j));
            password.setCharAt(j, tmp);
        }
        return password.toString();
    }

    /**
     * Pins a table column to an exact width: it can never grow, shrink or be
     * dragged, so structural columns (IDs, status, actions) keep their size
     * under the constrained resize policy.
     */
    private <S, T> void pinColumn(TableColumn<S, T> col, double width) {
        col.setMinWidth(width);
        col.setPrefWidth(width);
        col.setMaxWidth(width);
        col.setResizable(false);
    }

    /**
     * Lets a column flex with the table width, sharing leftover space
     * proportionally instead of being crushed to the JavaFX ~29px default
     * minimum. Keeps a sane floor so text truncates gracefully.
     */
    private <S, T> void flexColumn(TableColumn<S, T> col, double minWidth, double prefWidth) {
        col.setMinWidth(minWidth);
        col.setPrefWidth(prefWidth);
        col.setMaxWidth(Double.MAX_VALUE);
        col.setResizable(true);
    }

    private static <T> void lockColumnOrder(TableView<T> table) {
        javafx.collections.ObservableList<TableColumn<T, ?>> columns = table.getColumns();
        List<TableColumn<T, ?>> fixedOrder = new java.util.ArrayList<>(columns);
        columns.addListener((javafx.collections.ListChangeListener<TableColumn<T, ?>>) change -> {
            if (!columns.equals(fixedOrder)) {
                columns.setAll(fixedOrder);
            }
        });

        table.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                javafx.application.Platform.runLater(() -> {
                    for (javafx.scene.Node header : table.lookupAll(".column-header")) {
                        header.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, e -> e.consume());
                    }
                });
            }
        });
    }

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
        passwordValue.setValue(generateTempPassword());

        Button regenerateBtn = new Button("↻ Generate New");
        regenerateBtn.getStyleClass().add("button-secondary");
        regenerateBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
        regenerateBtn.setOnAction(e -> passwordValue.setValue(generateTempPassword()));

        HBox passRow = new HBox(8, passwordValue, regenerateBtn);
        HBox.setHgrow(passwordValue, Priority.ALWAYS);

        Label hintLabel = new Label("A secure temporary password is generated automatically. Give it to the student — they can change it on their Account Settings page. Use the eye button to reveal it, or Generate New for a different one.");
        hintLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-ink-soft; -fx-wrap-text: true;");

        grid.addRow(0, new Label("Temporary Password:"), passRow);

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

    /**
     * rejecting a student is irreversible from the queue (they cannot sign in
     * afterwards and would have to register again), so the admin must confirm
     * the exact account being rejected.
     */
    private void confirmRejectStudent(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reject Student");
        alert.setHeaderText("Are you sure you want to reject this student?");
        alert.setContentText(
                user.getFullName() + " (" + user.getStudentId() + ")\n\n" +
                "Double-check the rejected details against the submitted ID above. " +
                "Once rejected, the account is marked as REJECTED and the student cannot sign in " +
                "or join the campus network until the registration is accepted again."
        );
        UiDialogs.styleDialogPane(alert.getDialogPane());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (!verificationService.rejectStudent(user.getStudentId())) {
                    UiDialogs.showError("Could not reject this student. Please try again.");
                    return;
                }
                showVerificationQueueScreen();
            }
        });
    }

    private void confirmDeleteStudent(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Student");
        alert.setHeaderText("Are you sure you want to delete this student?");
        alert.setContentText(
                user.getFullName() + " (" + user.getStudentId() + ")\n\n" +
                "All of their incident connections and graph vertex entries will be removed. " +
                "The account and its profile details are kept in the Archive, where it can be restored later."
        );
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

    /**
     * Approving from the directory covers pending or rejected accounts that
     * never went through the verification queue (e.g. hand-recovered students).
     */
    private void confirmApproveFromAccounts(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Approve Student");
        alert.setHeaderText("Are you sure you want to approve this student?");
        alert.setContentText(
                user.getFullName() + " (" + user.getStudentId() + ")\n\n" +
                "Approving grants sign-in access and adds the student to the campus network graph. "
                + "Verify their details first; rejected accounts in the demo had their registration declined.");
        UiDialogs.styleDialogPane(alert.getDialogPane());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (!verificationService.approveStudent(user.getStudentId())) {
                    UiDialogs.showError("Could not approve this student. Please try again.");
                    return;
                }
                networkService.reloadGraph();
                refreshPendingBadge();
                showAccountsScreen();
            }
        });
    }

    // ==================================================
    // ARCHIVE (DELETED ACCOUNTS, RESTORE / PURGE)
    // ==================================================
    private void showArchiveScreen() {
        refreshPendingBadge();

        VBox mainBox = new VBox(16);
        mainBox.setPadding(new Insets(24, 32, 28, 32));
        VBox.setVgrow(mainBox, Priority.ALWAYS);

        HBox header = buildScreenHeader("Deleted Accounts Archive",
                "Deleted student accounts are kept here. Restoring puts the account back into the network.");

        List<ArchiveEntry> entries = networkService.getArchivedStudents();

        if (entries.isEmpty() && (archiveSearch == null || archiveSearch.isBlank())) {
            VBox emptyCard = new VBox(12);
            emptyCard.getStyleClass().add("card");
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(40));
            VBox.setVgrow(emptyCard, Priority.ALWAYS);

            Node icon = SvgIcons.icon(SvgIcons.Glyph.ARCHIVE, 34);
            Label emptyText = new Label("The archive is empty.");
            emptyText.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -fx-ink;");
            Label subEmpty = new Label("Student accounts you delete from Student Accounts will be kept here.");
            subEmpty.setStyle("-fx-text-fill: -fx-ink-soft;");

            emptyCard.getChildren().addAll(icon, emptyText, subEmpty);
            mainBox.getChildren().addAll(header, emptyCard);
        } else {
            // Real-Time Search Bar
            HBox searchRow = new HBox(14);
            searchRow.setAlignment(Pos.CENTER_LEFT);

            TextField searchField = new TextField(archiveSearch);
            searchField.setPromptText("Search archived accounts by name or ID...");
            searchField.getStyleClass().add("realtime-search-field");
            HBox.setHgrow(searchField, Priority.ALWAYS);

            Label countBadge = new Label();
            countBadge.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-ink-soft;");

            Button oldestBtn = new Button("↑ Oldest first");
            Button newestBtn = new Button("↓ Newest first");
            oldestBtn.getStyleClass().add("button-secondary");
            newestBtn.getStyleClass().add("button-secondary");
            oldestBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
            newestBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
            applySortSelection(oldestBtn, newestBtn, archiveSortNewest);

            searchRow.getChildren().addAll(searchField, oldestBtn, newestBtn, countBadge);

            TableView<ArchiveEntry> table = new TableView<>();
            table.getStyleClass().add("table-view");
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            VBox.setVgrow(table, Priority.ALWAYS);
            table.setMaxHeight(Double.MAX_VALUE);
            table.setMinHeight(300);

            TableColumn<ArchiveEntry, String> idCol = new TableColumn<>("Student ID");
            idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUser().getStudentId()));

            TableColumn<ArchiveEntry, String> nameCol = new TableColumn<>("Name");
            nameCol.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().getUser().getFullName() != null
                            ? data.getValue().getUser().getFullName()
                            : data.getValue().getUser().getStudentId()));

            TableColumn<ArchiveEntry, String> courseCol = new TableColumn<>("Course");
            courseCol.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().getProfile() != null && data.getValue().getProfile().getCourse() != null
                            ? data.getValue().getProfile().getCourse()
                            : "-"));

            TableColumn<ArchiveEntry, String> statusCol = new TableColumn<>("Verification");
            statusCol.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().getUser().getVerificationStatus().name()));
            statusCol.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                    } else if ("APPROVED".equals(item)) {
                        Label b = new Label("✓ Approved");
                        b.setGraphic(new Circle(3.5));
                        b.setGraphicTextGap(6);
                        b.setStyle("-fx-background-color: -fx-green-tint; -fx-text-fill: -fx-green; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: 700; -fx-font-size: 12.5px;");
                        setGraphic(b);
                    } else if ("PENDING".equals(item)) {
                        Label b = new Label("Pending");
                        b.setGraphic(new Circle(3.5));
                        b.setGraphicTextGap(6);
                        b.getStyleClass().add("badge-pending");
                        setGraphic(b);
                    } else {
                        Label b = new Label("✕ " + item);
                        b.setStyle("-fx-background-color: -fx-danger-tint; -fx-text-fill: -fx-danger; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-weight: 700; -fx-font-size: 12.5px;");
                        setGraphic(b);
                    }
                }
            });

            TableColumn<ArchiveEntry, String> deletedCol = new TableColumn<>("Deleted On");
            deletedCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getArchivedAtDisplay()));

            TableColumn<ArchiveEntry, Void> actionCol = new TableColumn<>("Actions");
            actionCol.setCellFactory(col -> new TableCell<>() {
                private final Button restoreBtn = new Button("Restore");
                private final Button purgeBtn = new Button("Delete Forever");
                private final HBox btnBox = new HBox(6, restoreBtn, purgeBtn);

                {
                    btnBox.setAlignment(Pos.CENTER);
                    restoreBtn.getStyleClass().add("button-primary");
                    restoreBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
                    restoreBtn.setOnAction(e -> {
                        ArchiveEntry entry = getTableView().getItems().get(getIndex());
                        confirmRestoreArchived(entry);
                    });

                    purgeBtn.getStyleClass().add("button-danger");
                    purgeBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
                    purgeBtn.setOnAction(e -> {
                        ArchiveEntry entry = getTableView().getItems().get(getIndex());
                        confirmPurgeArchived(entry);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btnBox);
                }
            });

            // Fixed structural widths: pinned columns never move, flexible
            // ones share the remaining width proportionally.
            pinColumn(idCol, 112);
            flexColumn(nameCol, 150, 195);
            flexColumn(courseCol, 110, 150);
            pinColumn(statusCol, 150);
            flexColumn(deletedCol, 140, 170);
            flexColumn(actionCol, 196, 214);

            table.getColumns().addAll(List.of(idCol, nameCol, courseCol, statusCol, deletedCol, actionCol));
            table.getColumns().forEach(col -> col.setSortable(false));
            lockColumnOrder(table);
            table.setPlaceholder(new Label("Nothing archived yet. Deleted accounts will appear here."));

            // Pagination Controls Bar
            HBox paginationBar = new HBox(12);
            paginationBar.getStyleClass().add("pagination-container");
            paginationBar.setAlignment(Pos.CENTER);

            Button prevBtn = new Button("← Previous");
            prevBtn.getStyleClass().add("pagination-btn");

            Label pageInfo = new Label();
            pageInfo.getStyleClass().add("pagination-info");

            Button nextBtn = new Button("Next →");
            nextBtn.getStyleClass().add("pagination-btn");

            paginationBar.getChildren().addAll(prevBtn, pageInfo, nextBtn);

            Runnable updateArchiveTable = () -> {
                List<ArchiveEntry> currentEntries = networkService.getArchivedStudents();
                String query = (searchField.getText() == null) ? "" : searchField.getText().trim().toLowerCase();
                java.util.Comparator<ArchiveEntry> archivedOrder = java.util.Comparator.comparing(
                        (ArchiveEntry a) -> parseTimestamp(a.getArchivedAt()),
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
                if (archiveSortNewest) {
                    archivedOrder = archivedOrder.reversed();
                }
                List<ArchiveEntry> filtered = currentEntries.stream()
                        .filter(a -> {
                            if (query.isEmpty()) return true;
                            String name = a.getUser().getFullName();
                            String id = a.getUser().getStudentId();
                            return (name != null && name.toLowerCase().contains(query))
                                    || (id != null && id.toLowerCase().contains(query));
                        })
                        .sorted(archivedOrder)
                        .toList();

                countBadge.setText(filtered.size() + " of " + currentEntries.size() + " archived");

                int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / archivePageSize));
                if (archivePage > totalPages) archivePage = totalPages;
                if (archivePage < 1) archivePage = 1;

                int from = (archivePage - 1) * archivePageSize;
                int to = Math.min(from + archivePageSize, filtered.size());
                List<ArchiveEntry> paged = (from <= to && from < filtered.size()) ? filtered.subList(from, to) : List.of();

                table.setItems(FXCollections.observableArrayList(paged));
                pageInfo.setText("Page " + archivePage + " of " + totalPages + " (" + filtered.size() + " total)");
                prevBtn.setDisable(archivePage <= 1);
                nextBtn.setDisable(archivePage >= totalPages || filtered.isEmpty());
            };

            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                archiveSearch = (newVal == null) ? "" : newVal;
                archivePage = 1;
                updateArchiveTable.run();
            });

            prevBtn.setOnAction(e -> {
                if (archivePage > 1) {
                    archivePage--;
                    updateArchiveTable.run();
                }
            });

            nextBtn.setOnAction(e -> {
                archivePage++;
                updateArchiveTable.run();
            });

            oldestBtn.setOnAction(e -> {
                if (!archiveSortNewest) return;
                archiveSortNewest = false;
                applySortSelection(oldestBtn, newestBtn, false);
                archivePage = 1;
                updateArchiveTable.run();
            });

            newestBtn.setOnAction(e -> {
                if (archiveSortNewest) return;
                archiveSortNewest = true;
                applySortSelection(oldestBtn, newestBtn, true);
                archivePage = 1;
                updateArchiveTable.run();
            });

            updateArchiveTable.run();

            mainBox.getChildren().addAll(header, searchRow, table, paginationBar);
        }

        contentArea.getChildren().setAll(mainBox);
    }

    private void confirmRestoreArchived(ArchiveEntry entry) {
        String studentId = entry.getUser().getStudentId();
        String name = entry.getUser().getFullName() != null ? entry.getUser().getFullName() : studentId;

        if (networkService.isStudentIdInUse(studentId)) {
            UiDialogs.showError(
                    "Cannot restore " + name + " (" + studentId + "): a live account with this Student ID "
                    + "already exists. Delete that account first, or remove this archived copy if it is obsolete.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Restore Account");
        alert.setHeaderText("Are you sure you want to restore this student?");
        alert.setContentText(
                name + " (" + studentId + ")\n\n" +
                "The account and its profile details will be moved back to Student Accounts. " +
                "If the student's ID was verified before deletion, they can sign in with their existing password immediately.");
        UiDialogs.styleDialogPane(alert.getDialogPane());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (!networkService.restoreArchivedStudent(studentId, entry.getArchivedAt())) {
                    UiDialogs.showError(
                            "Could not restore " + name + " (" + studentId + "). A live account with this "
                            + "Student ID may have been created in the meantime.");
                    return;
                }
                UiDialogs.showSuccess(name + " (" + studentId + ") was restored and is listed in Student Accounts again.");
                showArchiveScreen();
            }
        });
    }

    private void confirmPurgeArchived(ArchiveEntry entry) {
        String studentId = entry.getUser().getStudentId();
        String name = entry.getUser().getFullName() != null ? entry.getUser().getFullName() : studentId;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Forever");
        alert.setHeaderText("Are you sure you want to permanently delete this archived account?");
        alert.setContentText(
                name + " (" + studentId + ")\n\n" +
                "The archived copy and its profile details will be destroyed for good and can never "
                + "be restored afterwards. This action cannot be undone.");
        UiDialogs.styleDialogPane(alert.getDialogPane());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (!networkService.purgeArchivedStudent(studentId, entry.getArchivedAt())) {
                    UiDialogs.showError("Could not delete this archived account. Please try again.");
                    return;
                }
                showArchiveScreen();
            }
        });
    }

    // ==================================================
    // CONNECTIONS MANAGER (ADMIN EDGE CONTROL)
    // ==================================================
    private void showConnectionsManagerScreen() {
        VBox mainBox = new VBox(20);
        mainBox.setPadding(new Insets(24, 32, 28, 32));
        VBox.setVgrow(mainBox, Priority.ALWAYS);

        HBox header = buildScreenHeader("Connections Manager",
                "Administer graph edges: add or remove friendship connections between verified students.");

        // Card 1: Add Connection
        VBox addCard = new VBox(12);
        addCard.getStyleClass().add("card");
        Label addTitle = new Label("Add Graph Edge (Connection)");
        addTitle.getStyleClass().add("card-title");
        addTitle.setGraphic(SvgIcons.icon(SvgIcons.Glyph.PLUS, 16, "svg-icon-accent"));

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

        // Card 2: Active Connections Table with Search + Pagination
        VBox listCard = new VBox(12);
        listCard.getStyleClass().add("card");
        VBox.setVgrow(listCard, Priority.ALWAYS);

        List<String[]> allPairs = networkService.getAllAcceptedConnections();
        Label listTitle = new Label("Active Network Edges (" + allPairs.size() + " connections)");
        listTitle.getStyleClass().add("card-title");

        // Real-Time Search Bar
        HBox connSearchRow = new HBox(14);
        connSearchRow.setAlignment(Pos.CENTER_LEFT);

        TextField connSearchField = new TextField(connSearch);
        connSearchField.setPromptText("Search connections by name or ID...");
        connSearchField.getStyleClass().add("realtime-search-field");
        HBox.setHgrow(connSearchField, Priority.ALWAYS);

        Label connCountBadge = new Label();
        connCountBadge.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-ink-soft;");
        connSearchRow.getChildren().addAll(connSearchField, connCountBadge);

        TableView<String[]> connTable = new TableView<>();
        connTable.getStyleClass().add("table-view");
        connTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(connTable, Priority.ALWAYS);
        connTable.setMaxHeight(Double.MAX_VALUE);
        connTable.setMinHeight(240);

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
                delBtn.getStyleClass().add("button-ghost-danger");
                delBtn.setStyle("-fx-font-size: 13px; -fx-padding: 6px 12px;");
                delBtn.setOnAction(e -> {
                    String[] pair = getTableView().getItems().get(getIndex());
                    confirmDisconnectEdge(pair[0], pair[1]);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                setGraphic(empty ? null : delBtn);
            }
        });

        // Fixed structural widths: both student columns flex, the action
        // column stays a stable width.
        flexColumn(colA, 190, 250);
        flexColumn(colB, 190, 250);
        pinColumn(actionCol, 142);

        connTable.getColumns().addAll(List.of(colA, colB, actionCol));
        connTable.getColumns().forEach(col -> col.setSortable(false));
        lockColumnOrder(connTable);
        connTable.setPlaceholder(new Label("No network edges yet. Create a connection above."));

        // Pagination Controls Bar
        HBox connPaginationBar = new HBox(12);
        connPaginationBar.getStyleClass().add("pagination-container");
        connPaginationBar.setAlignment(Pos.CENTER);

        Button connPrevBtn = new Button("← Previous");
        connPrevBtn.getStyleClass().add("pagination-btn");

        Label connPageInfo = new Label();
        connPageInfo.getStyleClass().add("pagination-info");

        Button connNextBtn = new Button("Next →");
        connNextBtn.getStyleClass().add("pagination-btn");

        connPaginationBar.getChildren().addAll(connPrevBtn, connPageInfo, connNextBtn);

        // Real-Time Update & Pagination Renderer
        Runnable updateConnTable = () -> {
            List<String[]> currentPairs = networkService.getAllAcceptedConnections();
            String query = (connSearchField.getText() == null) ? "" : connSearchField.getText().trim().toLowerCase();
            List<String[]> filtered = currentPairs.stream()
                    .filter(pair -> {
                        if (query.isEmpty()) return true;
                        com.networkfinder.dsa.StudentNode nA = networkService.getGraph().getNode(pair[0]);
                        com.networkfinder.dsa.StudentNode nB = networkService.getGraph().getNode(pair[1]);
                        String nameA = nA != null ? nA.getFullName() : "";
                        String nameB = nB != null ? nB.getFullName() : "";
                        return pair[0].toLowerCase().contains(query)
                                || pair[1].toLowerCase().contains(query)
                                || nameA.toLowerCase().contains(query)
                                || nameB.toLowerCase().contains(query);
                    })
                    .toList();

            listTitle.setText("Active Network Edges (" + currentPairs.size() + " connections)");
            connCountBadge.setText(filtered.size() + " of " + currentPairs.size() + " connections");

            int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / connPageSize));
            if (connPage > totalPages) connPage = totalPages;
            if (connPage < 1) connPage = 1;

            int from = (connPage - 1) * connPageSize;
            int to = Math.min(from + connPageSize, filtered.size());
            List<String[]> paged = (from <= to && from < filtered.size()) ? filtered.subList(from, to) : List.of();

            connTable.setItems(FXCollections.observableArrayList(paged));
            connPageInfo.setText("Page " + connPage + " of " + totalPages + " (" + filtered.size() + " total)");
            connPrevBtn.setDisable(connPage <= 1);
            connNextBtn.setDisable(connPage >= totalPages || filtered.isEmpty());
        };

        // Real-time search: automatically loads on typing
        connSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            connSearch = (newVal == null) ? "" : newVal;
            connPage = 1;
            updateConnTable.run();
        });

        connPrevBtn.setOnAction(e -> {
            if (connPage > 1) {
                connPage--;
                updateConnTable.run();
            }
        });

        connNextBtn.setOnAction(e -> {
            connPage++;
            updateConnTable.run();
        });

        updateConnTable.run();

        listCard.getChildren().addAll(listTitle, connSearchRow, connTable, connPaginationBar);

        mainBox.getChildren().addAll(header, addCard, listCard);
        contentArea.getChildren().setAll(mainBox);
    }

    private void confirmDisconnectEdge(String studentIdA, String studentIdB) {
        com.networkfinder.dsa.StudentNode nodeA = networkService.getGraph().getNode(studentIdA);
        com.networkfinder.dsa.StudentNode nodeB = networkService.getGraph().getNode(studentIdB);
        String nameA = nodeA != null ? nodeA.getFullName() + " (" + studentIdA + ")" : studentIdA;
        String nameB = nodeB != null ? nodeB.getFullName() + " (" + studentIdB + ")" : studentIdB;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Disconnect Students");
        alert.setHeaderText("Are you sure you want to disconnect these students?");
        alert.setContentText(
                nameA + "  ⇄  " + nameB + "\n\n" +
                "The graph edge between them will be removed and they will no longer appear as "
                + "1st-degree connections. They can be connected again from this screen later.");
        UiDialogs.styleDialogPane(alert.getDialogPane());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (!networkService.removeConnection(studentIdA, studentIdB)) {
                    UiDialogs.showError("Could not remove this connection.");
                    return;
                }
                showConnectionsManagerScreen();
            }
        });
    }

    // Secondary screen: Network Graph overview
    private void showNetworkOverviewScreen() {
        VBox mainBox = new VBox(20);
        mainBox.setPadding(new Insets(24, 32, 28, 32));
        VBox.setVgrow(mainBox, Priority.ALWAYS);

        HBox header = buildScreenHeader("Network Graph Overview",
                "Inspect graph topology, live metrics, adjacency list dump, and visual node constellation.");

        HBox statsRow = new HBox(16);
        statsRow.getChildren().addAll(
                createStatCard("Total Vertices (Students)", String.valueOf(networkService.getGraph().getVertexCount()), "-fx-green"),
                createStatCard("Total Edges (Connections)", String.valueOf(networkService.getGraph().getEdgeCount()), "-fx-orange"),
                createStatCard("Graph Representation", "Adjacency List", "-fx-ink-soft")
        );

        // Side-by-side: Visual Graph (left) + Adjacency Inspector (right)
        HBox splitView = new HBox(16);
        VBox.setVgrow(splitView, Priority.ALWAYS);

        // LEFT: Visual Graph Canvas
        VBox canvasCard = new VBox(10);
        canvasCard.getStyleClass().add("card");
        HBox.setHgrow(canvasCard, Priority.ALWAYS);
        VBox.setVgrow(canvasCard, Priority.ALWAYS);
        Label canvasTitle = new Label("Visual Graph Canvas");
        canvasTitle.getStyleClass().add("card-title");
        GraphVisualizerCanvas canvas = new GraphVisualizerCanvas(networkService.getGraph());
        VBox.setVgrow(canvas, Priority.ALWAYS);
        canvasCard.getChildren().addAll(canvasTitle, canvas);

        // RIGHT: Adjacency List Inspector with pager
        VBox adjCard = new VBox(10);
        adjCard.getStyleClass().add("card");
        HBox.setHgrow(adjCard, Priority.ALWAYS);
        VBox.setVgrow(adjCard, Priority.ALWAYS);
        adjCard.setMinWidth(340);

        Label adjTitle = new Label("Adjacency List Inspector");
        adjTitle.getStyleClass().add("card-title");

        // Search field for nodes
        TextField adjSearchField = new TextField(adjSearch);
        adjSearchField.setPromptText("Search nodes by name or ID...");
        adjSearchField.getStyleClass().add("realtime-search-field");

        // Container for node cards
        VBox nodeContainer = new VBox(8);
        VBox.setVgrow(nodeContainer, Priority.ALWAYS);

        // Pagination bar
        HBox adjPaginationBar = new HBox(10);
        adjPaginationBar.getStyleClass().add("pagination-container");
        adjPaginationBar.setAlignment(Pos.CENTER);

        Button adjPrevBtn = new Button("← Prev");
        adjPrevBtn.getStyleClass().add("pagination-btn");

        Label adjPageInfo = new Label();
        adjPageInfo.getStyleClass().add("pagination-info");

        Button adjNextBtn = new Button("Next →");
        adjNextBtn.getStyleClass().add("pagination-btn");

        adjPaginationBar.getChildren().addAll(adjPrevBtn, adjPageInfo, adjNextBtn);

        // Toggle for raw dump
        Button toggleDumpBtn = new Button("Show Raw Dump");
        toggleDumpBtn.getStyleClass().add("button-secondary");
        toggleDumpBtn.setStyle("-fx-font-size: 12px;");

        TextArea rawDumpArea = new TextArea();
        rawDumpArea.setEditable(false);
        rawDumpArea.setMinHeight(200);
        rawDumpArea.setVisible(false);
        rawDumpArea.setManaged(false);

        toggleDumpBtn.setOnAction(ev -> {
            boolean showing = rawDumpArea.isVisible();
            rawDumpArea.setVisible(!showing);
            rawDumpArea.setManaged(!showing);
            toggleDumpBtn.setText(showing ? "Show Raw Dump" : "Hide Raw Dump");
            if (!showing) {
                StringBuilder sb = new StringBuilder();
                for (String v : networkService.getGraph().getAllVertexIds()) {
                    sb.append("Node [").append(v).append("] (").append(networkService.getGraph().getNode(v).getFullName()).append(")\n");
                    sb.append("   Edges -> ").append(networkService.getGraph().getNeighbors(v)).append("\n\n");
                }
                rawDumpArea.setText(sb.toString());
            }
        });

        // Render paged adjacency nodes
        Runnable updateAdjView = () -> {
            java.util.List<String> allVertexIds = new java.util.ArrayList<>(networkService.getGraph().getAllVertexIds());
            String query = (adjSearchField.getText() == null) ? "" : adjSearchField.getText().trim().toLowerCase();

            List<String> filtered = allVertexIds.stream()
                    .filter(vid -> {
                        if (query.isEmpty()) return true;
                        com.networkfinder.dsa.StudentNode sn = networkService.getGraph().getNode(vid);
                        String name = sn != null ? sn.getFullName() : "";
                        return vid.toLowerCase().contains(query) || name.toLowerCase().contains(query);
                    })
                    .toList();

            int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / adjPageSize));
            if (adjPage > totalPages) adjPage = totalPages;
            if (adjPage < 1) adjPage = 1;

            int from = (adjPage - 1) * adjPageSize;
            int to = Math.min(from + adjPageSize, filtered.size());
            List<String> paged = (from <= to && from < filtered.size()) ? filtered.subList(from, to) : List.of();

            nodeContainer.getChildren().clear();

            for (String vid : paged) {
                com.networkfinder.dsa.StudentNode sn = networkService.getGraph().getNode(vid);
                java.util.Set<String> neighbors = networkService.getGraph().getNeighbors(vid);
                int degree = neighbors != null ? neighbors.size() : 0;

                VBox nodeBox = new VBox(4);
                nodeBox.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 8; -fx-padding: 10 14;");

                HBox nodeHeader = new HBox(8);
                nodeHeader.setAlignment(Pos.CENTER_LEFT);
                Label nameLabel = new Label(sn != null ? sn.getFullName() : vid);
                nameLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: -fx-ink;");
                Label idLabel = new Label(vid);
                idLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-ink-soft;");
                Region hSpacer = new Region();
                HBox.setHgrow(hSpacer, Priority.ALWAYS);
                Label degreeBadge = new Label("Degree: " + degree);
                degreeBadge.setStyle("-fx-background-color: -fx-green-tint; -fx-text-fill: -fx-green; -fx-padding: 2 8; -fx-background-radius: 6; -fx-font-weight: 700; -fx-font-size: 12px;");
                nodeHeader.getChildren().addAll(nameLabel, idLabel, hSpacer, degreeBadge);

                FlowPane neighborChips = new FlowPane(6, 4);
                if (neighbors != null && !neighbors.isEmpty()) {
                    for (String nid : neighbors) {
                        com.networkfinder.dsa.StudentNode nn = networkService.getGraph().getNode(nid);
                        String chipText = nn != null ? nn.getFullName() : nid;
                        Label chip = new Label(chipText);
                        chip.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: -fx-ink-soft; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 12px;");
                        neighborChips.getChildren().add(chip);
                    }
                } else {
                    Label noEdges = new Label("No connections");
                    noEdges.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-ink-soft; -fx-font-style: italic;");
                    neighborChips.getChildren().add(noEdges);
                }

                nodeBox.getChildren().addAll(nodeHeader, neighborChips);
                nodeContainer.getChildren().add(nodeBox);
            }

            adjPageInfo.setText("Page " + adjPage + " of " + totalPages + " (" + filtered.size() + " nodes)");
            adjPrevBtn.setDisable(adjPage <= 1);
            adjNextBtn.setDisable(adjPage >= totalPages || filtered.isEmpty());
        };

        // Real-time search: auto-filter on typing
        adjSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            adjSearch = (newVal == null) ? "" : newVal;
            adjPage = 1;
            updateAdjView.run();
        });

        adjPrevBtn.setOnAction(e -> {
            if (adjPage > 1) {
                adjPage--;
                updateAdjView.run();
            }
        });

        adjNextBtn.setOnAction(e -> {
            adjPage++;
            updateAdjView.run();
        });

        updateAdjView.run();

        ScrollPane adjScroll = new ScrollPane(nodeContainer);
        adjScroll.setFitToWidth(true);
        adjScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(adjScroll, Priority.ALWAYS);

        adjCard.getChildren().addAll(adjTitle, adjSearchField, adjScroll, adjPaginationBar, toggleDumpBtn, rawDumpArea);

        splitView.getChildren().addAll(canvasCard, adjCard);

        mainBox.getChildren().addAll(header, statsRow, splitView);
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
