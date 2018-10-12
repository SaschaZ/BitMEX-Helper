package com.gapps.bitmexhelper.kotlin.ui.delegates

import com.gapps.bitmexhelper.kotlin.persistance.Settings
import com.gapps.bitmexhelper.kotlin.ui.controller.AppController
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
            controller.openMain()
        else {
            controller.openMain()
            MainDelegate.openSettings()
        }
    }

    fun onStopped() {}

    internal fun showError(message: String, type: String? = null) {
        Alert(Alert.AlertType.ERROR).apply {
            dialogPane.apply {
                minWidth = Region.USE_PREF_SIZE
                minHeight = Region.USE_PREF_SIZE
            }
            title = type ?: "ERROR"
            headerText = null
            contentText = message
            showAndWait()
        }
    }

    fun showInfo(message: String) {
        Alert(Alert.AlertType.INFORMATION).apply {
            dialogPane.apply {
                minWidth = Region.USE_PREF_SIZE
                minHeight = Region.USE_PREF_SIZE
            }
            title = "INFORMATION"
            headerText = null
            contentText = message
            showAndWait()
        }
    }
}