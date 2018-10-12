@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.gapps.bitmexhelper.kotlin.ui.delegates

import com.gapps.bitmexhelper.kotlin.ui.controller.MainController
import org.knowm.xchange.bitmex.BitmexException
import org.knowm.xchange.bitmex.dto.marketdata.BitmexPrivateOrder
import org.knowm.xchange.exceptions.ExchangeException


object MainDelegate {

    private lateinit var controller: MainController

    fun onControllerAvailable(controller: MainController) {
        MainDelegate.controller = controller
    }

    fun onSceneSet() {
        BulkDelegate.onSceneSet(controller)
        LinkedDelegate.onSceneSet(controller)
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
                        ?: "unknown error", (error?.cause as? BitmexException)?.errorName)
    }

    fun openSettings() = controller.tabPane.selectionModel.selectLast()
}