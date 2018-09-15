@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.gapps.bitmexhelper.kotlin.ui

import com.gapps.bitmexhelper.kotlin.*
import com.gapps.bitmexhelper.kotlin.persistance.Settings
import org.knowm.xchange.bitmex.BitmexExchange


object MainDelegate {

    private lateinit var controller: MainController
    private lateinit var exchange: XChangeWrapper

    fun onControllerAvailable(controller: MainController) {
        MainDelegate.controller = controller
        exchange = XChangeWrapper(BitmexExchange::class, Settings.getBitmexApiKey(), Settings.getBitmexApiSecret())
    }

    fun onSceneSet() {
        BulkDelegate.onSceneSet(controller, exchange)
        LinkedDelegate.onSceneSet(controller, exchange)
    }

    fun onSettingsClicked() = AppDelegate.openSettings()
}