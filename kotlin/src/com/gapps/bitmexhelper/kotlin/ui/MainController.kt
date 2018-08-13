@file:Suppress("unused")

package com.gapps.bitmexhelper.kotlin.ui

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.*

class MainController {

    private val delegate = MainDelegate

    @FXML
    internal lateinit var pair: ComboBox<*>
    @FXML
    internal lateinit var highPirce: Spinner<*>
    @FXML
    internal lateinit var lowPirce: Spinner<*>
    @FXML
    internal lateinit var amount: Spinner<*>
    @FXML
    internal lateinit var orderType: ComboBox<*>
    @FXML
    internal lateinit var side: ComboBox<*>
    @FXML
    internal lateinit var distribution: ComboBox<*>
    @FXML
    internal lateinit var parameter: Spinner<*>
    @FXML
    internal lateinit var minAmount: Spinner<*>
    @FXML
    internal lateinit var reversed: CheckBox
    @FXML
    internal lateinit var postOnly: CheckBox
    @FXML
    internal lateinit var reduceOnly: CheckBox
    @FXML
    internal lateinit var review: TableView<*>
    @FXML
    internal lateinit var stats: TextArea

    init {
        MainDelegate.onControllerAvailable(this)
    }

    @FXML
    private fun onAboutClicked() {
        MainDelegate.onAboutClicked()
    }

    @FXML
    private fun onSettingsClicked() {
        MainDelegate.onSettingsClicked()
    }

    @FXML
    private fun onQuitClicked() {
        MainDelegate.onQuitClicked()
    }

    @FXML
    private fun onExecuteClicked() {
        MainDelegate.onExecuteClicked()
    }


    internal fun exitApp() {
        Platform.exit()
    }

    internal fun changeInExecutionMode(show: Boolean) {
        // TODO implement
    }
}
