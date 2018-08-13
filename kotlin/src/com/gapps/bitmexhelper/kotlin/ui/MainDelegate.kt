@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.gapps.bitmexhelper.kotlin.ui

import com.gapps.bitmexhelper.kotlin.persistance.Constants
import com.gapps.bitmexhelper.kotlin.persistance.Settings
import com.gapps.xchangewrapper.XChangeWrapper
import com.gapps.xchangewrapper.toCurrencyPair
import javafx.collections.FXCollections
import kotlinx.coroutines.experimental.launch
import org.knowm.xchange.bitmex.BitmexExchange
import org.knowm.xchange.dto.Order


object MainDelegate {

    private lateinit var controller: MainController
    private val settings = Settings.let { it.load(); it.settings }

    fun onControllerAvailable(controller: MainController) {
        MainDelegate.controller = controller
    }

    private var exchange: XChangeWrapper? = null

    fun onSceneSet() {
        exchange = XChangeWrapper(BitmexExchange::class, settings.bitmexApiKey, settings.bitmexSecretKey)

        controller.apply {
            pair.apply {
                items = FXCollections.observableArrayList(Constants.pairs)
                value = items[Constants.pairs.indexOf(settings.lastPair)]
                setOnAction { updateView() }
            }
            highPirce.valueFactory.value = settings.lastHighPrice
            highPirce.valueProperty().addListener { _, _, _ -> updateView() }
            lowPirce.valueFactory.value = settings.lastLowPrice
            lowPirce.valueProperty().addListener { _, _, _ -> updateView() }
            amount.valueFactory.value = settings.lastAmount.toDouble()
            amount.valueProperty().addListener { _, _, _ -> updateView() }
            orderType.apply {
                items = FXCollections.observableArrayList(Constants.orderTypes)
                value = items[Constants.orderTypes.indexOf(settings.lastOrderType)]
                setOnAction { updateView() }
            }
            side.apply {
                items = FXCollections.observableArrayList(Constants.sides)
                value = items[Constants.sides.indexOf(settings.lastSide)]
                setOnAction { updateView() }
            }
            distribution.apply {
                items = FXCollections.observableArrayList(Constants.distributions)
                value = items[Constants.distributions.indexOf(settings.lastDistributionType)]
                setOnAction { updateView() }
            }
            parameter.valueFactory.value = settings.lastDistributionParameter
            parameter.valueProperty().addListener { _, _, _ -> updateView() }
            minAmount.valueFactory.value = settings.lastMinAmount.toDouble()
            minAmount.valueProperty().addListener { _, _, _ -> updateView() }
            reversed.isSelected = settings.lastReversed
            reversed.setOnAction { updateView() }
            postOnly.isSelected = settings.lastPostOnly
            postOnly.setOnAction { updateView() }
            reduceOnly.isSelected = settings.lastReduceOnly
            reduceOnly.setOnAction { updateView() }
        }
    }

    private fun updateView() {
        // TODO implement
    }

    internal fun onExecuteClicked() = launch {
        controller.changeInExecutionMode(true)
        storeSelection()
        executeOrder()
        controller.changeInExecutionMode(false)
    }

    private fun storeSelection() {
        controller.apply {
            settings.apply {
                lastPair = pair.value.toString()
                lastHighPrice = highPirce.value as Double
                lastLowPrice = lowPirce.value as Double
                lastAmount = (amount.value as Double).toInt()
                lastOrderType = orderType.value.toString()
                lastSide = side.value.toString()
                lastDistributionType = distribution.value.toString()
                lastDistributionParameter = parameter.value as Double
                lastMinAmount = (minAmount.value as Double).toInt()
                lastReversed = reversed.isSelected
                lastPostOnly = postOnly.isSelected
                lastReduceOnly = reduceOnly.isSelected
            }
            Settings.store()
        }
    }

    private suspend fun executeOrder() {
        controller.apply {
            exchange?.placeBulkOrders(
                    pair = pair.value.toString().toCurrencyPair(),
                    side = if (side.value.toString() == "BUY") Order.OrderType.BID else Order.OrderType.ASK,
                    type = XChangeWrapper.OrderType.valueOf(orderType.value.toString().replace("-", "_")),
                    amount = amount.value as Double,
                    minimumAmount = minAmount.value as Double,
                    priceLow = lowPirce.value as Double,
                    priceHigh = highPirce.value as Double,
                    distribution = XChangeWrapper.BulkDistribution.valueOf(distribution.value.toString()),
                    distributionParameter = parameter.value as Double,
                    postOnly = postOnly.isSelected,
                    reduceOnly = reduceOnly.isSelected,
                    reversed = reversed.isSelected)
        }
    }

    fun onAboutClicked() = AppDelegate.openAbout()

    fun onSettingsClicked() = AppDelegate.openSettings()

    fun onQuitClicked() = controller.exitApp()
}