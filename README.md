# BitMEX-Helper
Java application to execute bulk orders on the BitMEX exchange.

## Features:
* NEW: Create Bulk Order by editing every single order
* NEW: Link orders with OTO, OCO, OUOA and OUOP
![BitMEX LinkedOrderTypes](https://github.com/SaschaZ/BitMEX-Helper/raw/master/media/BitMEX-LinkedOrderTypes.png)
* Uses native bulk order feature which is only available via the BitMEX Api.
* From the BitMEX Api documentation: `Bulk orders require fewer risk checks in the trading engine and thus are ratelimited at 1/10 the normal rate.`
* Place up to 100 orders with one call.
* Offers three methods of amount distribution:

| Order |     FLAT    |      DCA (Dollar Cost Average)       |  SAME  |
| ----- | ----------- | ------------------------------------ | ------ |
|  1    | min. amount | min. amount                          | amount |
|  2    | min. amount | total after first order * parameter  | amount |
|  3    | min. amount | total after second order * parameter | amount |
|  ...  | ...         | ...                                  | ...    |
* Support for `postOnly` and `reduceOnly` order modifications.
* Realtime bulk order review.
* Supports `LIMIT`, `STOP` and `STOP_LIMIT` order types. The limit price for a `STOP_LIMIT` order is always one step
    below (long) or above  (short) the stop price.
* Supports all major BitMEX pairs (XBT/USD, ETH/USD, ADA/BTC, BCH/BTC, EOS/BTC, LTC/BTC, TRX/BTC, XRP/BTC).
* Uses the Open-Source [XChange Library](https://github.com/knowm/XChange) to connect to the BitMEX Api.
* **DOES ONLY CONNECT TO BITMEX. YOUR DATA IS SAFE.**

![BitMEX preview](https://github.com/SaschaZ/BitMEX-Helper/raw/master/media/BitMEX-Helper.png)

# **Download: [v1.1.0](https://github.com/SaschaZ/BitMEX-Helper/raw/master/releases/v1.1.0/BitmexHelper.jar)**

To run this application you need an installed [Java Runtime Environment](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html).
Depending on the configuration of your OS the steps to start this application differs. On some systems you can just
double click the jar file. On other systems you need to execute `java -jar BitmexHelper.jar` in the same directory as
the jar file.
