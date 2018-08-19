package com.gapps.bitmexhelper.kotlin.persistance

import com.gapps.bitmexhelper.kotlin.toCurrencyPair

object Constants {

    const val title = "BitMEX Helper v1.0.8"

    const val settingsFilename = "bhSettings.json"

    // TODO get available pairs and their minimum steps from exchange
    val pairs = listOf(
            "XBT/USD",
            "ETH/USD",
            "ADA/U18",
            "BCH/U18",
            "EOS/U18",
            "LTC/U18",
            "TRX/U18",
            "XRP/U18"
    )

    val orderTypes = listOf(
            "Limit",
            "Stop",
            "Stop-Limit"
    )

    val sides = listOf(
            "BUY",
            "SELL"
    )

    val distributions = listOf(
            "FLAT",
            "MULT_MIN",
            "DIV_AMOUNT"
    )

    val minimumPriceSteps = mapOf(
            Pair(pairs[0].toCurrencyPair(), 0.5),
            Pair(pairs[1].toCurrencyPair(), 0.05),
            Pair(pairs[2].toCurrencyPair(), 0.00000001),
            Pair(pairs[3].toCurrencyPair(), 0.0001),
            Pair(pairs[4].toCurrencyPair(), 0.0000001),
            Pair(pairs[5].toCurrencyPair(), 0.00001),
            Pair(pairs[6].toCurrencyPair(), 0.00000001),
            Pair(pairs[7].toCurrencyPair(), 0.00000001)
    )
}