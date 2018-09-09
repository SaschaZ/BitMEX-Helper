@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.gapps.bitmexhelper.kotlin.ui

import com.gapps.bitmexhelper.kotlin.XChangeWrapper
import com.gapps.bitmexhelper.kotlin.XChangeWrapper.BulkDistribution
import com.gapps.bitmexhelper.kotlin.XChangeWrapper.OrderType
import com.gapps.bitmexhelper.kotlin.persistance.Constants
import com.gapps.bitmexhelper.kotlin.persistance.Settings
import com.gapps.bitmexhelper.kotlin.persistance.Settings.Companion.settings
import com.gapps.bitmexhelper.kotlin.roundToMinimumStep
import com.gapps.bitmexhelper.kotlin.toCurrencyPair
import javafx.application.Platform
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.scene.control.ComboBox
import javafx.scene.control.CustomSpinnerValueFactory
import javafx.scene.control.Spinner
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.knowm.xchange.bitmex.BitmexExchange
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.Order
import org.knowm.xchange.dto.Order.OrderType.ASK
import org.knowm.xchange.dto.Order.OrderType.BID
import org.knowm.xchange.dto.marketdata.Ticker
import si.mazi.rescu.HttpStatusIOException
import java.lang.reflect.UndeclaredThrowableException


object MainDelegate {

    private lateinit var controller: MainController

    fun onControllerAvailable(controller: MainController) {
        MainDelegate.controller = controller
    }

    private var exchange: XChangeWrapper? = null
    private var tickers: Map<CurrencyPair, Ticker>? = null

    fun onSceneSet() {
        controller.changeInExecutionMode(true)
        launch {
            exchange = XChangeWrapper(BitmexExchange::class, Settings.getBitmexApiKey(), Settings.getBitmexApiSecret()).also {
                tickers = it.getTickers()
                Platform.runLater {
                    configureSpinnerParameters(controller.pair.value.toString().toCurrencyPair(), true)
                    controller.changeInExecutionMode(false)
                }
            }
        }

        controller.apply {
            pair.apply {
                items = FXCollections.observableArrayList(Constants.pairs)
                value = items[Constants.pairs.indexOf(settings.lastPair).let { if (it < 0) 0 else it }]
                setOnAction {
                    configureSpinnerParameters(pair.value.toString().toCurrencyPair())
                    updateView()
                }
                enableValueChangeOnScroll()
            }
            highPirce.apply {
                valueFactory.value = settings.lastHighPrice
                valueProperty().addListener { _, _, _ -> updateView() }
                enableBetterListener()
                enableValueChangeOnScroll()
            }
            lowPrice.apply {
                valueFactory.value = settings.lastLowPrice
                valueProperty().addListener { _, _, _ -> updateView() }
                enableBetterListener()
                enableValueChangeOnScroll()
            }
            amount.apply {
                valueFactory.value = settings.lastAmount.toDouble()
                valueProperty().addListener { _, _, _ -> updateView() }
                enableBetterListener()
                enableValueChangeOnScroll()
            }
            orderType.apply {
                items = FXCollections.observableArrayList(Constants.orderTypes)
                value = items[Constants.orderTypes.indexOf(settings.lastOrderType)]
                setOnAction { updateView() }
                enableValueChangeOnScroll()
            }
            side.apply {
                items = FXCollections.observableArrayList(Constants.sides)
                value = items[Constants.sides.indexOf(settings.lastSide)]
                setOnAction { updateView() }
                enableValueChangeOnScroll()
            }
            distribution.apply {
                val values = XChangeWrapper.BulkDistribution.values().map { it.toString() }
                items = FXCollections.observableArrayList(values)
                value = items[values.indexOf(settings.lastMode)]
                setOnAction { updateView() }
                enableValueChangeOnScroll()
            }
            parameter.apply {
                valueFactory.value = settings.lastDistributionParameter
                valueProperty().addListener { _, _, _ -> updateView() }
                enableBetterListener()
                enableValueChangeOnScroll()
            }
            minAmount.apply {
                valueFactory.value = settings.lastMinAmount.toDouble()
                valueProperty().addListener { _, _, _ -> updateView() }
                enableBetterListener()
                enableValueChangeOnScroll()
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
        updateView()
    }

    private fun configureSpinnerParameters(pair: CurrencyPair, useSettingsAsInitial: Boolean = false) {
        val minStep = Constants.minimumPriceSteps[pair] ?: 0.00000001
        controller.apply {
            val lastTicker = tickers?.get(pair)?.last?.toDouble()
            highPirce.valueFactory = CustomSpinnerValueFactory(minStep, 10000000.0,
                    if (useSettingsAsInitial && settings.lastHighPrice > 0.0) settings.lastHighPrice
                    else lastTicker?.let { it + 10 * minStep } ?: minStep, minStep)
            lowPrice.valueFactory = CustomSpinnerValueFactory(minStep, 10000000.0,
                    if (useSettingsAsInitial && settings.lastLowPrice > 0.0) settings.lastLowPrice
                    else lastTicker?.let { it - 10 * minStep } ?: minStep, minStep)
        }
        updateView()
    }

    private fun updateView() {
        controller.apply {
            exchange?.createBulkOrders(
                    pair = pair.value.toString().toCurrencyPair(),
                    side = if (side.value.toString() == "BUY") BID else ASK,
                    amount = amount.value as Double,
                    minimumAmount = minAmount.value as Double,
                    priceLow = lowPrice.value as Double,
                    priceHigh = highPirce.value as Double,
                    distribution = BulkDistribution.valueOf(distribution.value.toString()),
                    distributionParameter = parameter.value as Double,
                    postOnly = postOnly.isSelected,
                    reduceOnly = reduceOnly.isSelected,
                    reversed = reversed.isSelected)?.let { orders ->
                updatePreview(orders)
                updateStats(orders)
            } ?: {
                updatePreview(emptyList())
                updateStats(emptyList())
            }.invoke()
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
        if (orders.isNotEmpty()) {
            val sum = orders.sumBy { it.orderQuantity }
            val averagePrice = orders.sumByDouble { it.price * it.orderQuantity / sum }

            controller.stats.text = " total bulk order amount: " + sum + "\n" +
                    " average price: ${averagePrice.roundToMinimumStep(controller.pair.value.toString().toCurrencyPair())}\n" +
                    " order count: ${orders.size}\n" +
                    " min. order amount: ${orders.minBy { it.orderQuantity }?.orderQuantity}\n" +
                    " average order amount: ${String.format("%.1f", sum.toDouble() / orders.size)}\n" +
                    " max. order amount: ${orders.maxBy { it.orderQuantity }?.orderQuantity}\n"
        } else controller.stats.text = ""
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
                lastLowPrice = lowPrice.value as Double
                lastAmount = (amount.value as Double).toInt()
                lastOrderType = orderType.value.toString()
                lastSide = side.value.toString()
                lastMode = distribution.value.toString()
                lastDistributionParameter = parameter.value as Double
                lastMinAmount = (minAmount.value as Double).toInt()
                lastReversed = reversed.isSelected
                lastPostOnly = postOnly.isSelected
                lastReduceOnly = reduceOnly.isSelected
            }
            Settings.store()
        }
    }

    private fun executeOrder(): List<Order>? {
        controller.apply {
            return exchange?.placeBulkOrders(
                    pair = pair.value.toString().toCurrencyPair(),
                    side = if (side.value.toString() == "BUY") BID else ASK,
                    type = OrderType.valueOf(orderType.value.toString().toUpperCase().replace("-", "_")),
                    amount = amount.value as Double,
                    minimumAmount = minAmount.value as Double,
                    priceLow = lowPrice.value as Double,
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


fun Spinner<Double>.enableBetterListener() {
    editor.textProperty().addListener { _, _, new ->
        if (new.isNotBlank())
            valueFactory.value = new.replace(",", ".").toDouble() // FIX datatype
    }
}

fun Spinner<Double>.enableValueChangeOnScroll() {
    setOnScroll { event ->
        if (event.deltaY > 0)
            increment()
        else if (event.deltaY < 0)
            decrement()
    }
}

fun ComboBox<*>.enableValueChangeOnScroll() {
    setOnScroll { event ->
        if (event.deltaY > 0) {
            val index = items.indexOf(value)
            value = if (index - 1 >= 0)
                items[index - 1]
            else
                items[items.size - 1]
        } else if (event.deltaY < 0) {
            val index = items.indexOf(value)
            value = if (index + 1 < items.size)
                items[index + 1]
            else
                items[0]
        }
    }
}