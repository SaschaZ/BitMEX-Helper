package com.gapps.bitmexhelper.kotlin

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.*

class MainController {

    private val delegate = MainUiDelegate

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
    internal lateinit var execute: Button
    @FXML
    internal lateinit var review: TableView<*>
    @FXML
    internal lateinit var stats: TextArea

    init {
        delegate.onControllerAvailable(this)
    }

    @FXML
    private fun onAboutClicked() {
        delegate.onAboutClicked()
    }

    @FXML
    private fun onSettingsClicked() {
        delegate.onSettingsClicked()
    }

    @FXML
    private fun onQuitClicked() {
        delegate.onQuitClicked()
    }

    @FXML
    internal fun exitApp() {
        Platform.exit()
    }
}
