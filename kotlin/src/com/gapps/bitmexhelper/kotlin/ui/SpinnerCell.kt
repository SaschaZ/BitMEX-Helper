package com.gapps.bitmexhelper.kotlin.ui

import com.gapps.bitmexhelper.kotlin.ui.delegates.enableSpinnerChangeOnScroll
import com.gapps.utils.asUnit
import com.gapps.utils.round
import javafx.event.Event
import javafx.scene.control.*
import javafx.scene.control.TableColumn.CellEditEvent


class SpinnerCell<S>(private val min: Double, private val max: Double, private val initial: Double, _step: Double) : TableCell<S, Double>() {

    private var converter = SmallDoubleStringConverter(_step)
    private val spinner = Spinner<Double>()

    var step = _step
        set(value) {
            spinner.valueFactory = SmallDoubleValueFactory(min, max, item ?: initial, value)
            converter = SmallDoubleStringConverter(value)
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

    override fun commitEdit(item: Double?) = item?.let {
        val rounded = it.round(step)
        spinner.editor.text = converter.toString(rounded)
        commitEditInternal(rounded)
    }.asUnit()

    private fun commitEditInternal(item: Double?) = item?.let {
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

    override fun updateItem(item: Double?, empty: Boolean) {
        super.updateItem(item, empty)
        graphic = if (isEmpty) {
            null
        } else {
            spinner.valueFactory.value = item
            spinner
        }
    }
}