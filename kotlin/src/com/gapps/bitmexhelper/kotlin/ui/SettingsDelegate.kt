package com.gapps.bitmexhelper.kotlin.ui

import com.gapps.bitmexhelper.kotlin.persistance.Settings
import com.gapps.bitmexhelper.kotlin.persistance.Settings.Companion.settings

object SettingsDelegate {

    private lateinit var controller: SettingsController

    fun onControllerAvailable(settingsController: SettingsController) {
        controller = settingsController
        Settings.load()
    }

    fun onSceneSet() {
        controller.apiKey.text = settings.bitmexApiKey
        controller.apiSecret.text = settings.bitmexSecretKey
    }

    fun onStoreClicked(apiKey: String, apiSecret: String) {
        if (apiKey.isEmpty() || apiSecret.isEmpty())
            AppDelegate.showError("You have to enter an API-Key and API-Secret.")
        else {
            settings.bitmexApiKey = apiKey
            settings.bitmexSecretKey = apiSecret
            Settings.store()
            AppDelegate.openMain()
        }
    }
}