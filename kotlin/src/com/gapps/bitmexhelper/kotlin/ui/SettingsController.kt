@file:Suppress("unused")

package com.gapps.bitmexhelper.kotlin.ui

import javafx.fxml.FXML
import javafx.scene.control.TextField

class SettingsController {

    init {
        SettingsDelegate.onControllerAvailable(this)
    }

    @FXML
    internal lateinit var apiKey: TextField
    @FXML
    internal lateinit var apiSecret: TextField

    @FXML
    private fun onStoreClicked() = SettingsDelegate.onStoreClicked(apiKey.text, apiSecret.text)
}