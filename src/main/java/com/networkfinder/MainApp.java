package com.networkfinder;

import com.networkfinder.database.DataSeeder;
import com.networkfinder.database.DatabaseManager;
import com.networkfinder.service.NetworkService;
import com.networkfinder.ui.ScreenNavigator;
import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        loadFonts();

        DatabaseManager.getInstance();
        DataSeeder.seedIfEmpty();

        NetworkService.getInstance().reloadGraph();

        ScreenNavigator.init(primaryStage);
        ScreenNavigator.showStudentLogin();
    }

    private void loadFonts() {
        String[] weights = {"Light", "Regular", "Medium", "SemiBold", "Bold", "ExtraBold"};
        for (String weight : weights) {
            String path = "/fonts/Lato-" + weight + ".ttf";
            try (var stream = getClass().getResourceAsStream(path)) {
                if (stream != null) {
                    Font.loadFont(stream, 14);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
