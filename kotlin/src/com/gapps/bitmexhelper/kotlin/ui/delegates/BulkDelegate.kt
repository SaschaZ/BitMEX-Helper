package com.gapps.bitmexhelper.kotlin.ui.delegates

import com.gapps.bitmexhelper.kotlin.*
import com.gapps.bitmexhelper.kotlin.persistance.Constants
import com.gapps.bitmexhelper.kotlin.persistance.Settings
import com.gapps.bitmexhelper.kotlin.ui.controller.MainController
import com.gapps.utils.whenNotNull
import javafx.application.Platform
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import kotlinx.coroutines.experimental.launch
import org.knowm.xchange.bitmex.dto.trade.BitmexPlaceOrderParameters
import org.knowm.xchange.bitmex.dto.trade.BitmexSide
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.Order.OrderType.*
import org.knowm.xchange.dto.marketdata.Ticker
import org.knowm.xchange.exceptions.ExchangeException

object BulkDelegate {

    private lateinit var controller: MainController
    private lateinit var exchange: XChangeWrapper

    private var tickers: Map<CurrencyPair, Ticker>? = null

    fun onSceneSet(controller: MainController, exchange: XChangeWrapper) {
        BulkDelegate.controller = controller
        BulkDelegate.exchange = exchange

        controller.changeInExecutionMode(true)
        launch {
            tickers = exchange.getTickers()
            Platform.runLater {
                configureSpinnerParameters(controller.pair.value.toString().toCurrencyPair(), true)
                controller.changeInExecutionMode(false)
            }
        }

        controller.apply {
            pair.apply {
                items = FXCollections.observableArrayList(Constants.pairs)
                value = items[Constants.pairs.indexOf(Settings.settings.lastPair).let { if (it < 0) 0 else it }]
                setOnAction {
                    configureSpinnerParameters(value.toString().toCurrencyPair())
                    updateView()
                }
                enableValueChangeOnScroll()
            }
            highPirce.apply {
                valueFactory.value = Settings.settings.lastHighPrice
                valueProperty().addListener { _, _, _ -> updateView() }
                enableBetterListener()
                enableSpinnerChangeOnScroll()
            }
            lowPrice.apply {
                valueFactory.value = Settings.settings.lastLowPrice
                valueProperty().addListener { _, _, _ -> updateView() }
                enableBetterListener()
                enableSpinnerChangeOnScroll()
            }
            amount.apply {
                valueFactory.value = Settings.settings.lastAmount.toDouble()
                valueProperty().addListener { _, _, _ -> updateView() }
                enableBetterListener()
                enableSpinnerChangeOnScroll()
            }
            orderType.apply {
                items = FXCollections.observableArrayList(Constants.orderTypes)
                value = items[Constants.orderTypes.indexOf(Settings.settings.lastOrderType)]
                setOnAction { updateView() }
                enableValueChangeOnScroll()
            }
            side.apply {
                items = FXCollections.observableArrayList(Constants.sides)
                value = items[Constants.sides.indexOf(Settings.settings.lastSide)]
                setOnAction { updateView() }
                enableValueChangeOnScroll()
            }
            distribution.apply {
                val values = BulkDistribution.values().map { it.toString() }
                items = FXCollections.observableArrayList(values)
                value = items[values.indexOf(Settings.settings.lastMode)]
                setOnAction { updateView() }
                enableValueChangeOnScroll()
            }
            parameter.apply {
                valueFactory.value = Settings.settings.lastDistributionParameter
                valueProperty().addListener { _, _, _ -> updateView() }
                enableBetterListener()
                enableSpinnerChangeOnScroll()
            }
            minAmount.apply {
                valueFactory.value = Settings.settings.lastMinAmount.toDouble()
                valueProperty().addListener { _, _, _ -> updateView() }
                enableBetterListener()
                enableSpinnerChangeOnScroll()
            }
            slDistance.apply {
                valueFactory.value = Settings.settings.lastSlDistance.toDouble()
                valueProperty().addListener { _, _, _ -> updateView() }
                enableBetterListener()
                enableSpinnerChangeOnScroll()
            }
            reversed.isSelected = Settings.settings.lastReversed
            reversed.setOnAction { updateView() }
            postOnly.isSelected = Settings.settings.lastPostOnly
            postOnly.setOnAction { updateView() }
            reduceOnly.isSelected = Settings.settings.lastReduceOnly
            reduceOnly.setOnAction { updateView() }

            review.apply {
                columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
                setPlaceholder(Label("Current parameters do not return any orders."))
            }
            reviewPriceColumn.cellValueFactory = PropertyValueFactory<PreviewItem, Double>("price")
            reviewAmountColumn.cellValueFactory = PropertyValueFactory<PreviewItem, Int>("amount")
        }

        updateView()
    }

    private fun configureSpinnerParameters(pair: CurrencyPair, useSettingsAsInitial: Boolean = false) {
        val minStep = Constants.minimumPriceSteps[pair] ?: 0.00000001
        controller.apply {
            val lastTicker = tickers?.get(pair)?.last?.toDouble()
            highPirce.valueFactory = SmallDoubleValueFactory(minStep, 10000000.0,
                    if (useSettingsAsInitial && Settings.settings.lastHighPrice > 0.0) Settings.settings.lastHighPrice
                    else lastTicker?.let { it + 10 * minStep } ?: minStep, minStep)
            lowPrice.valueFactory = SmallDoubleValueFactory(minStep, 10000000.0,
                    if (useSettingsAsInitial && Settings.settings.lastLowPrice > 0.0) Settings.settings.lastLowPrice
                    else lastTicker?.let { it - 10 * minStep } ?: minStep, minStep)
        }
        updateView()
    }

    private fun updateView() {
        controller.apply {
            val isDistributionSame = BulkDistribution.valueOf(controller.distribution.value.toString()) == BulkDistribution.SAME
            (parameter.valueFactory as? SpinnerValueFactory.DoubleSpinnerValueFactory)?.apply {
                min = if (isDistributionSame) 1.0 else 0.01
                max = if (isDistributionSame) 100.0 else 10.0
                amountToStepBy = if (isDistributionSame) 1.0 else 0.01
            }
            parameter.editor.text = controller.parameter.editor.text?.let { text ->
                if (isDistributionSame) text.substring(0, text.indexOf(".").let { index ->
                    if (index < 0) text.indexOf(",").let { index2 ->
                        if (index2 < 0) text.length else index2
                    } else index
                }) else text
            }
            val isStopLimit = orderType.value.toString().toLowerCase().replace("-", "_") ==
                    BulkOrderType.STOP_LIMIT.toString().toLowerCase()
            slDistance.isVisible = isStopLimit
            slDistanceLabel.isVisible = isStopLimit
        }

        createOrders()?.also { orders ->
            updatePreview(orders)
            updateStats(orders)
        }
    }

    @Suppress("unused")
    internal data class PreviewItem(private val price: SimpleDoubleProperty, private val amount: SimpleIntegerProperty) {
        constructor(price: Double, amount: Int) : this(SimpleDoubleProperty(price), SimpleIntegerProperty(amount))

        fun getPrice(): Double = price.get()
        fun getAmount(): Int = amount.get()
    }

    private fun updatePreview(orders: List<BitmexPlaceOrderParameters>) {
        controller.apply {
            review.items = FXCollections.observableArrayList<PreviewItem>(
                    orders.asSequence().sortedByDescending { it.price ?: it.stopPrice }.mapNotNull {
                        whenNotNull(it.price ?: it.stopPrice, it.orderQuantity) { price, quantity ->
                            PreviewItem(price.toDouble(), quantity.toInt())
                        }
                    }.toList()
            )
            reviewPriceColumn.sortType = javafx.scene.control.TableColumn.SortType.ASCENDING
        }
    }

    private fun updateStats(orders: List<BitmexPlaceOrderParameters>) {
        if (orders.isNotEmpty()) {
            val sum = orders.sumBy { it.orderQuantity?.toInt() ?: 0 }
            val averagePrice = orders.sumByDouble {
                (it.price?.toDouble() ?: it.stopPrice?.toDouble() ?: 0.0) * (it.orderQuantity?.toDouble() ?: 0.0) / sum
            }

            controller.stats.text = " total bulk order amount: " + sum + "\n" +
                    " average price: ${averagePrice.roundToMinimumStep(controller.pair.value.toString().toCurrencyPair())}\n" +
                    " order count: ${orders.size}\n" +
                    " min. order amount: ${orders.minBy {
                        it.orderQuantity?.toDouble() ?: Double.MAX_VALUE
                    }?.orderQuantity}\n" +
                    " average order amount: ${String.format("%.1f", sum.toDouble() / orders.size)}\n" +
                    " max. order amount: ${orders.maxBy { it.orderQuantity?.toDouble() ?: 0.0 }?.orderQuantity}\n"
        } else controller.stats.text = ""
    }

    internal fun onExecuteClicked() {
        controller.changeInExecutionMode(true)
        launch {
            var error: ExchangeException? = null

            storeSelection()
            val result = try {
                executeOrder()
            } catch (e: ExchangeException) {
                error = e
                null
            }


            Platform.runLater {
                controller.changeInExecutionMode(false)
                if (result == null || error != null)
                    MainDelegate.reportError(error)
                else
                    MainDelegate.reportCanceledOrders(result)
            }
        }
    }

    private fun storeSelection() {
        controller.apply {
            Settings.settings.apply {
                lastPair = pair.value.toString()
                lastHighPrice = highPirce.value as Double
                lastLowPrice = lowPrice.value as Double
                lastAmount = (amount.value as Double).toInt()
                lastOrderType = orderType.value.toString()
                lastSide = side.value.toString()
                lastMode = distribution.value.toString()
                lastDistributionParameter = parameter.value as Double
                lastMinAmount = (minAmount.value as Double).toInt()
                lastSlDistance = (slDistance.value as Double).toInt()
                lastReversed = reversed.isSelected
                lastPostOnly = postOnly.isSelected
                lastReduceOnly = reduceOnly.isSelected
            }
            Settings.store()
        }
    }

    private fun executeOrder() = createOrders()?.let { exchange.placeBulkOrders(it) }

    fun createOrders(): List<BitmexPlaceOrderParameters>? {
        controller.apply {
            return exchange.createBulkOrders(
                    pair = pair.value.toString().toCurrencyPair(),
                    orderSide = if (BitmexSide.fromString(side.value.toString()) == BitmexSide.BUY) BID else ASK,
                    type = BulkOrderType.valueOf(orderType.value.toString().toUpperCase().replace("-", "_")),
                    amount = (amount.value as Double).toInt(),
                    minimumAmount = minAmount.value as Double,
                    slDistance = (slDistance.value as Double).toInt(),
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
}


fun Spinner<*>.enableBetterListener() {
    editor.textProperty().addListener { _, _, new ->
        if (new.isNotBlank() && new != "-")
            if (valueFactory.value is Double)
                valueFactory.value = new.replace(",", ".").toDouble()
            else if (valueFactory.value is Int)
                valueFactory.value = new.toInt()
    }
}

fun Spinner<*>.enableSpinnerChangeOnScroll() {
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