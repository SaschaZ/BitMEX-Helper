@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.gapps.bitmexhelper.kotlin.ui.delegates

import com.gapps.bitmexhelper.kotlin.*
import com.gapps.bitmexhelper.kotlin.persistance.Settings
import com.gapps.bitmexhelper.kotlin.ui.controller.MainController
import org.knowm.xchange.bitmex.BitmexExchange
import org.knowm.xchange.bitmex.dto.marketdata.BitmexPrivateOrder
import org.knowm.xchange.exceptions.ExchangeException


object MainDelegate {

    private lateinit var controller: MainController
    private lateinit var exchange: XChangeWrapper

    fun onControllerAvailable(controller: MainController) {
        MainDelegate.controller = controller
        exchange = XChangeWrapper(BitmexExchange::class, Settings.getBitmexApiKey(), Settings.getBitmexApiSecret())
    }

    fun onSceneSet() {
        BulkDelegate.onSceneSet(controller, exchange)
        LinkedDelegate.onSceneSet(controller, exchange)
        SettingsDelegate.onSceneSet(controller)
    }

    fun onMoveToLinkedClicked() {
        BulkDelegate.createOrders()?.let { LinkedDelegate.addOrders(it) }
        controller.apply { linkedPair.value = pair.value }
    }

    fun reportCanceledOrders(result: List<BitmexPrivateOrder>) {
        println(result.joinToString("\n"))
        val canceled = result.filter { it.orderStatus == BitmexPrivateOrder.OrderStatus.Canceled }
        if (canceled.isNotEmpty())
            AppDelegate.showError("${canceled.size} of ${result.count()} " +
                    "${if (canceled.size == 1) "order was" else "orders were"} canceled!\n" +
                    canceled.joinToString("\n") { item ->
                        "${item.volume}@${item.price ?: ""}${item.stopPx?.let { stop ->
                            "${item.price?.let { "/" } ?: ""}$stop"
                        } ?: ""}: ${item.text}"
                    })
    }

    fun reportError(error: ExchangeException?) {
        error?.printStackTrace()
        AppDelegate.showError(error?.localizedMessage
                ?: error?.message
                ?: "unknown error")
    }

    fun openSettings() = controller.tabPane.selectionModel.selectLast()
}