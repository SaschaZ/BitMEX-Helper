package com.gapps.bitmexhelper.kotlin.persistance

object Constants {

    const val title = "BitMEX Helper v1.0.3"
    const val settingsFilename = "bhSettings.json"
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
}