@file:Suppress("unused", "MemberVisibilityCanBePrivate", "EXPERIMENTAL_FEATURE_WARNING")

package com.gapps.bitmexhelper.kotlin

import com.gapps.bitmexhelper.kotlin.XChangeWrapper.BulkDistribution.*
import com.gapps.bitmexhelper.kotlin.XChangeWrapper.OrderType.*
import com.gapps.bitmexhelper.kotlin.persistance.Constants
import com.gapps.utils.TimeUnit
import com.gapps.utils.catchAsync
import com.gapps.utils.launchInterval
import com.gapps.utils.toMs
import org.knowm.xchange.ExchangeFactory
import org.knowm.xchange.bitmex.Bitmex
import org.knowm.xchange.bitmex.BitmexExchange
import org.knowm.xchange.bitmex.BitmexPrompt
import org.knowm.xchange.bitmex.dto.marketdata.BitmexPrivateOrder
import org.knowm.xchange.bitmex.dto.trade.BitmexSide
import org.knowm.xchange.bitmex.service.BitmexMarketDataServiceRaw
import org.knowm.xchange.bitmex.service.BitmexTradeServiceRaw
import org.knowm.xchange.currency.Currency
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.Order
import org.knowm.xchange.dto.Order.OrderType.ASK
import org.knowm.xchange.dto.Order.OrderType.BID
import org.knowm.xchange.dto.account.AccountInfo
import org.knowm.xchange.dto.account.Wallet
import org.knowm.xchange.dto.marketdata.Ticker
import org.knowm.xchange.dto.trade.LimitOrder
import org.knowm.xchange.dto.trade.MarketOrder
import org.knowm.xchange.dto.trade.StopOrder
import java.math.BigDecimal
import java.math.MathContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.reflect.KClass

class XChangeWrapper(exchangeClass: KClass<*>, apiKey: String? = null, secretKey: String? = null) {

    companion object {

        const val BITMEX_CROSS_LEVERAGE = 0.0
    }

    init {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "ERROR")
    }

    private val exchange = ExchangeFactory.INSTANCE.createExchange(exchangeClass.java.name, apiKey, secretKey)


    /**
     * Constant
     */
    val name: String = exchange.exchangeSpecification.exchangeName

    val supportedSymbols: List<CurrencyPair> = exchange.exchangeSymbols

    /**
     * Account Info
     */
    private var accountInfo: AccountInfo? = null

    private fun getAccountInfo(forceRefresh: Boolean = true): AccountInfo {
        if (accountInfo == null || forceRefresh)
            accountInfo = exchange.accountService.accountInfo
        return accountInfo!!
    }

    private fun getWallet(forceRefresh: Boolean = true, walletId: String? = null): Wallet {
        val info = getAccountInfo(forceRefresh)
        if (info.wallets.size > 1)
            return walletId?.let { info.getWallet(it) } ?: info.wallets.toList().first().second
        return info.wallet
    }

    fun getAvailableAmount(currency: Currency, forceRefresh: Boolean = true): Double? =
            getWallet(forceRefresh).getBalance(currency)?.available?.toDouble()

    fun getTotalAmount(currency: Currency, forceRefresh: Boolean = true): Double? =
            getWallet(forceRefresh).getBalance(currency)?.total?.toDouble()

    /**
     * Ticker
     */
    suspend fun getTicker(pair: CurrencyPair): Ticker? = catchAsync(null) {
        exchange.marketDataService.getTicker(pair)
    }

    suspend fun getTickers(): Map<CurrencyPair, Ticker>? = catchAsync(null) {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
        when (exchange) {
            is BitmexExchange -> (exchange.marketDataService as BitmexMarketDataServiceRaw).activeTickers?.mapNotNull {
                Pair(it.symbol.toCurrencyPair(), Ticker.Builder()
                        .ask(it.askPrice)
                        .bid(it.bidPrice)
                        .currencyPair(it.symbol.toCurrencyPair())
                        .high(it.highPrice)
                        .last(it.lastPrice)
                        .low(it.lowPrice)
                        .open(it.openValue)
                        .timestamp(dateFormatter.parse(it.timestamp))
                        .volume(it.volume)
                        .vwap(it.vwap.toBigDecimal())
                        .build())
            }
            else -> exchange.marketDataService.getTickers(null)?.mapNotNull {
                Pair(it.currencyPair, it)
            }
        }?.toMap()
    }

    suspend fun pollTicker(pair: CurrencyPair,
                           updateInterval: Long = 1.toMs(TimeUnit.SECONDS),
                           listener: (Ticker) -> Unit) = launchInterval(updateInterval) {
        getTicker(pair)?.let { listener(it) }
    }

    /**
     * Orders
     */
    fun placeBuyLimitOrder(pair: CurrencyPair,
                           amount: Double,
                           price: Double,
                           postOnly: Boolean = false,
                           reduceOnly: Boolean = false) =
            limitOrder(BID, pair, amount, price, postOnly, reduceOnly)

    fun placeSellLimitOrder(pair: CurrencyPair,
                            amount: Double,
                            price: Double,
                            postOnly: Boolean = false,
                            reduceOnly: Boolean = false) =
            limitOrder(ASK, pair, amount, price, postOnly, reduceOnly)

    private fun limitOrder(type: Order.OrderType,
                           pair: CurrencyPair,
                           amount: Double,
                           price: Double,
                           postOnly: Boolean,
                           reduceOnly: Boolean): String? = when (exchange) {
        is BitmexExchange -> {
            val execInstructions = createBitmexExecInstructions(postOnly, reduceOnly)

            (exchange.tradeService as BitmexTradeServiceRaw).placeLimitOrder(pair.toBitmexSymbol(),
                    amount.toBigDecimal(), price.toBigDecimal(), type.getSide(), null,
                    execInstructions.joinToString(","), null, null).id
        }
        else -> {
            exchange.tradeService.placeLimitOrder(LimitOrder.Builder(type, pair)
                    .originalAmount(amount.toBigDecimal())
                    .limitPrice(price.toBigDecimal())
                    .build())

        }
    }

    fun placeBuyMarketOrder(pair: CurrencyPair,
                            amount: Double,
                            reduceOnly: Boolean = false) =
            marketOrder(BID, pair, amount, reduceOnly)

    fun placeSellMarketOrder(pair: CurrencyPair,
                             amount: Double,
                             reduceOnly: Boolean = false) =
            marketOrder(ASK, pair, amount, reduceOnly)

    private fun marketOrder(type: Order.OrderType,
                            pair: CurrencyPair,
                            amount: Double,
                            reduceOnly: Boolean): String? = when (exchange) {
        is BitmexExchange -> {
            val execInstructions = createBitmexExecInstructions(false, reduceOnly)

            (exchange.tradeService as BitmexTradeServiceRaw).placeMarketOrder(pair.toBitmexSymbol(),
                    type.getSide(), amount.toBigDecimal(), execInstructions.joinToString(",")).id
        }
        else -> {
            exchange.tradeService.placeMarketOrder(MarketOrder.Builder(type, pair)
                    .originalAmount(amount.toBigDecimal())
                    .build())
        }
    }

    fun placeBuyStopOrder(pair: CurrencyPair,
                          amount: Double,
                          stopPrice: Double,
                          reduceOnly: Boolean = false) =
            stopOrder(BID, pair, amount, stopPrice, reduceOnly)

    fun placeSellStopOrder(pair: CurrencyPair,
                           amount: Double,
                           stopPrice: Double,
                           reduceOnly: Boolean = false) =
            stopOrder(ASK, pair, amount, stopPrice, reduceOnly)

    private fun stopOrder(type: Order.OrderType,
                          pair: CurrencyPair,
                          amount: Double,
                          stopPrice: Double,
                          reduceOnly: Boolean): String? = when (exchange) {
        is BitmexExchange -> {
            val execInstructions = createBitmexExecInstructions(false, reduceOnly)

            (exchange.tradeService as BitmexTradeServiceRaw).placeStopOrder(pair.toBitmexSymbol(),
                    type.getSide(), amount.toBigDecimal(), stopPrice.toBigDecimal(),
                    execInstructions.joinToString(","), null, null, null).id
        }
        else -> {
            exchange.tradeService.placeStopOrder(StopOrder.Builder(type, pair)
                    .originalAmount(amount.toBigDecimal())
                    .stopPrice(stopPrice.toBigDecimal())
                    .build())
        }
    }

    enum class OrderType {

        LIMIT,
        STOP,
        STOP_LIMIT
    }

    enum class OrderLinkType {

        OCO,
        OTO,
        OUOA,
        OUOP,
        NONE;

        fun toParameter() = when(this) {
            XChangeWrapper.OrderLinkType.OCO -> "OneCancelsTheOther"
            XChangeWrapper.OrderLinkType.OTO -> "OneTriggersTheOther"
            XChangeWrapper.OrderLinkType.OUOA -> "OneUpdatesTheOtherAbsolute"
            XChangeWrapper.OrderLinkType.OUOP -> "OneUpdatesTheOtherProportional"
            XChangeWrapper.OrderLinkType.NONE -> ""
        }
    }

    enum class BulkDistribution {

        FLAT,
        MULT_MIN,
        DIV_AMOUNT
    }

    fun placeBulkOrders(pair: CurrencyPair,
                        side: Order.OrderType,
                        type: OrderType,
                        amount: Double,
                        minimumAmount: Double,
                        priceLow: Double,
                        priceHigh: Double,
                        distribution: BulkDistribution,
                        distributionParameter: Double,
                        postOnly: Boolean,
                        reduceOnly: Boolean,
                        reversed: Boolean): List<Order>? {
        return when (exchange) {
            is BitmexExchange -> {
                val orders = createBulkOrders(amount, distribution, distributionParameter, minimumAmount,
                        postOnly, reduceOnly, priceLow, priceHigh, pair, side, reversed)

                (exchange.tradeService as BitmexTradeServiceRaw).placeLimitOrderBulk(orders.map {
                    val price = when (type) {
                        STOP -> null
                        STOP_LIMIT -> it.price + (Constants.minimumPriceSteps[pair]!! * if (side == BID) 1 else -1)
                        else -> it.price
                    }?.toBigDecimal()
                    val stop = when (type) {
                        STOP,
                        STOP_LIMIT -> it.price
                        else -> null
                    }?.toBigDecimal()

                    Bitmex.PlaceOrderCommand(it.symbol, it.side, it.orderQuantity, price, stop, when (type) {
                        LIMIT -> "Limit"
                        STOP -> "Stop"
                        else -> "StopLimit"
                    }, it.clOrId, it.executionInstructions, it.clOrdLinkID, it.linkedType?.toParameter())
                }).map {
                    LimitOrder.Builder(side, pair)
                            .limitPrice(it.price)
                            .averagePrice(it.avgPx)
                            .cumulativeAmount(it.cumQty)
                            .id(it.id)
                            .orderStatus(when (it.orderStatus!!) {
                                BitmexPrivateOrder.OrderStatus.New -> Order.OrderStatus.NEW
                                BitmexPrivateOrder.OrderStatus.Filled -> Order.OrderStatus.FILLED
                                BitmexPrivateOrder.OrderStatus.Canceled -> Order.OrderStatus.CANCELED
                                BitmexPrivateOrder.OrderStatus.PartiallyFilled -> Order.OrderStatus.PARTIALLY_FILLED
                                BitmexPrivateOrder.OrderStatus.Rejected -> Order.OrderStatus.REJECTED
                                BitmexPrivateOrder.OrderStatus.Replaced -> Order.OrderStatus.REPLACED
                            })
                            .timestamp(it.timestamp)
                            .build()
                }
            }
            else -> {
                System.err.println("Can not execute bulk order for exchange ${exchange.javaClass.simpleName}.")
                null
            }
        }
    }

    data class BulkOrder(val symbol: String,
                         val side: String?,
                         var orderQuantity: Int,
                         val price: Double,
                         val executionInstructions: String? = null,
                         val clOrId: String? = null,
                         val clOrdLinkID: String? = null,
                         val linkedType: OrderLinkType? = null)

    fun createBulkOrders(amount: Double, distribution: BulkDistribution, distributionParameter: Double,
                         minimumAmount: Double, postOnly: Boolean, reduceOnly: Boolean, priceLow: Double, priceHigh: Double,
                         pair: CurrencyPair, side: Order.OrderType, reversed: Boolean): List<BulkOrder> {
        var amounts = getBulkAmounts(amount, distribution, distributionParameter, minimumAmount)
        amounts = if (reversed) amounts.reversed() else amounts
        val execInstructions = createBitmexExecInstructions(postOnly, reduceOnly)
        val orders = amounts.mapIndexed { orderIndex, amountForOrder ->
            val priceForOrder = (priceLow + (priceHigh - priceLow) / (amounts.size - 1) * orderIndex).roundToMinimumStep(pair)

            BulkOrder(pair.toBitmexSymbol(), side.getSide().capitalized,
                    amountForOrder, priceForOrder, null, execInstructions.joinToString(","))
        }.toMutableList()

        val distinctOrders = ArrayList<BulkOrder>()
        orders.forEach { order ->
            distinctOrders.lastOrNull()?.let { last ->
                if (last.price == order.price)
                    last.orderQuantity += order.orderQuantity
                else
                    distinctOrders.add(order)
            } ?: distinctOrders.add(order)
        }
        return distinctOrders
    }

    private fun getBulkAmounts(amount: Double,
                               distribution: BulkDistribution,
                               distributionParameter: Double,
                               minimumAmount: Double): List<Int> {
        if (amount < minimumAmount)
            return emptyList()

        val maxOrderCount = 100
        var lastAmount = 0
        var totalAmount = 0

        return (0 until maxOrderCount).map loop@{
            if (totalAmount >= amount) return@loop 0
            when (distribution) {
                FLAT -> {
                    lastAmount = max(amount / maxOrderCount, minimumAmount).toInt()
                    if (totalAmount + lastAmount > amount)
                        0
                    else {
                        totalAmount += lastAmount
                        lastAmount
                    }
                }
                DIV_AMOUNT -> {
                    lastAmount = ((if (totalAmount == 0) amount.toInt() else lastAmount) / distributionParameter).toInt()
                    if (totalAmount + lastAmount > amount || lastAmount < minimumAmount)
                        0
                    else {
                        totalAmount += lastAmount
                        lastAmount
                    }
                }
                MULT_MIN -> {
                    lastAmount = max(minimumAmount, lastAmount * distributionParameter).toInt()
                    if (totalAmount + lastAmount > amount)
                        0
                    else {
                        totalAmount += lastAmount
                        lastAmount
                    }
                }
            }
        }.filter { it > 0 && it < Integer.MAX_VALUE }
    }

    private fun createBitmexExecInstructions(postOnly: Boolean, reduceOnly: Boolean): ArrayList<String> {
        val execInstructions = ArrayList<String>()
        if (postOnly)
            execInstructions.add("ParticipateDoNotInitiate")
        if (reduceOnly)
            execInstructions.add("ReduceOnly")
        return execInstructions
    }

    fun updateLeverage(pair: CurrencyPair, leverage: Double) {
        when (exchange) {
            is BitmexExchange -> (exchange.tradeService as BitmexTradeServiceRaw)
                    .updateLeveragePosition(pair.toBitmexSymbol(), leverage.toBigDecimal())
            else -> System.err.println("Can not set leverage for exchange ${exchange.javaClass.simpleName}.")
        }
    }

    @Suppress("EnumEntryName")
    enum class CandleInterval {
        m1,
        m3,
        m5,
        m10,
        m15,
        m30,
        m45,
        h1,
        h2,
        h3,
        h4,
        h6,
        h8,
        h12,
        d1,
        d2,
        d3,
        w1,
        M1
    }

    data class Candle(val low: Double, val high: Double, val open: Double, val close: Double, val volume: Double, val timestamp: Long)

    fun getCandles(pair: CurrencyPair, interval: CandleInterval): List<Candle>? {
        return when (exchange) {
            is BitmexExchange -> {
                interval.getBitmexBinSize()?.let { binSize ->
                    val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
                    (exchange.marketDataService as BitmexMarketDataServiceRaw).getBucketedTrades(binSize,
                            false, pair, BitmexPrompt.PERPETUAL, 100, false).map {
                        Candle(it.low.toDouble(), it.high.toDouble(), it.open.toDouble(), it.close.toDouble(), it.volume.toDouble(),
                                dateFormatter.parse(it.timestamp).time)
                    }
                }
            }
            else -> null
        }
    }
}

private fun XChangeWrapper.CandleInterval.getBitmexBinSize() = when (this) {
    XChangeWrapper.CandleInterval.m1 -> "1m"
    XChangeWrapper.CandleInterval.m5 -> "5m"
    XChangeWrapper.CandleInterval.h1 -> "1h"
    XChangeWrapper.CandleInterval.d1 -> "1d"
    else -> null
}

fun Double.roundToMinimumStep(pair: CurrencyPair) =
        BigDecimal(this - this % Constants.minimumPriceSteps[pair]!!).round(MathContext(8)).toDouble()

fun String.toCurrencyPair(): CurrencyPair {
    val counterStartIndex = length - when {
        endsWith("USDT", true) -> 4
        else -> 3
    }

    val baseEndIndex = if (!get(counterStartIndex - 1).isLetter()) counterStartIndex - 2
    else counterStartIndex - 1

    val base = substring(0, baseEndIndex + 1)
    val counter = substring(counterStartIndex, length)

    return CurrencyPair(base, counter)
}

internal fun CurrencyPair.toBitmexSymbol() = "${base.currencyCode}${counter.currencyCode}"

internal fun Order.OrderType.getSide() = when (this) {
    BID -> BitmexSide.BUY
    else -> BitmexSide.SELL
}