package com.gapps.bitmexhelper.kotlin.ui

import com.gapps.bitmexhelper.kotlin.persistance.Settings

object SettingsDelegate {

    private lateinit var controller: SettingsController
    private val settings = Settings.let { it.load(); it.settings }

    fun onControllerAvailable(settingsController: SettingsController) {
        controller = settingsController
    }

    fun onSceneSet() {
        controller.apiKey.text = Settings.settings.bitmexApiKey
        controller.apiSecret.text = Settings.settings.bitmexSecretKey
    }

    fun onStoreClicked(apiKey: String, apiSecret: String) {
        settings.bitmexApiKey = apiKey
        settings.bitmexSecretKey = apiSecret
        Settings.store()
        AppDelegate.openMain()
    }
}