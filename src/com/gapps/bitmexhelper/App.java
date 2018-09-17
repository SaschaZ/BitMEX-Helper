package com.gapps.bitmexhelper;

import com.gapps.bitmexhelper.kotlin.persistance.Constants;
import com.gapps.bitmexhelper.kotlin.ui.controller.AppController;
import com.gapps.bitmexhelper.kotlin.ui.delegates.AppDelegate;
import com.gapps.bitmexhelper.kotlin.ui.delegates.MainDelegate;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application implements AppController {

    private final AppDelegate appDelegate = AppDelegate.INSTANCE;
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
        appDelegate.onStarted();
    }

    @Override
    public void stop() throws Exception {
        appDelegate.onStopped();
        super.stop();
    }

    @Override
    public void openMain(final boolean closeCurrent) {
        if (closeCurrent) stage.close();
        try {
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("main.fxml"))));
            mainDelegate.onSceneSet();
            stage.setMinHeight(500.0);
            stage.setMinWidth(945.0);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
