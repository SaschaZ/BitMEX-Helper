package com.gapps.bitmexhelper.kotlin.ui

import com.gapps.bitmexhelper.kotlin.persistance.Settings

object AppDelegate {

    private lateinit var controller: AppController
    private val settings = Settings.let { it.load(); it.settings }

    fun onInitialized(appController: AppController) {
        controller = appController
    }

    fun onStarted() {
        if (settings.bitmexApiKey.isEmpty())
            controller.openSettings(false)
        else
            controller.openMain(false)
    }

    fun onStopped() {}

    internal fun openMain() = controller.openMain(true)

    internal fun openSettings() = controller.openSettings(true)

    internal fun openAbout() = controller.openAbout()
}