package com.gapps.bitmexhelper.kotlin.persistance

import com.gapps.bitmexhelper.kotlin.XChangeWrapper
import com.gapps.utils.catch
import com.gapps.utils.readString
import com.gapps.utils.writeString
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

data class Settings(var bitmexApiKey: String = "",
                    var bitmexSecretKey: String = "",
                    var lastSide: String = "BUY",
                    var lastOrderType: String = "Limit",
                    var lastPair: String = "XBT/BTC",
                    var lastLowPrice: Double = 7000.0,
                    var lastHighPrice: Double = 8000.0,
                    var lastAmount: Int = 1000,
                    var lastMinAmount: Int = 20,
                    var lastMode: String = XChangeWrapper.BulkDistribution.FLAT.toString(),
                    var lastDistributionParameter: Double = 2.0,
                    var lastPostOnly: Boolean = true,
                    var lastReduceOnly: Boolean = false,
                    var lastDistributionType: String = "FLAT",
                    var lastReversed: Boolean = false) {

    companion object {

        var settings: Settings = Settings()
        private val file = File("settings.json")

        fun load(): Boolean {
            return catch(false) {
                if (!file.exists()) store()
                else settings = Gson().fromJson(file.readString(), Settings::class.java)

                hasCredentials
            }
        }

        fun store() = catch(Unit) {
            file.writeString(GsonBuilder().setPrettyPrinting().create().toJson(settings))
        }


        var hasCredentials = false
            get() = settings.bitmexApiKey.isNotBlank() && settings.bitmexSecretKey.isNotBlank()
    }
}