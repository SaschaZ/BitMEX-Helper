package com.gapps.bitmexhelper.kotlin.ui.delegates

import com.gapps.bitmexhelper.kotlin.exchange.*
import com.gapps.bitmexhelper.kotlin.exchange.BulkOrderType.*
import com.gapps.bitmexhelper.kotlin.persistance.Constants
import com.gapps.bitmexhelper.kotlin.persistance.Settings
import com.gapps.bitmexhelper.kotlin.ui.ComboBoxCell
import com.gapps.bitmexhelper.kotlin.ui.EditCell
import com.gapps.bitmexhelper.kotlin.ui.GreenRedRowFactory
import com.gapps.bitmexhelper.kotlin.ui.SpinnerCell
import com.gapps.bitmexhelper.kotlin.ui.controller.MainController
import com.gapps.utils.equalsOne
import com.gapps.utils.setRelativeWidth
import com.gapps.utils.whenNotNull
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
import javafx.scene.control.cell.PropertyValueFactory
import javafx.util.Callback
import kotlinx.coroutines.experimental.launch
import org.knowm.xchange.bitmex.dto.trade.*
import org.knowm.xchange.bitmex.dto.trade.BitmexExecutionInstruction.PARTICIPATE_DO_NOT_INITIATE
import org.knowm.xchange.bitmex.dto.trade.BitmexExecutionInstruction.REDUCE_ONLY
import org.knowm.xchange.exceptions.ExchangeException

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
    data class LinkedTableItem(private val position: SimpleIntegerProperty,
                               private val side: SimpleStringProperty,
                               private val price: SimpleDoubleProperty,
                               private val amount: SimpleDoubleProperty,
                               private val orderType: SimpleStringProperty,
                               private val orderTypeParameter: SimpleDoubleProperty,
                               private val linkId: SimpleStringProperty,
                               private val linkType: SimpleStringProperty,
                               private val postOnly: SimpleBooleanProperty,
                               private val reduceOnly: SimpleBooleanProperty) {

        constructor(position: Int,
                    side: String = "Buy",
                    price: Double = 0.0,
                    amount: Double = 1.0,
                    orderType: BulkOrderType = LIMIT,
                    orderTypeParameter: Double = 0.0,
                    linkId: String = "",
                    linkType: LinkType = LinkType.NONE,
                    postOnly: Boolean = false,
                    reduceOnly: Boolean = false) : this(
                SimpleIntegerProperty(position),
                SimpleStringProperty(side),
                SimpleDoubleProperty(price),
                SimpleDoubleProperty(amount),
                SimpleStringProperty(orderType.toString()),
                SimpleDoubleProperty(orderTypeParameter),
                SimpleStringProperty(linkId),
                SimpleStringProperty(linkType.toString()),
                SimpleBooleanProperty(postOnly),
                SimpleBooleanProperty(reduceOnly))

        fun createNew(position: Int): LinkedTableItem {
            return LinkedTableItem(
                    position,
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

        fun getPosition(): Int = position.get() + 1
        fun setPosition(pos: Int) = position.set(pos)
        fun getSide(): String = side.get()
        fun setSide(value: String) = side.set(value)
        fun getPrice(): Double = price.get()
        fun setPrice(value: Double) = price.set(value)
        fun getPriceProperty() = price
        fun getAmount(): Double = amount.get()
        fun setAmount(value: Double) = amount.set(value)
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
    private var exchange: XChangeWrapper? = null

    private var linkedOrders = FXCollections.observableArrayList<LinkedTableItem>()
    private val priceSpinners = ArrayList<SpinnerCell<LinkedTableItem>>()

    fun onSceneSet(controller: MainController) {
        this.controller = controller
        ExchangeHolder.addListener { exchange = it }

        initLinkedTable()
    }

    private fun initLinkedTable() {
        controller.apply {
            linkedPair.apply {
                items = FXCollections.observableArrayList(Constants.pairs)
                value = items[Constants.pairs.indexOf(Settings.settings.lastPair).let { if (it < 0) 0 else it }]
                setOnAction { _ ->
                    linkedOrders.clear()
                    val minStep = Constants.minimumPriceSteps[value.toString().toCurrencyPair()]!!
                    priceSpinners.forEach { it.step = minStep }
                    linkedPriceColumn.initSpinnerCellValueFactory(minStep)
                    linkedOrderTypeParameterColumn.initSpinnerCellValueFactory(minStep)
                    linkedOrders.addAll(elements = linkedOrders.asSequence().map { item ->
                        item.copy(
                                price = item.getPriceProperty()
                                        .also { if (it.value > 0) it.value = it.value - it.value % minStep else it.value },
                                orderTypeParameter = item.getOrderTypeParameterProperty()
                                        .also { if (it.value > 0) it.value = it.value - it.value % minStep else it.value })
                    })
                }
                enableValueChangeOnScroll()
            }
            linkedOrdersTable.apply {
                rowFactory = GreenRedRowFactory(this)
                items = linkedOrders
                selectionModel.isCellSelectionEnabled = false
                placeholder = Label("Add new orders with the '+' button or press the 'Move to Linked' button on the 'Bulk' page.")
                setOnMouseClicked { linkedOrdersTable.items = linkedOrders } // fixes strange content disappearing bug
            }
            linkedPositionColumn.apply {
                cellValueFactory = PropertyValueFactory<LinkedTableItem, Int>("position")
                setRelativeWidth(linkedOrdersTable, 28.861)
            }
            val minStep = Constants.minimumPriceSteps[linkedPair.value.toString().toCurrencyPair()]!!
            linkedSideColumn.apply {
                cellValueFactory = PropertyValueFactory<LinkedTableItem, String>("side")
                cellFactory = ComboBoxCell.forTableColumn("Buy", "Sell")
                setOnEditCommit { event ->
                    whenNotNull(event.tablePosition, event.newValue) { tablePosition, value ->
                        val row = tablePosition.row
                        if (row in 0..linkedOrders.lastIndex)
                            linkedOrders[row].setSide(value)
                    }
                    linkedOrdersTable.refresh()
                }
                setRelativeWidth(linkedOrdersTable, 12.9875)
            }

            linkedPriceColumn.apply {
                cellValueFactory = PropertyValueFactory<LinkedTableItem, Double>("price")
                initSpinnerCellValueFactory(minStep)
                setOnEditCommit { event ->
                    whenNotNull(event.tablePosition, event.newValue) { tablePosition, value ->
                        val row = tablePosition.row
                        if (row in 0..linkedOrders.lastIndex)
                            linkedOrders[row].setPrice(value)
                    }
                }
                setRelativeWidth(linkedOrdersTable, 6.926666)
            }

            linkedAmountColumn.apply {
                cellValueFactory = PropertyValueFactory<LinkedTableItem, Double>("amount")
                initSpinnerCellValueFactory(1.0, 1.0, Double.MAX_VALUE, 1.0)
                setOnEditCommit { event ->
                    whenNotNull(event.tablePosition, event.newValue) { tablePosition, value ->
                        val row = tablePosition.row
                        if (row in 0..linkedOrders.lastIndex)
                            linkedOrders[row].setAmount(value)
                    }
                }
                setRelativeWidth(linkedOrdersTable, 6.926666)
            }

            linkedOrderTypeColumn.apply {
                cellValueFactory = PropertyValueFactory<LinkedTableItem, String>("orderType")
                cellFactory = ComboBoxCell.forTableColumn(*values().map { it.toString() }.toTypedArray())
                setOnEditCommit { event ->
                    whenNotNull(event.tablePosition, event.newValue) { tablePosition, value ->
                        val row = tablePosition.row
                        if (row in 0..linkedOrders.lastIndex)
                            linkedOrders[row].setOrderType(value)
                    }
                }
                setRelativeWidth(linkedOrdersTable, 6.49375)
            }

            linkedOrderTypeParameterColumn.apply {
                cellValueFactory = PropertyValueFactory<LinkedTableItem, Double>("orderTypeParameter")
                initSpinnerCellValueFactory(minStep)
                setOnEditCommit { event ->
                    whenNotNull(event.tablePosition, event.newValue) { tablePosition, value ->
                        val row = tablePosition.row
                        if (row in 0..linkedOrders.lastIndex)
                            linkedOrders[row].setOrderTypeParameter(value)
                    }
                }
                setRelativeWidth(linkedOrdersTable, 6.926666)
            }

            linkedLinkIdColumn.apply {
                cellValueFactory = PropertyValueFactory<LinkedTableItem, String>("linkId")
                cellFactory = Callback<TableColumn<LinkedTableItem, String>, TableCell<LinkedTableItem, String>> { _ ->
                    EditCell()
                }
                setOnEditCommit { event ->
                    whenNotNull(event.tablePosition, event.newValue) { tablePosition, value ->
                        val row = tablePosition.row
                        if (row in 0..linkedOrders.lastIndex)
                            linkedOrders[row].setLinkId(value)
                    }
                }
                setRelativeWidth(linkedOrdersTable, 10.39)
            }

            linkedLinkTypeColumn.apply {
                cellValueFactory = PropertyValueFactory<LinkedTableItem, String>("linkType")
                cellFactory = ComboBoxCell.forTableColumn(*BitmexContingencyType.values().map { it.toString() }
                        .toMutableList().also { it.add(0, "NONE") }.toTypedArray())
                setOnEditCommit { event ->
                    whenNotNull(event.tablePosition, event.newValue) { tablePosition, value ->
                        val row = tablePosition.row
                        if (row in 0..linkedOrders.lastIndex)
                            linkedOrders[row].setLinkType(value)
                    }
                }
                setRelativeWidth(linkedOrdersTable, 7.99230769231)
            }

            linkedPostOnlyColumn.apply {
                setCellValueFactory { param -> param.value.getPostOnlyProperty() }
                cellFactory = CheckBoxTableCell.forTableColumn(linkedPostOnlyColumn)
                setOnEditCancel { event ->
                    whenNotNull(event.tablePosition, event.newValue) { tablePosition, value ->
                        val row = tablePosition.row
                        if (row in 0..linkedOrders.lastIndex)
                            linkedOrders[row].setPostOnly(value)
                    }
                }
                setRelativeWidth(linkedOrdersTable, 34.63333)
            }

            linkedReduceOnlyColumn.apply {
                setCellValueFactory { param -> param.value.getReduceOnlyProperty() }
                cellFactory = CheckBoxTableCell.forTableColumn(linkedReduceOnlyColumn)
                setOnEditCancel { event ->
                    whenNotNull(event.tablePosition, event.newValue) { tablePosition, value ->
                        val row = tablePosition.row
                        if (row in 0..linkedOrders.lastIndex)
                            linkedOrders[row].setReduceOnly(value)
                    }
                }
                setRelativeWidth(linkedOrdersTable, 34.63333)
            }
        }
    }

    private fun TableColumn<LinkedTableItem, Double>.initSpinnerCellValueFactory(step: Double, min: Double = 0.0, max: Double = Double.MAX_VALUE, initial: Double = min) {
        cellFactory = Callback<TableColumn<LinkedTableItem, Double>, TableCell<LinkedTableItem, Double>> {
            SpinnerCell<LinkedTableItem>(min, max, initial, step).also { cell ->
                priceSpinners.add(cell)
            }
        }
    }

    fun onAddLinkedOrderClicked() {
        if (linkedOrders.size <= 100) {
            val position = linkedOrders.lastOrNull()?.getPosition() ?: 0
            linkedOrders.add(linkedOrders.lastOrNull()?.createNew(position) ?: LinkedTableItem(position))
        } else {
            // TODO show Dialog
        }
    }

    fun onRemoveLinkedOrderClicked() {
        val row = controller.linkedOrdersTable.selectionModel.selectedCells.firstOrNull()?.row ?: linkedOrders.lastIndex
        if (row in 0..linkedOrders.size) {
            linkedOrders.removeAt(row)
            if (linkedOrders.isEmpty()) {
                priceSpinners.clear()
            }
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
                        .setExecutionInstructions(BitmexExecutionInstruction.Builder()
                                .setPostOnly(item.getPostOnly())
                                .setReduceOnly(item.getReduceOnly())
                                .setLastPrice(orderType.equalsOne(STOP, STOP_LIMIT, TRAILING_STOP)).build())
                        .setContingencyType(LinkType.valueOf(item.getLinkType()).toBitmexContingencyType())
                        .setClOrdLinkId(item.getLinkId().let { if (it.isBlank()) null else it })
                        .setPegPriceType(pegPriceType)
                        .setPegOffsetValue(pegPriceAmount?.toBigDecimal())
                        .build()
            }

            var error: ExchangeException? = null
            val result = try {
                exchange?.placeBulkOrders(orderParameters)
                        ?: null.also { AppDelegate.showError("Exchange not available.") }
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
        if (linkedOrders.size + orders.size <= 100) {
            val existingSize = linkedOrders.lastOrNull()?.getPosition() ?: 0
            orders.forEachIndexed { index, order ->
                linkedOrders.add(LinkedTableItem(
                        position = existingSize + index,
                        side = order.side?.capitalized!!,
                        price = order.price?.toDouble() ?: -1.0,
                        amount = order.orderQuantity?.toDouble() ?: 0.0,
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
        } else {
            // TODO show dialog
        }
    }

    fun onClearAllLinkedOrdersClicked() {
        linkedOrders.clear()
    }

    fun onDuplicateOrdersClicked() {
        linkedOrders.addAll(linkedOrders.map {
            LinkedTableItem(position = it.getPosition() + linkedOrders.lastIndex,
                    side = it.getSide(), price = it.getPrice(), amount = it.getAmount(), orderType = valueOf(it.getOrderType()),
                    orderTypeParameter = it.getOrderTypeParameter(), linkId = it.getLinkId(),
                    linkType = LinkType.valueOf(it.getLinkType()), postOnly = it.getPostOnly(), reduceOnly = it.getReduceOnly())
        })
    }
}