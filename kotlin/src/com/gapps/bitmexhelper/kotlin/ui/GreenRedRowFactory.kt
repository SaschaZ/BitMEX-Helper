package com.gapps.bitmexhelper.kotlin.ui

import com.gapps.bitmexhelper.kotlin.persistance.Constants
import com.gapps.bitmexhelper.kotlin.ui.delegates.LinkedDelegate.LinkedTableItem
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.util.Callback
import java.util.*


class GreenRedRowFactory(private val tableView: TableView<LinkedTableItem>)
    : Callback<TableView<LinkedTableItem>, TableRow<LinkedTableItem>> {

    companion object {

        private const val greenStyleClass = "green_row"
        private const val redStyleClass = "red_row"
    }

    private val baseRowFactory = tableView.rowFactory

    init {
        tableView.itemsProperty().addListener { _, _, _ -> tableView.rowFactory.call(tableView) }
    }

    override fun call(tableView: TableView<LinkedTableItem>): TableRow<LinkedTableItem> {
        val row = baseRowFactory?.call(tableView) ?: TableRow()
        row.indexProperty().addListener { _, _, _ -> updateStyleClass(row) }
        row.itemProperty().addListener { _, _, _ -> updateStyleClass(row) }
        return row
    }

    private fun updateStyleClass(row: TableRow<LinkedTableItem>) {
        println("updateStyleClass() item=${row.item}; rowIdx=${row.index}")
        val rowStyleClasses = row.styleClass
        when {
            row.item?.getSide() == Constants.sides[0] -> {
                if (!rowStyleClasses.contains(greenStyleClass)) {
                    println("green")
                    rowStyleClasses.removeAll(Collections.singletonList(redStyleClass))
                    rowStyleClasses.add(greenStyleClass)
                }
            }
            row.item?.getSide() == Constants.sides[1] -> {
                if (!rowStyleClasses.contains(redStyleClass)) {
                    println("red")
                    rowStyleClasses.removeAll(Collections.singletonList(greenStyleClass))
                    rowStyleClasses.add(redStyleClass)
                }
            }
            else -> {
                println("clear")
                rowStyleClasses.removeAll(Collections.singletonList(greenStyleClass))
                rowStyleClasses.removeAll(Collections.singletonList(redStyleClass))
            }
        }
    }
}