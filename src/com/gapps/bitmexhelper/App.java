package com.gapps.bitmexhelper;

import com.gapps.bitmexhelper.kotlin.AppDelegate;
import com.gapps.bitmexhelper.kotlin.MainUiDelegate;
import com.gapps.bitmexhelper.kotlin.SettingsUiDelegate;
import com.gapps.bitmexhelper.kotlin.persistance.Constants;
import com.gapps.bitmexhelper.kotlin.persistance.Settings;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private final AppDelegate appDelegate = AppDelegate.INSTANCE;
    private final SettingsUiDelegate settingsDelegate = SettingsUiDelegate.INSTANCE;
    private final MainUiDelegate mainDelegate = MainUiDelegate.INSTANCE;

    @Override
    public void start(Stage primaryStage) throws Exception {
        appDelegate.onStarted();
        Settings.Companion.load();
        if (Settings.Companion.getSettings().getBitmexApiKey().isEmpty())
            openSettingsScreen(primaryStage);
        else
            openMainScreen(primaryStage);
    }

    @Override
    public void stop() throws Exception {
        appDelegate.onStopped();
        super.stop();
    }

    private void openSettingsScreen(Stage primaryStage) throws IOException {
        primaryStage.setTitle(Constants.title);
        primaryStage.setScene(new Scene(FXMLLoader.load(getClass().getResource("settings.fxml"))));
        settingsDelegate.onSceneSet();
        primaryStage.show();
    }

    private void openMainScreen(Stage primaryStage) throws IOException {
        primaryStage.setTitle(Constants.title);
        primaryStage.setScene(new Scene(FXMLLoader.load(getClass().getResource("main.fxml"))));
        mainDelegate.onSceneSet();
        primaryStage.show();
    }

    public static void main(String[] args){
        Application.launch(args);
    }
}
