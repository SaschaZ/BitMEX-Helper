@file:Suppress("unused", "MemberVisibilityCanBePrivate", "EXPERIMENTAL_FEATURE_WARNING", "RedundantVisibilityModifier")

package com.gapps.bitmexhelper.kotlin

import com.gapps.bitmexhelper.kotlin.BulkDistribution.*
import com.gapps.bitmexhelper.kotlin.BulkOrderType.*
import com.gapps.bitmexhelper.kotlin.persistance.Constants
import com.gapps.bitmexhelper.kotlin.persistance.Constants.minimumPriceSteps
import com.gapps.utils.TimeUnit
import com.gapps.utils.catchAsync
import com.gapps.utils.launchInterval
import com.gapps.utils.toMs
import org.knowm.xchange.ExchangeFactory
import org.knowm.xchange.bitmex.Bitmex
import org.knowm.xchange.bitmex.BitmexExchange
import org.knowm.xchange.bitmex.BitmexPrompt
import org.knowm.xchange.bitmex.dto.marketdata.BitmexPrivateOrder
import org.knowm.xchange.bitmex.dto.trade.BitmexExecutionInstruction
import org.knowm.xchange.bitmex.dto.trade.BitmexOrderType
import org.knowm.xchange.bitmex.dto.trade.BitmexPlaceOrderParameters
import org.knowm.xchange.bitmex.dto.trade.BitmexSide.BUY
import org.knowm.xchange.bitmex.dto.trade.BitmexSide.SELL
import org.knowm.xchange.bitmex.service.BitmexMarketDataServiceRaw
import org.knowm.xchange.bitmex.service.BitmexTradeServiceRaw
import org.knowm.xchange.currency.Currency
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.Order
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
    public fun limitOrder(type: Order.OrderType,
                          pair: CurrencyPair,
                          amount: Double,
                          price: Double,
                          postOnly: Boolean,
                          reduceOnly: Boolean): String? = when (exchange) {
        is BitmexExchange ->
            (exchange.tradeService as BitmexTradeServiceRaw).placeOrder(BitmexPlaceOrderParameters
                    .Builder(pair.toBitmexSymbol())
                    .setSide(type.getSide())
                    .setOrderQuantity(amount.toBigDecimal())
                    .setPrice(price.toBigDecimal())
                    .setExecutionInstructions(BitmexExecutionInstruction.Builder().setPostOnly(postOnly)
                            .setReduceOnly(reduceOnly).build())
                    .build()).id
        else ->
            exchange.tradeService.placeLimitOrder(LimitOrder.Builder(type, pair)
                    .originalAmount(amount.toBigDecimal())
                    .limitPrice(price.toBigDecimal())
                    .build())
    }

    fun marketOrder(type: Order.OrderType,
                    pair: CurrencyPair,
                    amount: Double): String? = when (exchange) {
        is BitmexExchange ->
            (exchange.tradeService as BitmexTradeServiceRaw).placeOrder(BitmexPlaceOrderParameters
                    .Builder(pair.toBitmexSymbol())
                    .setSide(type.getSide())
                    .setOrderQuantity(amount.toBigDecimal())
                    .build()).id
        else ->
            exchange.tradeService.placeMarketOrder(MarketOrder.Builder(type, pair)
                    .originalAmount(amount.toBigDecimal())
                    .build())
    }

    fun stopOrder(type: Order.OrderType,
                  pair: CurrencyPair,
                  amount: Double,
                  stopPrice: Double,
                  reduceOnly: Boolean): String? = when (exchange) {
        is BitmexExchange ->
            (exchange.tradeService as BitmexTradeServiceRaw).placeOrder(BitmexPlaceOrderParameters
                    .Builder(pair.toBitmexSymbol())
                    .setSide(type.getSide())
                    .setOrderQuantity(amount.toBigDecimal())
                    .setStopPrice(stopPrice.toBigDecimal())
                    .setExecutionInstructions(BitmexExecutionInstruction.Builder().setPostOnly(false)
                            .setReduceOnly(reduceOnly).build())
                    .build()).id
        else ->
            exchange.tradeService.placeStopOrder(StopOrder.Builder(type, pair)
                    .originalAmount(amount.toBigDecimal())
                    .stopPrice(stopPrice.toBigDecimal())
                    .build())
    }

    fun placeBulkOrders(orders: List<BitmexPlaceOrderParameters>): List<BitmexPrivateOrder>? =
            (exchange.tradeService as BitmexTradeServiceRaw).placeOrderBulk(orders.map {
                Bitmex.PlaceOrderCommand(it)
            })

    fun placeBulkOrders(pair: CurrencyPair,
                        orderSide: Order.OrderType,
                        type: BulkOrderType,
                        amount: Int,
                        priceHigh: Double,
                        priceLow: Double,
                        distribution: BulkDistribution,
                        distributionParameter: Double,
                        minimumAmount: Double,
                        slDistance: Int,
                        postOnly: Boolean = false,
                        reduceOnly: Boolean = false,
                        reversed: Boolean = false): List<BitmexPrivateOrder>? {
        return when (exchange) {
            is BitmexExchange -> {
                val orders = createBulkOrders(pair, orderSide, type, amount, priceHigh, priceLow, distribution,
                        distributionParameter, minimumAmount, slDistance, postOnly, reduceOnly, reversed)
                placeBulkOrders(orders)
            }
            else -> {
                System.err.println("Can not execute bulk order for exchange ${exchange.javaClass.simpleName}.")
                null
            }
        }
    }

    fun createBulkOrders(pair: CurrencyPair,
                         orderSide: Order.OrderType,
                         type: BulkOrderType,
                         amount: Int,
                         priceHigh: Double,
                         priceLow: Double,
                         distribution: BulkDistribution,
                         distributionParameter: Double,
                         minimumAmount: Double,
                         slDistance: Int,
                         postOnly: Boolean = false,
                         reduceOnly: Boolean = false,
                         reversed: Boolean = false): List<BitmexPlaceOrderParameters> {
        var amounts = getBulkAmounts(amount, distribution, distributionParameter, minimumAmount)
        amounts = if (reversed) amounts.reversed() else amounts
        val orders = amounts.asSequence().mapIndexed { orderIndex, amountForOrder ->
            val priceForOrder = (if (amounts.size == 1) priceLow + (priceHigh - priceLow) / 2
            else priceLow + (priceHigh - priceLow) / (amounts.size - 1) * orderIndex).roundToMinimumStep(pair)
            val builder = BitmexPlaceOrderParameters.Builder(pair.toBitmexSymbol())
            when (type) {
                LIMIT -> {
                    builder.setPrice(priceForOrder.toBigDecimal())
                    builder.setOrderType(BitmexOrderType.LIMIT)
                }
                STOP -> {
                    builder.setStopPrice(priceForOrder.toBigDecimal())
                    builder.setOrderType(BitmexOrderType.STOP)
                }
                STOP_LIMIT -> {
                    builder.setPrice((priceForOrder + (minimumPriceSteps[pair]!! * slDistance * if (orderSide == BID) -1 else 1)).toBigDecimal())
                    builder.setStopPrice(priceForOrder.toBigDecimal())
                    builder.setOrderType(BitmexOrderType.STOP_LIMIT)
                }
                TRAILING_STOP -> {
                    builder.setOrderType(BitmexOrderType.PEGGED)
                    // Not used for automatic bulk creation
                }
            }

            builder
                    .setSide(orderSide.getSide())
                    .setOrderQuantity(amountForOrder.toBigDecimal())
                    .setExecutionInstructions(BitmexExecutionInstruction.Builder().setPostOnly(postOnly)
                            .setReduceOnly(reduceOnly).setLastPrice(type == TRAILING_STOP).build())
                    .build()
        }.toMutableList()

        val distinctOrders = ArrayList<BitmexPlaceOrderParameters>()
        orders.forEach { order ->
            distinctOrders.lastOrNull()?.also { last ->
                if (last.hasSamePrice(order))
                    distinctOrders[distinctOrders.lastIndex] = last.changeQuantity(order.orderQuantity
                            ?: order.simpleOrderQuantity!!)
                else
                    distinctOrders.add(order)
            } ?: distinctOrders.add(order)
        }
        return distinctOrders
    }

    private fun BitmexPlaceOrderParameters.changeQuantity(quantity: BigDecimal): BitmexPlaceOrderParameters {
        return BitmexPlaceOrderParameters(
                this.symbol,
                this.orderQuantity?.add(quantity),
                this.simpleOrderQuantity?.add(quantity),
                this.displayQuantity?.add(quantity),
                this.price,
                this.stopPrice,
                this.side,
                this.orderType,
                this.clOrdId,
                this.executionInstructions,
                this.clOrdLinkId,
                this.contingencyType,
                this.pegOffsetValue,
                this.pegPriceType,
                this.timeInForce,
                this.text)
    }

    private fun getBulkAmounts(amount: Int,
                               distribution: BulkDistribution,
                               distributionParameter: Double,
                               minimumAmount: Double): List<Int> {
        if (amount < minimumAmount)
            return emptyList()

        val maxOrderCount = 100
        var lastAmount: Int
        var totalAmount = 0

        return (0 until maxOrderCount).map loop@{
            if (distribution == SAME && it >= distributionParameter
                    || distribution != SAME && totalAmount >= amount) return@loop 0
            lastAmount = when (distribution) {
                FLAT -> max(amount.toDouble() / maxOrderCount, minimumAmount).toInt()
                DCA -> max(minimumAmount, totalAmount * distributionParameter).toInt()
                SAME -> amount
            }
            if (distribution != SAME && totalAmount + lastAmount > amount)
                0
            else {
                totalAmount += lastAmount
                lastAmount
            }
        }.filter { it > 0 && it < Integer.MAX_VALUE }.toMutableList().let { result ->
            val sum = result.sum()
            if (sum < amount && result.isNotEmpty()) {
                val amountToAdd = (amount - sum) / (result.size * 3)
                result.asSequence().map { it + amountToAdd }.toMutableList().also { increasedResult ->
                    val increasedSum = increasedResult.sum()
                    if (increasedSum < amount) {
                        increasedResult[increasedResult.lastIndex] += amount - increasedSum
                    }
                }
            } else
                result
        }
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
    BID -> BUY
    else -> SELL
}

internal fun BitmexPlaceOrderParameters.hasSamePrice(other: BitmexPlaceOrderParameters) = when {
    price != null && stopPrice != null && other.price != null && other.stopPrice != null -> price == other.price && stopPrice == other.stopPrice
    price != null && other.price != null -> price == other.price
    stopPrice != null && other.stopPrice != null -> stopPrice == other.stopPrice
    else -> false
}

enum class BulkOrderType {

    LIMIT,
    STOP,
    STOP_LIMIT,
    TRAILING_STOP;

    fun toBitmexOrderType() = when (this) {
        LIMIT -> BitmexOrderType.LIMIT
        STOP -> BitmexOrderType.STOP
        STOP_LIMIT -> BitmexOrderType.STOP_LIMIT
        TRAILING_STOP -> BitmexOrderType.PEGGED
    }
}

enum class BulkDistribution {

    FLAT,
    DCA,
    SAME
}