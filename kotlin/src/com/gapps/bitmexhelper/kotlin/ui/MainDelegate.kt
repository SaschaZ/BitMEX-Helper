@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.gapps.bitmexhelper.kotlin.ui

import com.gapps.bitmexhelper.kotlin.XChangeWrapper
import com.gapps.bitmexhelper.kotlin.XChangeWrapper.BulkDistribution
import com.gapps.bitmexhelper.kotlin.XChangeWrapper.OrderType
import com.gapps.bitmexhelper.kotlin.persistance.Constants
import com.gapps.bitmexhelper.kotlin.persistance.Settings
import com.gapps.bitmexhelper.kotlin.persistance.Settings.Companion.settings
import com.gapps.bitmexhelper.kotlin.toCurrencyPair
import javafx.application.Platform
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.knowm.xchange.bitmex.BitmexExchange
import org.knowm.xchange.dto.Order
import org.knowm.xchange.dto.Order.OrderType.ASK
import org.knowm.xchange.dto.Order.OrderType.BID
import si.mazi.rescu.HttpStatusIOException
import java.lang.reflect.UndeclaredThrowableException


object MainDelegate {

    private lateinit var controller: MainController

    fun onControllerAvailable(controller: MainController) {
        MainDelegate.controller = controller
    }

    private var exchange: XChangeWrapper? = null

    fun onSceneSet() {
        launch {
            exchange = XChangeWrapper(BitmexExchange::class, Settings.getBitmexApiKey(), Settings.getBitmexApiSecret())
            updateView()
        }

        controller.apply {
            pair.apply {
                items = FXCollections.observableArrayList(Constants.pairs)
                value = items[Constants.pairs.indexOf(settings.lastPair)]
                setOnAction { updateView() }
            }
            highPirce.apply {
                valueFactory.value = settings.lastHighPrice
                valueProperty().addListener { _, _, _ -> updateView() }
                editor.textProperty().addListener { _, _, new -> valueFactory.value = new.toDouble() }
            }
            lowPirce.apply {
                valueFactory.value = settings.lastLowPrice
                valueProperty().addListener { _, _, _ -> updateView() }
                editor.textProperty().addListener { _, _, new -> valueFactory.value = new.toDouble() }
            }
            amount.apply {
                valueFactory.value = settings.lastAmount.toDouble()
                valueProperty().addListener { _, _, _ -> updateView() }
                editor.textProperty().addListener { _, _, new -> valueFactory.value = new.toDouble() }
            }
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
            parameter.apply {
                valueFactory.value = settings.lastDistributionParameter
                valueProperty().addListener { _, _, _ -> updateView() }
                editor.textProperty().addListener { _, _, new -> valueFactory.value = new.toDouble() }
            }
            minAmount.apply {
                valueFactory.value = settings.lastMinAmount.toDouble()
                valueProperty().addListener { _, _, _ -> updateView() }
                editor.textProperty().addListener { _, _, new -> valueFactory.value = new.toDouble() }
            }
            reversed.isSelected = settings.lastReversed
            reversed.setOnAction { updateView() }
            postOnly.isSelected = settings.lastPostOnly
            postOnly.setOnAction { updateView() }
            reduceOnly.isSelected = settings.lastReduceOnly
            reduceOnly.setOnAction { updateView() }
            review.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            reviewPriceColumn.cellValueFactory = PropertyValueFactory<ReviewItem, Double>("price")
            reviewAmountColumn.cellValueFactory = PropertyValueFactory<ReviewItem, Int>("amount")
        }
    }

    private fun updateView() {
        controller.apply {
            exchange?.createBulkOrders(
                    pair = pair.value.toString().toCurrencyPair(),
                    side = if (side.value.toString() == "BUY") BID else ASK,
                    amount = amount.value as Double,
                    minimumAmount = minAmount.value as Double,
                    priceLow = lowPirce.value as Double,
                    priceHigh = highPirce.value as Double,
                    distribution = BulkDistribution.valueOf(distribution.value.toString()),
                    distributionParameter = parameter.value as Double,
                    postOnly = postOnly.isSelected,
                    reduceOnly = reduceOnly.isSelected,
                    reversed = reversed.isSelected)?.let { orders ->
                updatePreview(orders)
                updateStats(orders)
            }
        }
    }

    @Suppress("unused")
    internal data class ReviewItem(private val price: SimpleDoubleProperty, private val amount: SimpleIntegerProperty) {
        constructor(price: Double, amount: Int) : this(SimpleDoubleProperty(price), SimpleIntegerProperty(amount))

        fun getPrice(): Double = price.get()
        fun getAmount(): Int = amount.get()
    }

    private fun updatePreview(orders: List<XChangeWrapper.BulkOrder>) {
        controller.review.items = FXCollections.observableArrayList<ReviewItem>(
                orders.sortedByDescending { it.price }.map { ReviewItem(it.price, it.orderQuantity) }
        )
        controller.reviewPriceColumn.sortType = javafx.scene.control.TableColumn.SortType.ASCENDING
    }

    private fun updateStats(orders: List<XChangeWrapper.BulkOrder>) {
        val sum = orders.sumBy { it.orderQuantity }
        val averagePrice = orders.sumByDouble { it.price * it.orderQuantity / sum }

        controller.stats.text = " total bulk order amount: " + sum + "\n" +
                " average price: " + String.format("%.1f", averagePrice) + "\n" +
                " order count: " + orders.size + "\n" +
                " min. order amount: " + orders.minBy { it.orderQuantity }?.orderQuantity + "\n" +
                " average order amount: " + String.format("%.1f", sum.toDouble() / orders.size) + "\n" +
                " max. order amount: " + orders.maxBy { it.orderQuantity }?.orderQuantity + "\n"
    }

    internal fun onExecuteClicked() {
        controller.changeInExecutionMode(true)
        launch {
            var error: Throwable? = null

            val result = async {
                storeSelection()
                try {
                    executeOrder()
                } catch (t: Throwable) {
                    error = t
                }
            }.await()

            Platform.runLater {
                controller.changeInExecutionMode(false)
                if (result == null || error != null)
                    AppDelegate.showError(((error as? UndeclaredThrowableException)?.undeclaredThrowable as? HttpStatusIOException)?.httpBody
                            ?: "Something went wrong.")
            }
        }
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

    private suspend fun executeOrder(): List<Order>? {
        controller.apply {
            exchange?.placeBulkOrders(
                    pair = pair.value.toString().toCurrencyPair(),
                    side = if (side.value.toString() == "BUY") BID else ASK,
                    type = OrderType.valueOf(orderType.value.toString().toUpperCase().replace("-", "_")),
                    amount = amount.value as Double,
                    minimumAmount = minAmount.value as Double,
                    priceLow = lowPirce.value as Double,
                    priceHigh = highPirce.value as Double,
                    distribution = BulkDistribution.valueOf(distribution.value.toString()),
                    distributionParameter = parameter.value as Double,
                    postOnly = postOnly.isSelected,
                    reduceOnly = reduceOnly.isSelected,
                    reversed = reversed.isSelected)
        }
        return null
    }

    fun onSettingsClicked() = AppDelegate.openSettings()
}