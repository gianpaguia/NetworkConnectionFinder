package com.networkfinder.ui;

import com.networkfinder.database.ProfileDao;
import com.networkfinder.database.UserDao;
import com.networkfinder.model.StudentProfile;
import com.networkfinder.model.User;
import com.networkfinder.service.NetworkService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.util.ArrayList;
import java.util.List;

public class ProfileWizardView extends StackPane {
    private final User user;
    private final ProfileDao profileDao = new ProfileDao();
    private final UserDao userDao = new UserDao();

    private int currentStep = 1;

    // Form state variables
    private String selectedCourse = "Bachelor of Science in Information Technology";
    private String selectedYear = "1st Year";
    private String enteredSection = "";
    private final List<String> selectedSubjects = new ArrayList<>();
    private final List<String> selectedHabits = new ArrayList<>();
    private String selectedGroupSize = "Small Group";
    private final List<String> selectedInterests = new ArrayList<>();
    private String enteredOtherInterest = "";

    private final VBox stepContentContainer = new VBox(20);
    private final Label progressText = new Label("Step 1 of 5");
    private final HBox progressIndicator = new HBox(4);
    private VBox sidePanelContainer;

    @SuppressWarnings("this-escape")

    public ProfileWizardView(User user) {
        this.user = user;
        setAlignment(Pos.CENTER);
        getStyleClass().add("fluent-root-pane");

        // Top Return Bar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(14, 28, 6, 28));

        Button returnBtn = new Button(user.isProfileCompleted() ? "←  Return to Dashboard" : "←  Back to Login");
        returnBtn.getStyleClass().add("window-back-btn");
        returnBtn.setOnAction(e -> {
            if (user.isProfileCompleted()) {
                ScreenNavigator.showStudentDashboard(user);
            } else {
                ScreenNavigator.showStudentLogin();
            }
        });
        topBar.getChildren().add(returnBtn);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox card = new VBox(22);
        card.getStyleClass().add("wizard-card");
        card.setMaxWidth(Double.MAX_VALUE);

        VBox progressHeader = new VBox(8);
        progressHeader.setAlignment(Pos.CENTER);

        progressIndicator.setAlignment(Pos.CENTER);
        progressText.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-accent; -fx-font-size: 13px;");

        progressHeader.getChildren().addAll(progressIndicator, progressText);
        card.getChildren().addAll(progressHeader, stepContentContainer);

        HBox columns = new HBox(20);
        columns.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        sidePanelContainer = buildSidePanel();
        columns.getChildren().addAll(card, sidePanelContainer);

        VBox root = new VBox(16);
        root.setMaxWidth(Double.MAX_VALUE);
        root.setPadding(new Insets(6, 36, 40, 36));
        root.getChildren().addAll(buildWizardHero(), columns);

        scroll.setContent(root);

        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(topBar);
        mainLayout.setCenter(scroll);

        getChildren().add(mainLayout);

        renderStep();
    }

    private void updateProgressDots() {
        progressIndicator.getChildren().clear();
        for (int i = 1; i <= 5; i++) {
            StackPane node = new StackPane();
            node.setPrefSize(34, 34);

            Circle circle = new Circle(13);
            Label num = new Label(String.valueOf(i));
            num.setStyle("-fx-font-size: 13px; -fx-font-weight: 800;");

            if (i < currentStep) {
                circle.setFill(Color.web("#107C41"));
                circle.setStroke(Color.web("#22C55E"));
                num.setTextFill(Color.WHITE);
            } else if (i == currentStep) {
                circle.setFill(Color.web("#60CDFF"));
                circle.setStroke(Color.web("#78D5FF"));
                circle.setStrokeWidth(2.0);
                num.setTextFill(Color.web("#0F172A"));
            } else {
                circle.setFill(Color.web("#262632"));
                circle.setStroke(Color.color(1.0, 1.0, 1.0, 0.20));
                circle.setStrokeWidth(1.2);
                num.setTextFill(Color.web("#A2A2AE"));
            }

            node.getChildren().addAll(circle, num);
            progressIndicator.getChildren().add(node);

            if (i < 5) {
                Line edge = new Line(0, 0, 26, 0);
                edge.setStrokeWidth(2);
                edge.setStroke(i < currentStep ? Color.web("#107C41") : Color.color(1.0, 1.0, 1.0, 0.15));
                progressIndicator.getChildren().add(edge);
            }
        }
        progressText.setText("Step " + currentStep + " of 5");
    }

    private void renderStep() {
        updateProgressDots();
        stepContentContainer.getChildren().clear();
        refreshSidePanel();

        switch (currentStep) {
            case 1 -> renderStep1();
            case 2 -> renderStep2();
            case 3 -> renderStep3();
            case 4 -> renderStep4();
            case 5 -> renderStep5();
        }
    }

    private VBox buildWizardHero() {
        HBox heroRow = new HBox(18);
        heroRow.setAlignment(Pos.CENTER_LEFT);
        heroRow.setPadding(new Insets(6, 4, 2, 4));

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("fluent-tile-icon-box");
        iconBox.getChildren().add(SvgIcons.icon(SvgIcons.Glyph.GRADUATION_CAP, 26, "svg-icon-accent"));

        VBox textCol = new VBox(4);
        Label title = new Label("Profile Setup");
        title.getStyleClass().add("fluent-user-name");
        Label subtitle = new Label("5 quick questions so the Study Group Matcher can find your best study partners on campus.");
        subtitle.getStyleClass().add("fluent-user-subtitle");
        textCol.getChildren().addAll(title, subtitle);

        heroRow.getChildren().addAll(iconBox, textCol);

        VBox hero = new VBox();
        hero.getChildren().add(heroRow);
        return hero;
    }

    private VBox buildSidePanel() {
        VBox side = new VBox(16);
        side.setMinWidth(340);
        side.setMaxWidth(340);
        fillSidePanel(side);
        return side;
    }

    private void refreshSidePanel() {
        if (sidePanelContainer != null) {
            fillSidePanel(sidePanelContainer);
        }
    }

    private void fillSidePanel(VBox side) {
        VBox whyCard = new VBox(12);
        whyCard.getStyleClass().add("card");
        whyCard.setPadding(new Insets(18, 20, 16, 20));

        Label whyTitle = new Label("Why we ask");
        whyTitle.getStyleClass().add("card-title");

        VBox tipsBox = new VBox(10);
        for (String tip : stepTips()) {
            HBox tipRow = new HBox(10);
            tipRow.setAlignment(Pos.TOP_LEFT);
            Node icon = SvgIcons.icon(SvgIcons.Glyph.LIGHTBULB, 14, "svg-icon-accent");
            Label text = new Label(tip);
            text.setStyle("-fx-text-fill: -fx-ink-soft; -fx-font-size: 12px;");
            text.setWrapText(true);
            text.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(text, Priority.ALWAYS);
            tipRow.getChildren().addAll(icon, text);
            tipsBox.getChildren().add(tipRow);
        }

        whyCard.getChildren().addAll(whyTitle, tipsBox);

        VBox summaryCard = new VBox(10);
        summaryCard.getStyleClass().add("card");
        summaryCard.setPadding(new Insets(18, 20, 16, 20));

        Label sumTitle = new Label("Your answers");
        sumTitle.getStyleClass().add("card-title");

        GridPane sumGrid = new GridPane(14, 7);
        String course = courseAbbreviation(selectedCourse);
        String[][] rows = {
                {"Course", course != null ? course
                        : (selectedCourse == null || selectedCourse.isBlank() ? "-" : "Custom")},
                {"Year", selectedYear == null || selectedYear.isBlank() ? "-" : selectedYear},
                {"Section", enteredSection == null || enteredSection.isBlank() ? "-" : enteredSection},
                {"Subjects", selectedSubjects.size() + " selected"},
                {"Study habits", selectedHabits.size() + " selected"},
                {"Group size", selectedGroupSize == null ? "-" : selectedGroupSize},
                {"Interests", selectedInterests.size() + " selected"}
        };
        for (int i = 0; i < rows.length; i++) {
            Label key = new Label(rows[i][0]);
            key.getStyleClass().add("helper-text");
            Label val = new Label(rows[i][1]);
            val.setStyle("-fx-text-fill: -fx-ink; -fx-font-weight: 600; -fx-font-size: 12px;");
            sumGrid.add(key, 0, i);
            sumGrid.add(val, 1, i);
        }

        summaryCard.getChildren().addAll(sumTitle, sumGrid);
        side.getChildren().setAll(whyCard, summaryCard);
    }

    private String[] stepTips() {
        return switch (currentStep) {
            case 1 -> new String[]{
                    "Your course groups you with classmates who take the same core subjects — the strongest match signal.",
                    "Your year level decides which sections you can pick in the next step."};
            case 2 -> new String[]{
                    "We prioritize classmates from your exact section (e.g. BSIT 2A) when recommending partners.",
                    "Pick the section printed on your class schedule."};
            case 3 -> new String[]{
                    "Shared subjects become the topics of suggested study groups.",
                    "Choose at least 2 subjects to unlock matching."};
            case 4 -> new String[]{
                    "Overlapping study habits raise a partner's compatibility score.",
                    "Group size matches you with students who prefer the same setup."};
            default -> new String[]{
                    "Interests power ice-breaker suggestions and org recommendations.",
                    "The extra text field is optional — add any hobby not listed."};
        };
    }

    // ==================================================
    // SCREEN 7 — PROFILE SETUP STEP 1 (Course & Year)
    // ==================================================
    private void renderStep1() {
        VBox box = new VBox(20);

        Label heading = new Label("Let's set up your profile");
        heading.getStyleClass().add("screen-heading");

        // Question 1: Course
        VBox courseBox = new VBox(8);
        Label q1 = new Label("What course are you taking?");
        q1.setStyle("-fx-font-weight: 700; -fx-text-fill: -fx-ink;");

        ComboBox<String> courseCombo = new ComboBox<>();
        courseCombo.getItems().addAll(
                "Bachelor of Science in Information Technology",
                "Bachelor of Science in Computer Science",
                "Bachelor of Science in Information Systems",
                "Bachelor of Science in Business Administration"
        );
        courseCombo.setValue(selectedCourse);
        courseCombo.setMaxWidth(Double.MAX_VALUE);
        courseCombo.setOnAction(e -> selectedCourse = courseCombo.getValue());
        courseBox.getChildren().addAll(q1, courseCombo);

        // Question 2: Year
        VBox yearBox = new VBox(8);
        Label q2 = new Label("What year are you in?");
        q2.setStyle("-fx-font-weight: 700; -fx-text-fill: -fx-ink;");

        HBox yearCards = new HBox(12);
        String[] years = {"1st Year", "2nd Year", "3rd Year", "4th Year"};
        for (String yr : years) {
            Button yBtn = new Button(yr);
            boolean isSel = yr.equals(selectedYear);
            yBtn.getStyleClass().add(isSel ? "selectable-card-selected" : "selectable-card");
            HBox.setHgrow(yBtn, Priority.ALWAYS);
            yBtn.setMaxWidth(Double.MAX_VALUE);
            yBtn.setOnAction(e -> {
                selectedYear = yr;
                renderStep();
            });
            yearCards.getChildren().add(yBtn);
        }
        yearBox.getChildren().addAll(q2, yearCards);

        // Continue Button
        HBox navBox = new HBox();
        navBox.setAlignment(Pos.CENTER_RIGHT);
        Button continueBtn = new Button("Continue");
        continueBtn.getStyleClass().add("button-primary");
        continueBtn.setOnAction(e -> {
            currentStep = 2;
            renderStep();
        });
        navBox.getChildren().add(continueBtn);

        box.getChildren().addAll(heading, courseBox, yearBox, navBox);
        stepContentContainer.getChildren().setAll(box);
    }

    // ==================================================
    // SCREEN 8 — PROFILE SETUP STEP 2 (Section)
    // ==================================================
    private void renderStep2() {
        VBox box = new VBox(18);

        Label heading = new Label("Which section are you in?");
        heading.getStyleClass().add("screen-heading");

        Label subtitle = new Label("Your section helps us identify classmates who may share your classes.");
        subtitle.getStyleClass().add("screen-subtitle");

        VBox inputSection = new VBox(8);
        Label secLabel = new Label("Section");
        secLabel.getStyleClass().add("field-label");

        List<String> sectionOptions = buildSectionOptions();
        ComboBox<String> secCombo = new ComboBox<>();
        secCombo.getItems().addAll(sectionOptions);
        secCombo.setPromptText("Select your section");
        secCombo.setMaxWidth(Double.MAX_VALUE);
        if (enteredSection != null && !enteredSection.isBlank() && sectionOptions.contains(enteredSection)) {
            secCombo.setValue(enteredSection);
        } else {
            enteredSection = "";
        }
        secCombo.setOnAction(e -> enteredSection = secCombo.getValue() == null ? "" : secCombo.getValue());
        inputSection.getChildren().addAll(secLabel, secCombo);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("form-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Buttons: Back and Continue
        HBox navBox = new HBox(12);
        Button backBtn = new Button("Back");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setOnAction(e -> {
            currentStep = 1;
            renderStep();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button continueBtn = new Button("Continue");
        continueBtn.getStyleClass().add("button-primary");
        continueBtn.setOnAction(e -> {
            if (enteredSection.trim().isEmpty()) {
                errorLabel.setText("Please enter your section (e.g. BSIT 1A).");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }
            currentStep = 3;
            renderStep();
        });

        navBox.getChildren().addAll(backBtn, spacer, continueBtn);
        box.getChildren().addAll(heading, subtitle, inputSection, errorLabel, navBox);
        stepContentContainer.getChildren().setAll(box);
    }

    private List<String> buildSectionOptions() {
        String abbr = courseAbbreviation(selectedCourse);
        String[] abbrs = abbr != null ? new String[]{abbr} : new String[]{"BSIT", "BSCS", "BSIS", "BSBA"};
        int year = parseYearDigit(selectedYear);
        List<String> options = new ArrayList<>();
        for (String a : abbrs) {
            options.add(a + " " + year + "A");
            options.add(a + " " + year + "B");
            options.add(a + " " + year + "C");
            options.add(a + " " + year + "D");
        }
        return options;
    }

    private int parseYearDigit(String year) {
        if (year != null && !year.isBlank() && Character.isDigit(year.charAt(0))) {
            return Character.getNumericValue(year.charAt(0));
        }
        return 1;
    }

    private String courseAbbreviation(String course) {
        if (course == null) return null;
        if (course.contains("Information Technology")) return "BSIT";
        if (course.contains("Computer Science")) return "BSCS";
        if (course.contains("Information Systems")) return "BSIS";
        if (course.contains("Business Administration")) return "BSBA";
        return null;
    }

    // ==================================================
    // SCREEN 9 — PROFILE SETUP STEP 3 (Favorite Subjects)
    // ==================================================
    private void renderStep3() {
        VBox box = new VBox(18);

        Label heading = new Label("What subjects do you enjoy?");
        heading.getStyleClass().add("screen-heading");

        Label subtitle = new Label("Select subjects you're interested in so we can help you discover students with similar academic interests.");
        subtitle.getStyleClass().add("screen-subtitle");

        Label notice = new Label("Select at least 2 subjects.");
        notice.setStyle("-fx-font-weight: 700; -fx-text-fill: -fx-green; -fx-font-size: 13px;");

        FlowPane chipsPane = new FlowPane();
        chipsPane.setHgap(10);
        chipsPane.setVgap(10);

        String[] subjectsList = {
                "Programming", "Data Structures", "Database Management", "Networking",
                "Web Development", "Mobile Development", "Mathematics", "Cybersecurity",
                "Software Engineering", "Research", "Communication", "Entrepreneurship"
        };

        for (String sub : subjectsList) {
            Button chip = new Button(sub);
            boolean selected = selectedSubjects.contains(sub);
            chip.getStyleClass().add(selected ? "chip-selected" : "chip");
            chip.setOnAction(e -> {
                if (selectedSubjects.contains(sub)) {
                    selectedSubjects.remove(sub);
                } else {
                    selectedSubjects.add(sub);
                }
                renderStep();
            });
            chipsPane.getChildren().add(chip);
        }

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("form-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Buttons
        HBox navBox = new HBox(12);
        Button backBtn = new Button("Back");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setOnAction(e -> {
            currentStep = 2;
            renderStep();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button continueBtn = new Button("Continue");
        continueBtn.getStyleClass().add("button-primary");
        continueBtn.setOnAction(e -> {
            if (selectedSubjects.size() < 2) {
                errorLabel.setText("Please select at least 2 subjects.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }
            currentStep = 4;
            renderStep();
        });

        navBox.getChildren().addAll(backBtn, spacer, continueBtn);
        box.getChildren().addAll(heading, subtitle, notice, chipsPane, errorLabel, navBox);
        stepContentContainer.getChildren().setAll(box);
    }

    // ==================================================
    // SCREEN 10 — PROFILE SETUP STEP 4 (Study Habits & Group Size)
    // ==================================================
    private void renderStep4() {
        VBox box = new VBox(20);

        Label heading = new Label("How do you like to study?");
        heading.getStyleClass().add("screen-heading");

        Label sub1 = new Label("Select your preferred study habits:");
        sub1.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-ink-soft;");

        // Study habit cards grid
        GridPane habitsGrid = new GridPane();
        habitsGrid.setHgap(12);
        habitsGrid.setVgap(10);
        habitsGrid.setMaxWidth(Double.MAX_VALUE);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 3);
            col.setHgrow(Priority.ALWAYS);
            col.setFillWidth(true);
            habitsGrid.getColumnConstraints().add(col);
        }

        String[] habits = {
                "Reviewing Lessons",
                "Coding Together",
                "Solving Problems",
                "Exam Review",
                "Working on Projects",
                "Group Discussions",
                "Sharing Notes"
        };

        int col = 0, row = 0;
        for (String val : habits) {
            Button hBtn = new Button(val);
            hBtn.setGraphic(SvgIcons.icon(habitIcon(val), 16, "svg-icon-accent"));
            boolean selected = selectedHabits.contains(val);
            hBtn.getStyleClass().add(selected ? "selectable-card-selected" : "selectable-card");
            hBtn.setMaxWidth(Double.MAX_VALUE);
            hBtn.setOnAction(e -> {
                if (selectedHabits.contains(val)) selectedHabits.remove(val);
                else selectedHabits.add(val);
                renderStep();
            });
            habitsGrid.add(hBtn, col, row);
            col++;
            if (col > 2) {
                col = 0;
                row++;
            }
        }

        // Group size preference
        VBox groupSizeBox = new VBox(8);
        Label qGroup = new Label("What type of study group do you prefer?");
        qGroup.setStyle("-fx-font-weight: 700; -fx-text-fill: -fx-ink;");

        HBox sizeCards = new HBox(12);
        String[][] sizes = {
                {"Small Group", "2–4 students"},
                {"Medium Group", "5–8 students"},
                {"Large Group", "9+ students"}
        };

        for (String[] s : sizes) {
            String title = s[0];
            String desc = s[1];
            VBox card = new VBox(4);
            boolean selected = title.equalsIgnoreCase(selectedGroupSize);
            card.getStyleClass().add(selected ? "selectable-card-selected" : "selectable-card");
            HBox.setHgrow(card, Priority.ALWAYS);

            Label titleL = new Label(title);
            titleL.getStyleClass().add(selected ? "selectable-card-title-selected" : "selectable-card-title");
            Label descL = new Label(desc);
            descL.getStyleClass().add("selectable-card-desc");
            card.getChildren().addAll(titleL, descL);

            card.setOnMouseClicked(e -> {
                selectedGroupSize = title;
                renderStep();
            });
            sizeCards.getChildren().add(card);
        }
        groupSizeBox.getChildren().addAll(qGroup, sizeCards);

        // Buttons
        HBox navBox = new HBox(12);
        Button backBtn = new Button("Back");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setOnAction(e -> {
            currentStep = 3;
            renderStep();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button continueBtn = new Button("Continue");
        continueBtn.getStyleClass().add("button-primary");
        continueBtn.setOnAction(e -> {
            if (selectedHabits.isEmpty()) {
                selectedHabits.add("Reviewing Lessons");
            }
            currentStep = 5;
            renderStep();
        });

        navBox.getChildren().addAll(backBtn, spacer, continueBtn);
        box.getChildren().addAll(heading, sub1, habitsGrid, groupSizeBox, navBox);
        stepContentContainer.getChildren().setAll(box);
    }

    private SvgIcons.Glyph habitIcon(String habit) {
        switch (habit) {
            case "Reviewing Lessons": return SvgIcons.Glyph.BOOK_OPEN;
            case "Coding Together": return SvgIcons.Glyph.CODE;
            case "Solving Problems": return SvgIcons.Glyph.BRAIN;
            case "Exam Review": return SvgIcons.Glyph.FILE_TEXT;
            case "Working on Projects": return SvgIcons.Glyph.FOLDER_KANBAN;
            case "Group Discussions": return SvgIcons.Glyph.MESSAGE_CIRCLE;
            case "Sharing Notes": return SvgIcons.Glyph.NOTEBOOK;
            default: return SvgIcons.Glyph.INFO;
        }
    }

    // ==================================================
    // SCREEN 11 — PROFILE SETUP STEP 5 (Interests)
    // ==================================================
    private void renderStep5() {
        VBox box = new VBox(18);

        Label heading = new Label("What are you interested in?");
        heading.getStyleClass().add("screen-heading");

        Label subtitle = new Label("Select your extracurricular and personal hobbies:");
        subtitle.getStyleClass().add("screen-subtitle");

        FlowPane chipsPane = new FlowPane();
        chipsPane.setHgap(10);
        chipsPane.setVgap(10);

        String[] interestsList = {
                "Technology", "Gaming", "Sports", "Music", "Arts", "Photography",
                "Volunteering", "Business", "Fitness", "Research", "Academic Organizations", "Student Activities"
        };

        for (String item : interestsList) {
            Button chip = new Button(item);
            boolean selected = selectedInterests.contains(item);
            chip.getStyleClass().add(selected ? "chip-selected" : "chip");
            chip.setOnAction(e -> {
                if (selectedInterests.contains(item)) selectedInterests.remove(item);
                else selectedInterests.add(item);
                renderStep();
            });
            chipsPane.getChildren().add(chip);
        }

        // Optional other interest
        VBox otherBox = new VBox(6);
        Label otherLabel = new Label("Other interests (optional)");
        otherLabel.getStyleClass().add("field-label");

        TextField otherField = new TextField(enteredOtherInterest);
        otherField.setPromptText("Enter an interest");
        otherField.textProperty().addListener((obs, old, val) -> enteredOtherInterest = val);
        otherBox.getChildren().addAll(otherLabel, otherField);

        // Buttons
        HBox navBox = new HBox(12);
        Button backBtn = new Button("Back");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setOnAction(e -> {
            currentStep = 4;
            renderStep();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button finishBtn = new Button("Finish Profile");
        finishBtn.getStyleClass().add("button-accent");
        finishBtn.setOnAction(e -> {
            // Save profile
            StudentProfile profile = new StudentProfile(
                    user.getStudentId(),
                    selectedCourse,
                    selectedYear,
                    enteredSection,
                    selectedSubjects,
                    selectedHabits,
                    selectedGroupSize,
                    selectedInterests,
                    enteredOtherInterest
            );
            profileDao.saveOrUpdateProfile(profile);
            userDao.updateProfileCompleted(user.getStudentId(), true);
            user.setProfileCompleted(true);

            // Synchronize Graph Node
            NetworkService.getInstance().reloadGraph();

            // Navigate to Screen 12
            ScreenNavigator.showProfileSummary(user, profile);
        });

        navBox.getChildren().addAll(backBtn, spacer, finishBtn);
        box.getChildren().addAll(heading, subtitle, chipsPane, otherBox, navBox);
        stepContentContainer.getChildren().setAll(box);
    }
}
