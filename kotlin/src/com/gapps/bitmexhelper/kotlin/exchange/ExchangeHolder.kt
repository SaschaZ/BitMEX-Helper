package com.gapps.bitmexhelper.kotlin.exchange

import com.gapps.bitmexhelper.kotlin.persistance.Settings
import org.knowm.xchange.bitmex.BitmexExchange

object ExchangeHolder {

    var exchange: XChangeWrapper? = null

    private val exchangeAvailableListeners = ArrayList<(XChangeWrapper) -> Unit>()

    init {
        Settings.apiCredentialsAvailableListener = { key, secret ->
            exchange = XChangeWrapper(BitmexExchange::class, key, secret)
            invokeListeners()
        }
    }

    fun addListener(listener: (XChangeWrapper) -> Unit) {
        exchangeAvailableListeners.add(listener)
        invokeListeners()
    }

    fun removeListener(listener: (XChangeWrapper) -> Unit) = exchangeAvailableListeners.remove(listener)

    private fun invokeListeners() = exchange?.also { xchange ->
        exchangeAvailableListeners.forEach { it.invoke(xchange) }
    }
}