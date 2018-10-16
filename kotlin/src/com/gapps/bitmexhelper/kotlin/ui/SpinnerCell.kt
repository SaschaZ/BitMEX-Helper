package com.gapps.bitmexhelper.kotlin.ui

import com.gapps.utils.asUnit
import javafx.event.Event
import javafx.scene.control.*
import javafx.scene.control.TableColumn.CellEditEvent
import java.math.BigDecimal


class SpinnerCell<S>(private val min: BigDecimal, private val max: BigDecimal, private val initial: BigDecimal, _step: BigDecimal) : TableCell<S, BigDecimal>() {

    private var converter = BigDecimalStringConverter(_step)
    private val spinner = Spinner<BigDecimal>()

    var step = _step
        set(value) {
            spinner.valueFactory = BigDecimalValueFactory(min, max, item ?: initial, value)
            converter = BigDecimalStringConverter(value)
            field = value
        }

    init {
        text = null
        item = initial
        step = _step

        spinner.apply {
            isEditable = true
            editor.textProperty().addListener { _, _, new ->
                if (new.isNotBlank() && new != "-" && new != ",")
                    commitEditInternal(converter.fromString(new))
            }
            valueProperty().addListener { _, _, value ->
                editor.text = converter.toString(value)
            }
            setOnScroll { event ->
                if (event.deltaY > 0)
                    increment()
                else if (event.deltaY < 0)
                    decrement()
            }
            focusedProperty().addListener { _, _, focused ->
                val new = editor.text
                if (!focused && new.isNotBlank() && new != "-" && new != ",")
                    commitEdit(converter.fromString(new))
            }
        }

        graphic = spinner
        contentDisplay = ContentDisplay.GRAPHIC_ONLY
    }

    override fun commitEdit(item: BigDecimal?) = item?.let {
        spinner.editor.text = converter.toString(item)
        commitEditInternal(item)
    }.asUnit()

    private fun commitEditInternal(item: BigDecimal?) = item?.let {
        if (!isEditing && it != getItem()) {
            val table = tableView
            if (table != null) {
                val column = tableColumn
                val event = CellEditEvent(table,
                        TablePosition(table, index, column),
                        TableColumn.editCommitEvent(), it)
                Event.fireEvent(column, event)
            }
        }

        super.commitEdit(it)
    }

    override fun updateItem(item: BigDecimal?, empty: Boolean) {
        super.updateItem(item, empty)
        graphic = if (isEmpty) {
            null
        } else {
            spinner.valueFactory.value = item
            spinner
        }
    }
}