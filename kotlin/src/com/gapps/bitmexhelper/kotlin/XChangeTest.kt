@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.gapps.bitmexhelper.kotlin

import com.gapps.bitmexhelper.kotlin.persistance.Constants
import kotlinx.coroutines.experimental.runBlocking
import org.knowm.xchange.bitmex.BitmexExchange
import org.knowm.xchange.bitmex.dto.trade.BitmexContingencyType
import org.knowm.xchange.bitmex.dto.trade.BitmexExecutionInstruction
import org.knowm.xchange.bitmex.dto.trade.BitmexPlaceOrderParameters
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.Order
import org.knowm.xchange.dto.Order.OrderType.*

class XChangeTest {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) = runBlocking {
            val exchange = XChangeWrapper(BitmexExchange::class, "8W_TtdgciHy_bpUsEZqF3iVS",
                    "fDVhHDAsuOwNA-mt8yZYXgMBXE-04UxdTgNGCwawu5havZ7V")

//            createStopLimitRange(exchange, CurrencyPair("ADA", "U18"), BID,
//                    0.00001107, 0.00001098, 8000)
//            createStopLimitRange(exchange, CurrencyPair("ETH", "U18"), BID,
//                    0.02972, 0.02929, 3)
            createStopLimitRange(exchange, CurrencyPair("XBT", "USD"), BID,
                    6326.5, 6390.5, 3)

            Unit
        }

        private fun createStopLimitRange(exchange: XChangeWrapper, pair: CurrencyPair, type: Order.OrderType,
                                         high: Double, low: Double, amount: Int) {
            val orders = ArrayList<BitmexPlaceOrderParameters>()

            Constants.minimumPriceSteps[pair]?.let { minStep ->
                val stepSize = ((high - low) / 7).let { it - it % minStep }
                var price = low;
                while (price <= high) {
                    orders.add(BitmexPlaceOrderParameters.Builder(pair.toBitmexSymbol())
                            .setPrice(price.toBigDecimal())
                            .setStopPrice((price + minStep * if (type == BID) 1 else -1).toBigDecimal())
                            .setOrderQuantity(amount.toBigDecimal())
                            .setExecutionInstructions(BitmexExecutionInstruction.fromParameter(true, true))
//                            .setContingencyType(BitmexContingencyType.OCO)
//                            .setClOrdLinkId("fooboo")
                            .build())

                    price += stepSize
                }

                if (orders.isNotEmpty())
                    exchange.placeBulkOrders(orders)
            }
        }
    }
}