# BitMEX-Helper
Java application to execute bulk orders on the BitMEX exchange.

## Features:
* Uses native bulk order feature which is only available via the BitMEX Api.
* From the BitMEX Api documentation: `Bulk orders require fewer risk checks in the trading engine and thus are ratelimited at 1/10 the normal rate.`
* Place up to 100 orders with one call.
* Offers three methods of amount distribution:

| Order |     FLAT    |       MULT_MIN                     |        DIV_AMOUNT                  |
|:-----:| ----------- | ---------------------------------- | ---------------------------------- |
|   1   | min. amount | min. amount                        | total amount / parameter           |
|   2   | min. amount | amount of first order * parameter  | amount of first order / parameter  |
|   3   | min. amount | amount of second order * parameter | amount of second order / parameter |
|  ...  | ...         | ...                                | ...                                |
* Support for `postOnly` and `reduceOnly` order modifications.
* Realtime bulk order review.
* Supports `LIMIT`, `STOP` and `STOP_LIMIT` order types. The limit price for a `STOP_LIMIT` order is always one step
    below (long) or above  (short) the stop price.
* Supports all major BitMEX pairs (XBT/BTC, ETH/BTC, ADA/BTC, BCH/BTC, EOS/BTC, LTC/BTC, TRX/BTC, XRP/BTC).
* Uses the Open-Source [XChange Library](https://github.com/knowm/XChange) to connect to the BitMEX Api.
* **DOES ONLY CONNECT TO BITMEX. YOUR DATA IS SAFE.**

![BitMEX preview](https://github.com/SaschaZ/BitMEX-Helper/raw/master/media/BitMEX-Helper.png)

# **Download: [v1.0.2](https://github.com/SaschaZ/BitMEX-Helper/raw/master/releases/v1.0.2/BitmexHelper.jar)**

To run this application you need an installed [Java Runtime Environment](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html).
Depending on the configuration of your OS the steps to start this application differs. On some systems you can just
double click the jar file. On other systems you need to execute `java -jar BitmexHelper.jar` in the same directory as
the jar file.
