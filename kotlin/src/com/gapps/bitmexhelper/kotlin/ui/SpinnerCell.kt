package com.gapps.bitmexhelper.kotlin.ui

import javafx.event.Event
import javafx.scene.control.*
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import javafx.scene.control.TableColumn.CellEditEvent
import javafx.util.StringConverter
import javafx.util.converter.IntegerStringConverter
import com.gapps.bitmexhelper.kotlin.ui.delegates.enableBetterListener
import com.gapps.bitmexhelper.kotlin.ui.delegates.enableSpinnerChangeOnScroll


class SpinnerCell<S, T : Any>(private val min: T, private val max: T, private val initial: T, private var step: T) : TableCell<S, T>() {

    private lateinit var converter: StringConverter<T>
    private val spinner = Spinner<T>()

    init {
        text = null
        item = initial
        setStep(step)

        spinner.isEditable = true
        spinner.enableBetterListener()
        spinner.enableSpinnerChangeOnScroll()
        spinner.valueProperty().addListener { _, _, value ->
            commitEdit(value)
        }
        spinner.setOnScroll { event ->
            if (event.deltaY > 0)
                spinner.increment()
            else if (event.deltaY < 0)
                spinner.decrement()
        }

        graphic = spinner
        contentDisplay = ContentDisplay.GRAPHIC_ONLY
    }

    fun setStep(step: T) {
        this.step = step
        @Suppress("UNCHECKED_CAST")
        spinner.valueFactory = when (initial) {
            is Double -> SmallDoubleValueFactory(min as Double, max as Double, item as? Double
                    ?: initial, step as Double)
            is Int -> IntegerSpinnerValueFactory(min as Int, max as Int, item as? Int ?: initial, step as Int)
            else -> throw IllegalArgumentException("Unknown generic parameter T: ${initial.javaClass}")
        } as SpinnerValueFactory<T>

        @Suppress("UNCHECKED_CAST")
        converter = when (initial) {
            is Double -> SmallDoubleStringConverter()
            is Int -> IntegerStringConverter()
            else -> throw IllegalArgumentException("Unknown generic parameter T: ${initial.javaClass}")
        } as StringConverter<T>
    }

    override fun commitEdit(item: T?) {
        // This block is necessary to support commit on losing focus, because the baked-in mechanism
        // sets our editing state to false before we can intercept the loss of focus.
        // The default commitEdit(...) method simply bails if we are not editing...
        if (!isEditing && item != getItem()) {
            val table = tableView
            if (table != null) {
                val column = tableColumn
                val event = CellEditEvent(table,
                        TablePosition(table, index, column),
                        TableColumn.editCommitEvent(), item)
                Event.fireEvent(column, event)
            }
        }

        super.commitEdit(item)
    }

    override fun updateItem(item: T?, empty: Boolean) {
        super.updateItem(item, empty)
        graphic = if (isEmpty) {
            null
        } else {
            spinner.editor.text = item.toString()
            spinner
        }
    }
}