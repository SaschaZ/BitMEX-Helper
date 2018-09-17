package com.gapps.bitmexhelper.kotlin.ui.delegates

import com.gapps.bitmexhelper.kotlin.BulkOrderType
import com.gapps.bitmexhelper.kotlin.BulkOrderType.*
import com.gapps.bitmexhelper.kotlin.XChangeWrapper
import com.gapps.bitmexhelper.kotlin.persistance.Constants
import com.gapps.bitmexhelper.kotlin.persistance.Settings
import com.gapps.bitmexhelper.kotlin.toBitmexSymbol
import com.gapps.bitmexhelper.kotlin.toCurrencyPair
import com.gapps.bitmexhelper.kotlin.ui.EditCell
import com.gapps.bitmexhelper.kotlin.ui.controller.MainController
import com.gapps.bitmexhelper.kotlin.ui.SpinnerCell
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.control.Label
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.cell.CheckBoxTableCell
import javafx.scene.control.cell.ComboBoxTableCell
import javafx.scene.control.cell.PropertyValueFactory
import javafx.util.Callback
import javafx.util.StringConverter
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.knowm.xchange.bitmex.BitmexException
import org.knowm.xchange.bitmex.dto.trade.*
import org.knowm.xchange.bitmex.dto.trade.BitmexExecutionInstruction.*
import org.knowm.xchange.exceptions.ExchangeException
import java.lang.Exception

object LinkedDelegate {

    enum class LinkType {

        NONE,
        OCO,
        OTO,
        OUOA,
        OUOP;

        fun toBitmexContingencyType() = when (this) {
            OCO -> BitmexContingencyType.OCO
            OTO -> BitmexContingencyType.OTO
            OUOA -> BitmexContingencyType.OUOA
            OUOP -> BitmexContingencyType.OUOP
            else -> null
        }
    }

    @Suppress("unused", "MemberVisibilityCanBePrivate")
    data class LinkedTableItem(private val side: SimpleStringProperty,
                               private val price: SimpleDoubleProperty,
                               private val amount: SimpleIntegerProperty,
                               private val orderType: SimpleStringProperty,
                               private val orderTypeParameter: SimpleDoubleProperty,
                               private val linkId: SimpleStringProperty,
                               private val linkType: SimpleStringProperty,
                               private val postOnly: SimpleBooleanProperty,
                               private val reduceOnly: SimpleBooleanProperty) {

        constructor(side: String = "Buy",
                    price: Double = -1.0,
                    amount: Int = 1,
                    orderType: BulkOrderType = LIMIT,
                    orderTypeParameter: Double = -1.0,
                    linkId: String = "",
                    linkType: LinkType = LinkType.NONE,
                    postOnly: Boolean = false,
                    reduceOnly: Boolean = false) : this(
                SimpleStringProperty(side),
                SimpleDoubleProperty(price),
                SimpleIntegerProperty(amount),
                SimpleStringProperty(orderType.toString()),
                SimpleDoubleProperty(orderTypeParameter),
                SimpleStringProperty(linkId),
                SimpleStringProperty(linkType.toString()),
                SimpleBooleanProperty(postOnly),
                SimpleBooleanProperty(reduceOnly))

        fun createNew(): LinkedTableItem {
            return LinkedTableItem(
                    getSide(),
                    getPrice(),
                    getAmount(),
                    BulkOrderType.valueOf(getOrderType()),
                    getOrderTypeParameter(),
                    getLinkId(),
                    LinkType.valueOf(getLinkType()),
                    getPostOnly(),
                    getReduceOnly())
        }

        fun getSide(): String = side.get()
        fun setSide(value: String) = side.set(value)
        fun getPrice(): Double = price.get()
        fun setPrice(value: Double) = price.set(value)
        fun getPriceProperty() = price
        fun getAmount(): Int = amount.get()
        fun setAmount(value: Int) = amount.set(value)
        fun getOrderType(): String = orderType.get()
        fun setOrderType(value: String) = orderType.set(value)
        fun getOrderTypeParameter(): Double = orderTypeParameter.get()
        fun setOrderTypeParameter(value: Double) = orderTypeParameter.set(value)
        fun getOrderTypeParameterProperty() = orderTypeParameter
        fun getLinkId(): String = linkId.get()
        fun setLinkId(value: String) = linkId.set(value)
        fun getLinkType(): String = linkType.get()
        fun setLinkType(value: String) = linkType.set(value)
        fun getPostOnly(): Boolean = postOnly.get()
        fun setPostOnly(value: Boolean) = postOnly.set(value)
        fun getPostOnlyProperty() = postOnly
        fun getReduceOnly(): Boolean = reduceOnly.get()
        fun setReduceOnly(value: Boolean) = reduceOnly.set(value)
        fun getReduceOnlyProperty() = reduceOnly
    }

    private lateinit var controller: MainController
    private lateinit var exchange: XChangeWrapper

    private var linkedOrders = ArrayList<LinkedTableItem>()
    private val priceSpinners = ArrayList<SpinnerCell<LinkedTableItem, Double>>()

    fun onSceneSet(controller: MainController, exchange: XChangeWrapper) {
        LinkedDelegate.controller = controller
        LinkedDelegate.exchange = exchange

        initLinkedTable()
    }

    private fun initLinkedTable() {
        controller.apply {
            linkedPair.apply {
                items = FXCollections.observableArrayList(Constants.pairs)
                value = items[Constants.pairs.indexOf(Settings.settings.lastPair).let { if (it < 0) 0 else it }]
                setOnAction { _ ->
                    val minStep = Constants.minimumPriceSteps[value.toString().toCurrencyPair()]!!
                    priceSpinners.forEach { it.setStep(minStep) }
                    linkedPriceColumn.initSpinnerCellValueFactory(minStep)
                    linkedOrderTypeParameterColumn.initSpinnerCellValueFactory(minStep)
                    linkedOrders = ArrayList<LinkedTableItem>().also { list ->
                        list.addAll(elements = linkedOrders.asSequence().map { item ->
                            item.copy(
                                    price = item.getPriceProperty()
                                            .also { if (it.value > 0) it.value = it.value - it.value % minStep else it.value },
                                    orderTypeParameter = item.getOrderTypeParameterProperty()
                                            .also { if (it.value > 0) it.value = it.value - it.value % minStep else it.value })
                        })
                    }
                }
                enableValueChangeOnScroll()
            }
            val minStep = Constants.minimumPriceSteps[linkedPair.value.toString().toCurrencyPair()]!!
            linkedSideColumn.apply {
                cellValueFactory = PropertyValueFactory<LinkedTableItem, String>("side")
                cellFactory = ComboBoxTableCell.forTableColumn("Buy", "Sell")
                setOnEditCommit { event ->
                    linkedOrders[event.tablePosition.row].setSide(event.newValue)
                }
            }

            linkedPriceColumn.apply {
                cellValueFactory = PropertyValueFactory<LinkedTableItem, Double>("price")
                initSpinnerCellValueFactory(minStep)
                setOnEditCommit { event ->
                    linkedOrders[event.tablePosition.row].setPrice(event.newValue)
                }
            }

            linkedAmountColumn.apply {
                cellValueFactory = PropertyValueFactory<LinkedTableItem, Int>("amount")
                cellFactory = Callback<TableColumn<LinkedTableItem, Int>, TableCell<LinkedTableItem, Int>> {
                    SpinnerCell(1, 10000000, 1, 1)
                }
                setOnEditCommit { event ->
                    linkedOrders[event.tablePosition.row].setAmount(event.newValue)
                }
            }

            linkedOrderTypeColumn.apply {
                cellValueFactory = PropertyValueFactory<LinkedTableItem, String>("orderType")
                cellFactory = ComboBoxTableCell.forTableColumn(*BulkOrderType.values()
                        .map { it.toString() }.toTypedArray())
                setOnEditCommit { event ->
                    linkedOrders[event.tablePosition.row].setOrderType(event.newValue)
                }
            }

            linkedOrderTypeParameterColumn.apply {
                cellValueFactory = PropertyValueFactory<LinkedTableItem, Double>("orderTypeParameter")
                initSpinnerCellValueFactory(minStep)
                setOnEditCommit { event ->
                    linkedOrders[event.tablePosition.row].setOrderTypeParameter(event.newValue)
                }
            }

            linkedLinkIdColumn.apply {
                cellValueFactory = PropertyValueFactory<LinkedTableItem, String>("linkId")
                cellFactory = Callback<TableColumn<LinkedTableItem, String>, TableCell<LinkedTableItem, String>> { _ ->
                    EditCell(object : StringConverter<String>() {
                        override fun toString(value: String?) = value
                        override fun fromString(string: String?) = string
                    })
                }
                setOnEditCommit { event ->
                    linkedOrders[event.tablePosition.row].setLinkId(event.newValue)
                }
            }

            linkedLinkTypeColumn.apply {
                cellValueFactory = PropertyValueFactory<LinkedTableItem, String>("linkType")
                cellFactory = ComboBoxTableCell.forTableColumn(*BitmexContingencyType.values()
                        .map { it.toString() }.toMutableList().also { it.add(0, "NONE") }.toTypedArray())
                setOnEditCommit { event ->
                    linkedOrders[event.tablePosition.row].setLinkType(event.newValue)
                }
            }

            linkedPostOnlyColumn.apply {
                setCellValueFactory { param -> param.value.getPostOnlyProperty() }
                cellFactory = CheckBoxTableCell.forTableColumn(linkedPostOnlyColumn)
                setOnEditCancel { event ->
                    linkedOrders[event.tablePosition.row].setPostOnly(event.newValue)
                }
            }

            linkedReduceOnlyColumn.apply {
                setCellValueFactory { param -> param.value.getReduceOnlyProperty() }
                cellFactory = CheckBoxTableCell.forTableColumn(linkedReduceOnlyColumn)
                setOnEditCancel { event ->
                    linkedOrders[event.tablePosition.row].setReduceOnly(event.newValue)
                }
            }
            linkedOrdersTable.setPlaceholder(Label("Add new orders with the '+' button or press the 'Move to Linked' button on the 'Bulk' page."))

        }
    }

    private fun TableColumn<LinkedTableItem, Double>.initSpinnerCellValueFactory(minStep: Double) {
        cellFactory = Callback<TableColumn<LinkedTableItem, Double>, TableCell<LinkedTableItem, Double>> {
            SpinnerCell<LinkedTableItem, Double>(0.0, 1000000000.0, 0.0, minStep).also { cell ->
                priceSpinners.add(cell)
            }
        }
    }

    private fun updateLinkedOrders() {
        controller.apply {
            linkedOrdersTable.items = FXCollections.observableArrayList<LinkedTableItem>(linkedOrders)
        }
    }

    fun onAddLinkedOrderClicked() {
        linkedOrders.add(linkedOrders.lastOrNull()?.createNew() ?: LinkedTableItem())
        updateLinkedOrders()
    }

    fun onRemoveLinkedOrderClicked() {
        val row = controller.linkedOrdersTable.selectionModel.selectedCells.firstOrNull()?.row ?: linkedOrders.lastIndex
        if (row in 0..linkedOrders.size) {
            linkedOrders.removeAt(row)
            if (linkedOrders.isEmpty()) {
                priceSpinners.clear()
            }
            updateLinkedOrders()
        }
    }

    fun onExecuteLinkedOrdersClicked() {
        controller.changeInExecutionMode(true)

        launch {
            val orderParameters = linkedOrders.map { item ->
                val side = BitmexSide.fromString(item.getSide())
                val orderType = BulkOrderType.valueOf(item.getOrderType())
                val price = when (orderType) {
                    LIMIT,
                    STOP_LIMIT -> item.getPrice()
                    else -> null
                }
                val stop = when (orderType) {
                    STOP_LIMIT,
                    STOP -> item.getOrderTypeParameter()
                    else -> null
                }
                val pegPriceType = when (orderType) {
                    TRAILING_STOP -> BitmexPegPriceType.TRAILING_STOP_PEG
                    else -> null
                }
                val pegPriceAmount = when (orderType) {
                    TRAILING_STOP -> item.getOrderTypeParameter() * if (side == BitmexSide.BUY) 1 else -1
                    else -> null
                }
                BitmexPlaceOrderParameters.Builder(controller.linkedPair.value.toString().toCurrencyPair().toBitmexSymbol())
                        .setOrderQuantity(item.getAmount().toBigDecimal())
                        .setPrice(price?.let { if (it < 0) null else it.toBigDecimal() })
                        .setStopPrice(stop?.let { if (it < 0) null else it.toBigDecimal() })
                        .setSide(side)
                        .setOrderType(orderType.toBitmexOrderType())
                        .setExecutionInstructions(BitmexExecutionInstruction.Builder().setPostOnly(item.getPostOnly())
                                .setReduceOnly(item.getReduceOnly()).setLastPrice(orderType == TRAILING_STOP).build())
                        .setContingencyType(LinkType.valueOf(item.getLinkType()).toBitmexContingencyType())
                        .setClOrdLinkId(item.getLinkId().let { if (it.isBlank()) null else it })
                        .setPegPriceType(pegPriceType)
                        .setPegOffsetValue(pegPriceAmount?.toBigDecimal())
                        .build()
            }

            var error: ExchangeException? = null
            val result = try {
                exchange.placeBulkOrders(orderParameters)
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

    fun addOrders(orders: List<BitmexPlaceOrderParameters>) {
        orders.forEach { order ->
            linkedOrders.add(LinkedTableItem(
                    side = order.side?.capitalized!!,
                    price = order.price?.toDouble() ?: -1.0,
                    amount = order.orderQuantity?.toInt() ?: 0,
                    orderType = when (order.orderType) {
                        BitmexOrderType.STOP -> STOP
                        BitmexOrderType.STOP_LIMIT -> STOP_LIMIT
                        BitmexOrderType.PEGGED -> TRAILING_STOP
                        else -> LIMIT
                    },
                    orderTypeParameter = order.stopPrice?.toDouble() ?: -1.0,
                    linkId = order.clOrdLinkId ?: "",
                    linkType = when (order.contingencyType) {
                        BitmexContingencyType.OCO -> LinkType.OCO
                        BitmexContingencyType.OTO -> LinkType.OTO
                        BitmexContingencyType.OUOP -> LinkType.OUOP
                        BitmexContingencyType.OUOA -> LinkType.OUOA
                        else -> LinkType.NONE
                    },
                    postOnly = order.executionInstructions?.contains(PARTICIPATE_DO_NOT_INITIATE) ?: false,
                    reduceOnly = order.executionInstructions?.contains(REDUCE_ONLY) ?: false
            ))
        }
        updateLinkedOrders()
    }
}