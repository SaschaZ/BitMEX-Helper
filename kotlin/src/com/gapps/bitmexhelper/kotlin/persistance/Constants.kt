package com.gapps.bitmexhelper.kotlin.persistance

object Constants {

    const val title = "BitMEX Helper"
    val pairs = listOf(
            "XBT/BTC",
            "ETH/BTC",
            "ADA/BTC",
            "BCH/BTC",
            "EOS/BTC",
            "LTC/BTC",
            "TRX/BTC",
            "XRP/BTC")
    val orderTypes = listOf("Limit", "Stop", "Stop-Limit")
    val sides = listOf("BUY", "SELL")
    val distributions = listOf("FLAT", "MIN_MULT", "AMOUNT_DIV")
}