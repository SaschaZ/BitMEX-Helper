package com.gapps.bitmexhelper.kotlin

import com.gapps.bitmexhelper.kotlin.persistance.Settings

object AppDelegate {

    private lateinit var app: App
    private val settings = Settings.let { it.load(); it.settings }

    fun onInitialized(app: App) {
        this.app = app
    }

    fun onStarted() {
        if (settings.bitmexApiKey.isEmpty())
            app.openSettings(false)
        else
            app.openMain(false)
    }

    fun onStopped() {}

    fun openMain() = app.openMain(true)

    fun openSettings() = app.openSettings(true)
}