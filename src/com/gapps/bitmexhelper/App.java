package com.gapps.bitmexhelper;

import com.gapps.bitmexhelper.kotlin.persistance.Constants;
import com.gapps.bitmexhelper.kotlin.ui.AppController;
import com.gapps.bitmexhelper.kotlin.ui.AppDelegate;
import com.gapps.bitmexhelper.kotlin.ui.MainDelegate;
import com.gapps.bitmexhelper.kotlin.ui.SettingsDelegate;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application implements AppController {

    private final AppDelegate appDelegate = AppDelegate.INSTANCE;
    private final SettingsDelegate settingsDelegate = SettingsDelegate.INSTANCE;
    private final MainDelegate mainDelegate = MainDelegate.INSTANCE;

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
        this.stage = primaryStage;
        primaryStage.setTitle(Constants.title);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("icBitmex.jpg")));
        primaryStage.setResizable(false);
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
