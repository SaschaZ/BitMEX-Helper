package com.gapps.bitmexhelper.kotlin.ui

import javafx.event.Event
import javafx.scene.control.*
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import javafx.scene.control.TableColumn.CellEditEvent
import javafx.util.StringConverter
import javafx.util.converter.IntegerStringConverter


class SpinnerCell<S, T : Any>(private val min: T, private val max: T, private val initial: T, private var step: T) : TableCell<S, T>() {

    private lateinit var converter: StringConverter<T>
    private val spinner = Spinner<T>()

    init {
        itemProperty().addListener { _, _, newItem ->
            if (newItem == null) {
                setText(null)
            } else {
                setText(newItem.toString())
            }
        }

        item = initial
        setStep(step)

        spinner.isEditable = true
        graphic = spinner
        contentDisplay = ContentDisplay.TEXT_ONLY

        spinner.editor.setOnAction { _ -> commitEdit() }
        spinner.focusedProperty().addListener { _, _, isNowFocused ->
            if (!isNowFocused) commitEdit()
        }
        spinner.setOnScroll { event ->
            if (event.deltaY > 0)
                spinner.increment()
            else if (event.deltaY < 0)
                spinner.decrement()
        }
    }

    fun setStep(step: T) {
        println("setStep() step=$step")
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

    override fun startEdit() {
        super.startEdit()
        spinner.editor.text = converter.toString(item)
        contentDisplay = ContentDisplay.GRAPHIC_ONLY
        spinner.requestFocus()
    }

    override fun cancelEdit() {
        super.cancelEdit()
        contentDisplay = ContentDisplay.TEXT_ONLY
    }

    private fun commitEdit() = commitEdit(this.converter.fromString(spinner.editor.text.replace(",", ".")))

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

        contentDisplay = ContentDisplay.TEXT_ONLY
    }
}