package com.gapps.bitmexhelper.kotlin

import javafx.fxml.FXML
import javafx.scene.control.TextField

class SettingsController {

    init {
        SettingsUiDelegate.onControllerAvailable(this)
    }

    @FXML
    internal lateinit var apiKey: TextField
    @FXML
    internal lateinit var apiSecret: TextField

    @FXML
    private fun onStoreClicked() {
        SettingsUiDelegate.onStoreClicked(apiKey.text, apiSecret.text)
    }
}