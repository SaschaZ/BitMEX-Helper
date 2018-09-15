package com.gapps.bitmexhelper.kotlin.ui

import com.gapps.bitmexhelper.kotlin.persistance.Settings
import javafx.scene.control.Alert
import javafx.scene.layout.Region

object AppDelegate {

    private lateinit var controller: AppController

    fun onInitialized(appController: AppController) {
        controller = appController
        Settings.load()
    }

    fun onStarted() {
        if (Settings.hasCredentials)
            controller.openMain(false)
        else
            controller.openSettings(false)
    }

    fun onStopped() {}

    internal fun openMain() = controller.openMain(true)

    internal fun openSettings() = controller.openSettings(true)

    internal fun showError(message: String) {
        Alert(Alert.AlertType.ERROR).apply {
            dialogPane.apply {
                minWidth = Region.USE_PREF_SIZE
                minHeight = Region.USE_PREF_SIZE
            }
            title = "ERROR"
            headerText = null
            contentText = message
            showAndWait()
        }
    }
}