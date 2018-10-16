package com.gapps.bitmexhelper.kotlin.persistance

import com.gapps.bitmexhelper.kotlin.exchange.toCurrencyPair

object Constants {

    const val title = "BitMEX Helper v1.1.1"

    const val settingsFilename = "bhSettings.json"

    // TODO get available pairs and their minimum steps from exchange
    val pairs = listOf(
            "XBT/USD",
            "ETH/USD",
            "ETH/Z18",
            "ADA/Z18",
            "BCH/Z18",
            "EOS/Z18",
            "LTC/Z18",
            "TRX/Z18",
            "XRP/Z18"
    )

    val orderTypes = listOf(
            "Limit",
            "Stop",
            "Stop-Limit"
    )

    val sides = listOf(
            "Buy",
            "Sell"
    )

    val minimumPriceSteps = mapOf(
            Pair(pairs[0].toCurrencyPair(), 0.5.toBigDecimal()),
            Pair(pairs[1].toCurrencyPair(), 0.05.toBigDecimal()),
            Pair(pairs[2].toCurrencyPair(), 0.00001.toBigDecimal()),
            Pair(pairs[3].toCurrencyPair(), 0.00000001.toBigDecimal()),
            Pair(pairs[4].toCurrencyPair(), 0.0001.toBigDecimal()),
            Pair(pairs[5].toCurrencyPair(), 0.0000001.toBigDecimal()),
            Pair(pairs[6].toCurrencyPair(), 0.00001.toBigDecimal()),
            Pair(pairs[7].toCurrencyPair(), 0.00000001.toBigDecimal()),
            Pair(pairs[8].toCurrencyPair(), 0.00000001.toBigDecimal())
    )
}