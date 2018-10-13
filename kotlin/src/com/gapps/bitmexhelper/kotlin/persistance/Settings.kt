package com.gapps.bitmexhelper.kotlin.persistance

import com.gapps.bitmexhelper.kotlin.exchange.BulkDistribution
import com.gapps.utils.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.math.BigDecimal


data class Settings(private var bitmexApiKey: String = "",
                    private var bitmexSecretKey: String = "",
                    var lastSide: String = "Buy",
                    var lastOrderType: String = "Limit",
                    var lastPair: String = "XBT/USD",
                    var lastLowPrice: BigDecimal = 0.0.toBigDecimal(),
                    var lastHighPrice: BigDecimal = 0.0.toBigDecimal(),
                    var lastAmount: Int = 1,
                    var lastMinAmount: Int = 26,
                    var lastMode: String = BulkDistribution.FLAT.toString(),
                    var lastDistributionParameter: BigDecimal = 2.0.toBigDecimal(),
                    var lastSlDistance: Int = 1,
                    var lastPostOnly: Boolean = true,
                    var lastReduceOnly: Boolean = false,
                    var lastReversed: Boolean = false) {

    companion object {

        var settings: Settings = Settings()
        var apiCredentialsAvailableListener: ((String, String) -> Unit)? = null
            set(value) {
                field = value
                checkCredentialsAvailable()
            }

        private val file = JarLocation.fileInSameDir(Constants.settingsFilename)

        private val cipher = Cipher("WTrUCHj6bVn3jaRxEqx9SetrZpKDX7sNYCpdqjz8fUQPv6aSMjBGrtTJP75CFwKKw98QGHAS6Wg9a5cV92geRWY3MKR3A3vDRB3q")

        fun load() = catch(Unit) {
            if (!file.exists()) store()
            else settings = Gson().fromJson(file.readString(), Settings::class.java)
            checkCredentialsAvailable()
        }

        fun store() = catch(Unit) {
            file.writeString(GsonBuilder().setPrettyPrinting().create().toJson(settings))
            checkCredentialsAvailable()
        }

        private fun checkCredentialsAvailable() = whenNotNull(getBitmexApiKey(), getBitmexApiSecret()) { key, secret ->
            if (hasCredentials)
                apiCredentialsAvailableListener?.invoke(key, secret)
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