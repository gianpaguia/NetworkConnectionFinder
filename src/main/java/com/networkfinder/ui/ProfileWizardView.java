package com.networkfinder.ui;

import com.networkfinder.model.StudentProfile;
import com.networkfinder.model.User;
import com.networkfinder.service.NetworkService;
import com.networkfinder.service.OperationResult;
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

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setMaxWidth(680);
        root.setPadding(new Insets(10, 20, 40, 20));

        // Top wizard card
        VBox card = new VBox(22);
        card.getStyleClass().add("wizard-card");

        // Progress bar header
        VBox progressHeader = new VBox(8);
        progressHeader.setAlignment(Pos.CENTER);

        progressIndicator.setAlignment(Pos.CENTER);
        progressText.setStyle("-fx-font-weight: 600; -fx-text-fill: -fx-accent; -fx-font-size: 13px;");

        progressHeader.getChildren().addAll(progressIndicator, progressText);

        card.getChildren().addAll(progressHeader, stepContentContainer);
        root.getChildren().add(card);

        StackPane centerWrapper = new StackPane(root);
        centerWrapper.setAlignment(Pos.CENTER);
        centerWrapper.setPadding(new Insets(10, 0, 30, 0));

        scroll.setContent(centerWrapper);

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
            num.setStyle("-fx-font-size: 11px; -fx-font-weight: 700;");

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

        switch (currentStep) {
            case 1 -> renderStep1();
            case 2 -> renderStep2();
            case 3 -> renderStep3();
            case 4 -> renderStep4();
            case 5 -> renderStep5();
        }
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
                "Bachelor of Science in Business Administration",
                "Other"
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
            yBtn.setStyle("-fx-text-fill: " + (isSel ? "#60CDFF;" : "#FFFFFF;") + " -fx-font-weight: 600;");
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
        continueBtn.setDefaultButton(true);
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

        TextField secField = new TextField(enteredSection);
        secField.setPromptText("Example: BSIT 1A");
        secField.textProperty().addListener((obs, old, val) -> enteredSection = val);
        inputSection.getChildren().addAll(secLabel, secField);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: -fx-danger; -fx-font-size: 13px;");
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
        continueBtn.setDefaultButton(true);
        continueBtn.setOnAction(e -> {
            if (enteredSection.trim().isEmpty()) {
                errorLabel.setText("Please enter your section (e.g. BSIT 1A).");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }
            if (enteredSection.trim().length() > 50) {
                errorLabel.setText("Section must be 50 characters or fewer.");
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
            chip.setStyle("-fx-text-fill: " + (selected ? "#60CDFF;" : "#E2E8F0;") + " -fx-font-weight: " + (selected ? "700;" : "500;"));
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
        errorLabel.setStyle("-fx-text-fill: -fx-danger; -fx-font-size: 13px;");
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
        continueBtn.setDefaultButton(true);
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

        String[][] habits = {
                {"📚 Reviewing Lessons", "Reviewing Lessons"},
                {"💻 Coding Together", "Coding Together"},
                {"🧠 Solving Problems", "Solving Problems"},
                {"📝 Exam Review", "Exam Review"},
                {"🎯 Working on Projects", "Working on Projects"},
                {"🗣️ Group Discussions", "Group Discussions"},
                {"📖 Sharing Notes", "Sharing Notes"}
        };

        int col = 0, row = 0;
        for (String[] h : habits) {
            String label = h[0];
            String val = h[1];
            Button hBtn = new Button(label);
            boolean selected = selectedHabits.contains(val);
            hBtn.getStyleClass().add(selected ? "selectable-card-selected" : "selectable-card");
            hBtn.setStyle("-fx-text-fill: " + (selected ? "#60CDFF;" : "#FFFFFF;") + " -fx-font-weight: 600;");
            hBtn.setPrefWidth(280);
            hBtn.setOnAction(e -> {
                if (selectedHabits.contains(val)) selectedHabits.remove(val);
                else selectedHabits.add(val);
                renderStep();
            });
            habitsGrid.add(hBtn, col, row);
            col++;
            if (col > 1) {
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
            Button card = new Button();
            boolean selected = title.equalsIgnoreCase(selectedGroupSize);
            card.getStyleClass().add(selected ? "selectable-card-selected" : "selectable-card");
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setMaxWidth(Double.MAX_VALUE);
            card.setAlignment(Pos.CENTER_LEFT);

            VBox cardContent = new VBox(4);
            Label titleL = new Label(title);
            titleL.setStyle("-fx-font-weight: 700; -fx-text-fill: " + (selected ? "#60CDFF;" : "#FFFFFF;"));
            Label descL = new Label(desc);
            descL.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-ink-soft;");
            cardContent.getChildren().addAll(titleL, descL);
            card.setGraphic(cardContent);
            card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

            card.setOnAction(e -> {
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

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: -fx-danger; -fx-font-size: 13px;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button continueBtn = new Button("Continue");
        continueBtn.getStyleClass().add("button-primary");
        continueBtn.setDefaultButton(true);
        continueBtn.setOnAction(e -> {
            if (selectedHabits.isEmpty()) {
                errorLabel.setText("Please select at least one study habit.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }
            currentStep = 5;
            renderStep();
        });

        navBox.getChildren().addAll(backBtn, spacer, continueBtn);
        box.getChildren().addAll(heading, sub1, habitsGrid, groupSizeBox, errorLabel, navBox);
        stepContentContainer.getChildren().setAll(box);
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
            chip.setStyle("-fx-text-fill: " + (selected ? "#60CDFF;" : "#E2E8F0;") + " -fx-font-weight: " + (selected ? "700;" : "500;"));
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
        otherField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().length() <= 100 ? change : null));
        otherField.textProperty().addListener((obs, old, val) -> enteredOtherInterest = val);
        otherBox.getChildren().addAll(otherLabel, otherField);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: -fx-danger; -fx-font-size: 13px;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

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
        finishBtn.setDefaultButton(true);
        finishBtn.setOnAction(e -> {
            String savedSection = enteredSection == null ? "" : enteredSection.trim();
            String savedOther = enteredOtherInterest == null ? "" : enteredOtherInterest.trim();

            if (!savedOther.isEmpty() && selectedInterests.contains(savedOther)) {
                errorLabel.setText("That interest is already selected above.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            // Save profile
            StudentProfile profile = new StudentProfile(
                    user.getStudentId(),
                    selectedCourse,
                    selectedYear,
                    savedSection,
                    selectedSubjects,
                    selectedHabits,
                    selectedGroupSize,
                    selectedInterests,
                    savedOther
            );
            // Save profile through the service layer
            OperationResult saveResult = NetworkService.getInstance().saveStudentProfile(user.getStudentId(), profile);
            if (!saveResult.isOk()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Profile Not Saved");
                alert.setHeaderText(null);
                alert.setContentText(saveResult.getMessage());
                alert.showAndWait();
                return;
            }
            user.setProfileCompleted(true);

            // Navigate to Screen 12
            ScreenNavigator.showProfileSummary(user, profile);
        });

        navBox.getChildren().addAll(backBtn, spacer, finishBtn);
        box.getChildren().addAll(heading, subtitle, chipsPane, otherBox, navBox);
        stepContentContainer.getChildren().setAll(box);
    }
}
