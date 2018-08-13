package com.gapps.bitmexhelper;

import com.gapps.bitmexhelper.kotlin.AppDelegate;
import com.gapps.bitmexhelper.kotlin.MainUiDelegate;
import com.gapps.bitmexhelper.kotlin.SettingsUiDelegate;
import com.gapps.bitmexhelper.kotlin.persistance.Constants;
import com.sun.istack.internal.NotNull;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application implements com.gapps.bitmexhelper.kotlin.App {

    private final AppDelegate appDelegate = AppDelegate.INSTANCE;
    private final SettingsUiDelegate settingsDelegate = SettingsUiDelegate.INSTANCE;
    private final MainUiDelegate mainDelegate = MainUiDelegate.INSTANCE;

    @NotNull
    private Stage stage;

    public App() {
        super();
        appDelegate.onInitialized(this);
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(Constants.title);
        this.stage = primaryStage;
        appDelegate.onStarted();
    }

    @Override
    public void stop() throws Exception {
        appDelegate.onStopped();
        super.stop();
    }

    @Override
    public void openSettings(final boolean closeCurrent) {
        if (closeCurrent) stage.close();
        try {
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("settings.fxml"))));
            settingsDelegate.onSceneSet();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void openMain(final boolean closeCurrent) {
        if (closeCurrent) stage.close();
        try {
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("main.fxml"))));
            mainDelegate.onSceneSet();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
