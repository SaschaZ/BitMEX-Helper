package com.gapps.bitmexhelper.kotlin.ui

import javafx.collections.FXCollections
import javafx.event.Event
import javafx.scene.control.*
import javafx.util.Callback
import javafx.util.StringConverter
import com.gapps.bitmexhelper.kotlin.ui.delegates.enableValueChangeOnScroll

class ComboBoxCell<S>(items: List<String>) : TableCell<S, String>() {

    companion object {

        fun <S> forTableColumn(vararg items: String): Callback<TableColumn<S, String>, TableCell<S, String>> =
                Callback { ComboBoxCell(items.toList()) }
    }

    private val comboBox: ComboBox<String>

    init {
        comboBox = ComboBox<String>().apply {
            converter = object : StringConverter<String>() {
                override fun toString(p0: String?) = p0
                override fun fromString(p0: String?) = p0
            }
            selectionModel.selectedItemProperty().addListener { _, _, value ->
                commitEdit(value)
            }
            this.items = FXCollections.observableArrayList(items)
            selectionModel.select(items.first())
            enableValueChangeOnScroll()
        }
        graphic = comboBox
        text = null
        contentDisplay = ContentDisplay.GRAPHIC_ONLY
    }

    override fun commitEdit(item: String?) {
        // This block is necessary to support commit on losing focus, because the baked-in mechanism
        // sets our editing state to false before we can intercept the loss of focus.
        // The default commitEdit(...) method simply bails if we are not editing...
        if (!isEditing && item != getItem()) {
            val table = tableView
            if (table != null) {
                val column = tableColumn
                val event = TableColumn.CellEditEvent(table,
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
            comboBox.selectionModel.select(getItem())
            comboBox
        }
    }
}