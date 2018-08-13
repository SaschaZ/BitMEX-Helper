package com.gapps.bitmexhelper.kotlin

import com.gapps.bitmexhelper.kotlin.persistance.Constants
import com.gapps.bitmexhelper.kotlin.persistance.Settings
import javafx.collections.FXCollections


object MainUiDelegate {

    private lateinit var controller: MainController
    private val settings = Settings.let {
        it.load()
        it.settings
    }

    fun onControllerAvailable(controller: MainController) {
        println("onControllerAvailable")
        this.controller = controller
    }

    fun onSceneSet() {
        println("onSceneSet")

        controller.apply {
            pair.apply {
                items = FXCollections.observableArrayList(Constants.pairs)
                value = items[Constants.pairs.indexOf(settings.lastPair)]
            }
            highPirce.valueFactory.value = settings.lastHighPrice
            lowPirce.valueFactory.value = settings.lastLowPrice
            amount.valueFactory.value = settings.lastAmount.toDouble()
            orderType.apply {
                items = FXCollections.observableArrayList(Constants.orderTypes)
                value = items[Constants.orderTypes.indexOf(settings.lastOrderType)]
            }
            side.apply {
                items = FXCollections.observableArrayList(Constants.sides)
                value = items[Constants.sides.indexOf(settings.lastSide)]
            }
            distribution.apply {
                items = FXCollections.observableArrayList(Constants.distributions)
                value = items[Constants.distributions.indexOf(settings.lastDistributionType)]
            }
            parameter.valueFactory.value = settings.lastDistributionParameter
            minAmount.valueFactory.value = settings.lastMinAmount.toDouble()
            reversed.isSelected = settings.lastReversed
            postOnly.isSelected = settings.lastPostOnly
            reduceOnly.isSelected = settings.lastReduceOnly
            execute.setOnMouseClicked { onExecuteClicked() }
        }
    }

    private fun onExecuteClicked() {
        println("onExecuteClicked")

        storeSelection()

    }

    private fun storeSelection() {
        controller.apply {
            settings.apply {
                lastPair = pair.value.toString()
                lastHighPrice = highPirce.value as Double
                lastLowPrice = lowPirce.value as Double
                lastAmount = (amount.value as Double).toInt()
                lastOrderType = orderType.value.toString()
                lastSide = side.value.toString()
                lastDistributionType = distribution.value.toString()
                lastDistributionParameter = parameter.value as Double
                lastMinAmount = (minAmount.value as Double).toInt()
                lastReversed = reversed.isSelected
                lastPostOnly = postOnly.isSelected
                lastReduceOnly = reduceOnly.isSelected
            }
            Settings.store()
        }
    }

    fun onAboutClicked() {
        println("onAboutClicked")
    }

    fun onSettingsClicked() {
        println("onSettingsClicked")
    }

    fun onQuitClicked() {
        println("onQuitClicked")
        controller.exitApp()
    }
}