@file:Suppress("unused")

package com.gapps.bitmexhelper.kotlin.ui.controller

import com.gapps.bitmexhelper.kotlin.ui.delegates.BulkDelegate
import com.gapps.bitmexhelper.kotlin.ui.delegates.LinkedDelegate
import com.gapps.bitmexhelper.kotlin.ui.delegates.MainDelegate
import com.gapps.bitmexhelper.kotlin.ui.delegates.SettingsDelegate
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.text.Text
import java.math.BigDecimal

class MainController {

    @FXML
    internal lateinit var tabPane: TabPane
    @FXML
    internal lateinit var pair: ComboBox<*>
    @FXML
    internal lateinit var highPirce: Spinner<BigDecimal>
    @FXML
    internal lateinit var lowPrice: Spinner<BigDecimal>
    @FXML
    internal lateinit var amount: Spinner<Int>
    @FXML
    internal lateinit var orderType: ComboBox<*>
    @FXML
    internal lateinit var side: ComboBox<*>
    @FXML
    internal lateinit var distribution: ComboBox<*>
    @FXML
    internal lateinit var parameter: Spinner<BigDecimal>
    @FXML
    internal lateinit var slDistance: Spinner<Int>
    @FXML
    internal lateinit var slDistanceLabel: Text
    @FXML
    internal lateinit var minAmount: Spinner<Int>
    @FXML
    internal lateinit var reversed: CheckBox
    @FXML
    internal lateinit var postOnly: CheckBox
    @FXML
    internal lateinit var reduceOnly: CheckBox
    @FXML
    internal lateinit var review: TableView<*>
    @FXML
    internal lateinit var reviewPriceColumn: TableColumn<BulkDelegate.PreviewItem, BigDecimal>
    @FXML
    internal lateinit var reviewAmountColumn: TableColumn<BulkDelegate.PreviewItem, Int>
    @FXML
    internal lateinit var stats: TextArea
    @FXML
    internal lateinit var moveToLinked: Button
    @FXML
    internal lateinit var execute: Button
    @FXML
    internal lateinit var progress: ProgressIndicator

    @FXML
    internal lateinit var linkedPair: ComboBox<*>
    @FXML
    internal lateinit var linkedOrdersTable: TableView<LinkedDelegate.LinkedTableItem>
    @FXML
    internal lateinit var linkedPositionColumn: TableColumn<LinkedDelegate.LinkedTableItem, Int>
    @FXML
    internal lateinit var linkedSideColumn: TableColumn<LinkedDelegate.LinkedTableItem, String>
    @FXML
    internal lateinit var linkedPriceColumn: TableColumn<LinkedDelegate.LinkedTableItem, BigDecimal>
    @FXML
    internal lateinit var linkedAmountColumn: TableColumn<LinkedDelegate.LinkedTableItem, BigDecimal>
    @FXML
    internal lateinit var linkedOrderTypeColumn: TableColumn<LinkedDelegate.LinkedTableItem, String>
    @FXML
    internal lateinit var linkedOrderTypeParameterColumn: TableColumn<LinkedDelegate.LinkedTableItem, BigDecimal>
    @FXML
    internal lateinit var linkedLinkIdColumn: TableColumn<LinkedDelegate.LinkedTableItem, String>
    @FXML
    internal lateinit var linkedLinkTypeColumn: TableColumn<LinkedDelegate.LinkedTableItem, String>
    @FXML
    internal lateinit var linkedPostOnlyColumn: TableColumn<LinkedDelegate.LinkedTableItem, Boolean>
    @FXML
    internal lateinit var linkedReduceOnlyColumn: TableColumn<LinkedDelegate.LinkedTableItem, Boolean>
    @FXML
    internal lateinit var linkedExecute: Button
    @FXML
    internal lateinit var linkedProgress: ProgressIndicator

    @FXML
    internal lateinit var apiKey: TextField
    @FXML
    internal lateinit var apiSecret: TextField

    @FXML
    private fun onStoreClicked() = SettingsDelegate.onStoreClicked(apiKey.text, apiSecret.text)

    init {
        MainDelegate.onControllerAvailable(this)
    }

    @FXML
    private fun onDuplicateLinkedOrdersClicked() = LinkedDelegate.onDuplicateOrdersClicked()

    @FXML
    private fun onExecuteClicked() = BulkDelegate.onExecuteClicked()

    internal fun exitApp() = Platform.exit()

    internal fun changeInExecutionMode(show: Boolean) {
        progress.isVisible = show
        execute.isDisable = show
        linkedProgress.isVisible = show
        linkedExecute.isDisable = show
    }

    @FXML
    private fun onAddLinkedOrderClicked() = LinkedDelegate.onAddLinkedOrderClicked()

    @FXML
    private fun onRemoveLinkedOrderClicked() = LinkedDelegate.onRemoveLinkedOrderClicked()

    @FXML
    private fun onExecuteLinkedOrdersClicked() = LinkedDelegate.onExecuteLinkedOrdersClicked()

    @FXML
    private fun onMoveToLinkedClicked() = MainDelegate.onMoveToLinkedClicked()

    @FXML
    private fun onClearAllLinkedOrdersClicked() = LinkedDelegate.onClearAllLinkedOrdersClicked()
}
