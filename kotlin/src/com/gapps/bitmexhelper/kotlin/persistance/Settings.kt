package com.gapps.bitmexhelper.kotlin.persistance

import com.gapps.bitmexhelper.kotlin.BulkDistribution
import com.gapps.utils.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder


data class Settings(private var bitmexApiKey: String = "",
                    private var bitmexSecretKey: String = "",
                    var lastSide: String = "BUY",
                    var lastOrderType: String = "Limit",
                    var lastPair: String = "XBT/USD",
                    var lastLowPrice: Double = 0.0,
                    var lastHighPrice: Double = 0.0,
                    var lastAmount: Int = 1,
                    var lastMinAmount: Int = 26,
                    var lastMode: String = BulkDistribution.FLAT.toString(),
                    var lastDistributionParameter: Double = 2.0,
                    var lastSlDistance: Int = 1,
                    var lastPostOnly: Boolean = true,
                    var lastReduceOnly: Boolean = false,
                    var lastReversed: Boolean = false) {

    companion object {

        var settings: Settings = Settings()
        private val file = JarLocation.fileInSameDir(Constants.settingsFilename)

        private val cipher = Cipher("WTrUCHj6bVn3jaRxEqx9SetrZpKDX7sNYCpdqjz8fUQPv6aSMjBGrtTJP75CFwKKw98QGHAS6Wg9a5cV92geRWY3MKR3A3vDRB3q")

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

        fun getBitmexApiKey() = cipher.decrypt(settings.bitmexApiKey)
        fun setBitmexApiKey(apiKey: String) {
            settings.bitmexApiKey = cipher.encrypt(apiKey)
        }

        fun getBitmexApiSecret() = cipher.decrypt(settings.bitmexSecretKey)
        fun setBitmexApiSecret(apiSecret: String) {
            settings.bitmexSecretKey = cipher.encrypt(apiSecret)
        }

        var hasCredentials = false
            get() = settings.bitmexApiKey.isNotBlank() && settings.bitmexSecretKey.isNotBlank()
    }
}