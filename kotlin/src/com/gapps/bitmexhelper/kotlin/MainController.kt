package com.gapps.bitmexhelper.kotlin

import javafx.scene.control.*

interface MainController {

    val pair: ComboBox<*>
    val highPirce: Spinner<*>
    val lowPirce: Spinner<*>
    val amount: Spinner<*>
    val orderType: ComboBox<*>
    val side: ComboBox<*>
    val distribution: ComboBox<*>
    val parameter: Spinner<*>
    val minAmount: Spinner<*>
    val reversed: CheckBox
    val postOnly: CheckBox
    val reduceOnly: CheckBox
    val execute: Button
    val review: TableView<*>
    val stats: TextArea

    fun exitApp()
}
