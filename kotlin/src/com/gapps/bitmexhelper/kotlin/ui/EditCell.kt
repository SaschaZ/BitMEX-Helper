package com.gapps.bitmexhelper.kotlin.ui

import javafx.event.Event
import javafx.scene.control.*
import javafx.scene.control.TableColumn.CellEditEvent


class EditCell<S> : TableCell<S, String>() {

    private val textField = TextField()

    init {
        text = null
        graphic = textField
        contentDisplay = ContentDisplay.GRAPHIC_ONLY

        textField.textProperty().addListener { _, _, value ->
            commitEdit(value)
        }
    }

    override fun commitEdit(item: String?) {
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

    override fun updateItem(item: String?, empty: Boolean) {
        super.updateItem(item, empty)
        graphic = if (isEmpty) {
            null
        } else {
            textField.text = item
            textField
        }
    }
}