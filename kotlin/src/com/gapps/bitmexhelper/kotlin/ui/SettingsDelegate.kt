package com.gapps.bitmexhelper.kotlin.ui

import com.gapps.bitmexhelper.kotlin.persistance.Settings

object SettingsDelegate {

    private lateinit var controller: MainController

    fun onSceneSet(mainController: MainController) {
        controller = mainController
        controller.apiKey.text = Settings.getBitmexApiKey()
        controller.apiSecret.text = Settings.getBitmexApiSecret()
    }

    fun onStoreClicked(apiKey: String, apiSecret: String) {
        if (apiKey.isEmpty() || apiSecret.isEmpty())
            AppDelegate.showError("You have to enter an API-Key and API-Secret.")
        else {
            Settings.setBitmexApiKey(apiKey)
            Settings.setBitmexApiSecret(apiSecret)
            Settings.store()
            AppDelegate.showInfo("Settings were stored.")
        }
    }
}