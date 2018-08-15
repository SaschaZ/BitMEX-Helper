package com.gapps.bitmexhelper.kotlin.ui

import com.gapps.bitmexhelper.kotlin.persistance.Settings
import javafx.scene.control.Alert

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
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = "ERROR"
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}