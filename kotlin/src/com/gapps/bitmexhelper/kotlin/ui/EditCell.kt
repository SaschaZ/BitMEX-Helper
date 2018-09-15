package com.gapps.bitmexhelper.kotlin.ui

import javafx.event.Event
import javafx.scene.control.*
import javafx.scene.control.TableColumn.CellEditEvent
import javafx.util.StringConverter


class EditCell<S, T>(
        private val converter: StringConverter<T>) : TableCell<S, T>() {

    private val textField = TextField()

    init {

        itemProperty().addListener { _, _, newItem ->
            if (newItem == null) {
                setText(null)
            } else {
                setText(converter.toString(newItem))
            }
        }
        graphic = textField
        contentDisplay = ContentDisplay.TEXT_ONLY

        textField.setOnAction { _ -> commitEdit() }
        textField.focusedProperty().addListener { _, _, isNowFocused ->
            if (!isNowFocused) commitEdit()
        }
    }

    override fun startEdit() {
        super.startEdit()
        textField.text = converter.toString(item)
        contentDisplay = ContentDisplay.GRAPHIC_ONLY
        textField.requestFocus()
    }

    override fun cancelEdit() {
        super.cancelEdit()
        contentDisplay = ContentDisplay.TEXT_ONLY
    }

    private fun commitEdit() = commitEdit(this.converter.fromString(textField.text))

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

    companion object {

        private val IDENTITY_CONVERTER: StringConverter<String> = object : StringConverter<String>() {

            override fun toString(`object`: String): String {
                return `object`
            }

            override fun fromString(string: String): String {
                return string
            }

        }

        @Suppress("unused")
        fun <S> createStringEditCell(): EditCell<S, String> {
            return EditCell(IDENTITY_CONVERTER)
        }
    }

}