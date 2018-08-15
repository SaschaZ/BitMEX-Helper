package com.gapps.bitmexhelper.kotlin.persistance

object Constants {

    const val title = "BitMEX Helper v1.0.3"
    val pairs = listOf(
            "XBT/USD",
            "ETH/BTC",
            "ADA/BTC",
            "BCH/BTC",
            "EOS/BTC",
            "LTC/BTC",
            "TRX/BTC",
            "XRP/BTC"
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
}