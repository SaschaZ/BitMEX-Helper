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
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.control.*
import javafx.scene.control.cell.ComboBoxTableCell
import javafx.scene.control.cell.PropertyValueFactory
import javafx.util.Callback
import javafx.util.StringConverter
import javafx.util.converter.DoubleStringConverter
import javafx.util.converter.IntegerStringConverter
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
            }
            highPirce.apply {
                valueFactory.value = settings.lastHighPrice
                valueProperty().addListener { _, _, _ -> updateView() }
                enableBetterListener()
            }
            lowPrice.apply {
                valueFactory.value = settings.lastLowPrice
                valueProperty().addListener { _, _, _ -> updateView() }
                enableBetterListener()
            }
            amount.apply {
                valueFactory.value = settings.lastAmount.toDouble()
                valueProperty().addListener { _, _, _ -> updateView() }
                enableBetterListener()
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
                value = items[Constants.distributions.indexOf(settings.lastMode)]
                setOnAction { updateView() }
            }
            parameter.apply {
                valueFactory.value = settings.lastDistributionParameter
                valueProperty().addListener { _, _, _ -> updateView() }
                enableBetterListener()
            }
            minAmount.apply {
                valueFactory.value = settings.lastMinAmount.toDouble()
                valueProperty().addListener { _, _, _ -> updateView() }
                enableBetterListener()
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

            initLinkedTable()
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
        controller.apply {
            review.items = FXCollections.observableArrayList<ReviewItem>(
                    orders.sortedByDescending { it.price }.map { ReviewItem(it.price, it.orderQuantity) }
            )
            reviewPriceColumn.sortType = javafx.scene.control.TableColumn.SortType.ASCENDING
        }
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

    @Suppress("unused")
    data class LinkedTableItem(private val price: SimpleDoubleProperty,
                               private val amount: SimpleIntegerProperty,
                               private val orderType: SimpleStringProperty,
                               private val orderTypeParameter: SimpleDoubleProperty,
                               private val linkId: SimpleStringProperty,
                               private val linkType: SimpleStringProperty) {
        constructor(price: Double,
                    amount: Int,
                    orderType: OrderType,
                    orderTypeParameter: Double,
                    linkId: String,
                    linkType: XChangeWrapper.OrderLinkType) : this(
                SimpleDoubleProperty(price),
                SimpleIntegerProperty(amount),
                SimpleStringProperty(orderType.toString()),
                SimpleDoubleProperty(orderTypeParameter),
                SimpleStringProperty(linkId),
                SimpleStringProperty(linkType.toString()))

        fun getPrice(): Double = price.get()
        fun setPrice(value: Double) = price.set(value)
        fun getAmount(): Int = amount.get()
        fun setAmount(value: Int) = amount.set(value)
        fun getOrderType(): String = orderType.get()
        fun setOrderType(value: String) = orderType.set(value)
        fun getOrderTypeParameter(): Double = orderTypeParameter.get()
        fun setOrderTypeParameter(value: Double) = orderTypeParameter.set(value)
        fun getLinkId(): String = linkId.get()
        fun setLinkId(value: String) = linkId.set(value)
        fun getLinkType(): String = linkType.get()
        fun setLinkType(value: String) = linkType.set(value)
    }

    private fun initLinkedTable() {
        controller.apply {
            linkedPriceColumn.cellValueFactory = PropertyValueFactory<LinkedTableItem, Double>("price")
            linkedPriceColumn.cellFactory = Callback<TableColumn<LinkedTableItem, Double>, TableCell<LinkedTableItem, Double>> {
                EditCell(DoubleStringConverter())
            }
            linkedPriceColumn.setOnEditCommit { event ->
                linkedOrders[event.tablePosition.row].setPrice(event.newValue)
            }

            linkedAmountColumn.cellValueFactory = PropertyValueFactory<LinkedTableItem, Int>("amount")
            linkedAmountColumn.cellFactory = Callback<TableColumn<LinkedTableItem, Int>, TableCell<LinkedTableItem, Int>> {
                EditCell(IntegerStringConverter())
            }
            linkedAmountColumn.setOnEditCommit { event ->
                linkedOrders[event.tablePosition.row].setAmount(event.newValue)
            }

            linkedOrderTypeColumn.cellValueFactory = PropertyValueFactory<LinkedTableItem, String>("orderType")
            linkedOrderTypeColumn.cellFactory = ComboBoxTableCell.forTableColumn(*XChangeWrapper.OrderType.values()
                    .map { it.toString() }.toTypedArray())
            linkedOrderTypeColumn.setOnEditCommit { event ->
                linkedOrders[event.tablePosition.row].setOrderType(event.newValue)
            }

            linkedOrderTypeParameterColumn.cellValueFactory = PropertyValueFactory<LinkedTableItem, Double>("orderTypeParameter")
            linkedOrderTypeParameterColumn.cellFactory = Callback<TableColumn<LinkedTableItem, Double>, TableCell<LinkedTableItem, Double>> {
                EditCell(DoubleStringConverter())
            }
            linkedOrderTypeParameterColumn.setOnEditCommit { event ->
                linkedOrders[event.tablePosition.row].setOrderTypeParameter(event.newValue)
            }

            linkedLinkIdColumn.cellValueFactory = PropertyValueFactory<LinkedTableItem, String>("linkId")
            linkedLinkIdColumn.cellFactory = Callback<TableColumn<LinkedTableItem, String>, TableCell<LinkedTableItem, String>> {
                EditCell(object : StringConverter<String>() {
                    override fun toString(value: String?) = value
                    override fun fromString(string: String?) = string
                })
            }
            linkedLinkIdColumn.setOnEditCommit { event ->
                linkedOrders[event.tablePosition.row].setLinkId(event.newValue)
            }

            linkedLinkTypeColumn.cellValueFactory = PropertyValueFactory<LinkedTableItem, String>("linkType")
            linkedLinkTypeColumn.cellFactory = ComboBoxTableCell.forTableColumn(*XChangeWrapper.OrderLinkType.values()
                    .map { it.toString() }.toMutableList().toTypedArray())
            linkedLinkTypeColumn.setOnEditCommit { event ->
                linkedOrders[event.tablePosition.row].setLinkType(event.newValue)
            }
        }
    }

    private fun updateLinkedOrders() {
        controller.apply {
            linkedOrdersTable.items = FXCollections.observableArrayList<LinkedTableItem>(linkedOrders)
        }
    }

    private val linkedOrders = ArrayList<LinkedTableItem>()

    fun onAddLinkedOrderClicked() {
        linkedOrders.add(LinkedTableItem(100.0, 10, OrderType.LIMIT, 0.0, "fooboo", XChangeWrapper.OrderLinkType.NONE))
        updateLinkedOrders()
    }
}


fun Spinner<Double>.enableBetterListener() {
    editor.textProperty().addListener { _, _, new ->
        if (new.isNotBlank())
            valueFactory.value = new.replace(",", ".").toDouble()
    }
}