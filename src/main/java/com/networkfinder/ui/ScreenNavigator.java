package com.networkfinder.ui;

import com.networkfinder.model.StudentProfile;
import com.networkfinder.model.User;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ScreenNavigator {
    private static Stage primaryStage;
    private static User currentUser;

    public static void init(Stage stage) {
        primaryStage = stage;
        stage.setMinWidth(960);
        stage.setMinHeight(640);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void setRoot(Parent root, String title) {
        if (primaryStage.getScene() == null) {
            Scene scene = new Scene(root, 1020, 720);
            scene.getStylesheets().add(ScreenNavigator.class.getResource("/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
        } else {
            primaryStage.getScene().setRoot(root);
        }
        primaryStage.setTitle("Social Network Connection Finder - " + title);
        primaryStage.show();
    }

    public static void showStudentLogin() {
        currentUser = null;
        setRoot(new LoginView(), "Student Login");
    }

    public static void showRegister() {
        setRoot(new RegisterView(), "Student Registration");
    }

    public static void showRegistrationSubmitted(User user) {
        setRoot(new RegistrationSubmittedView(user), "Account Submitted");
    }

    public static void showAdminLogin() {
        currentUser = null;
        setRoot(new AdminLoginView(), "Administrator Login");
    }

    public static void showAdminDashboard() {
        setRoot(new AdminDashboardView(), "Administrator Dashboard");
    }

    public static void showProfileWizard(User user) {
        currentUser = user;
        setRoot(new ProfileWizardView(user), "Profile Setup");
    }

    public static void showProfileSummary(User user, StudentProfile profile) {
        currentUser = user;
        setRoot(new ProfileCompleteView(user, profile), "Profile Complete");
    }

    public static void showStudentDashboard(User user) {
        currentUser = user;
        setRoot(new StudentDashboardView(user), "Network & Study Group Matcher");
    }
}
