@file:Suppress("unused", "EXPERIMENTAL_FEATURE_WARNING")

package com.gapps.utils

import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import java.io.File
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.math.absoluteValue
import kotlin.math.max

fun Long.divMod(div: Double, modulo: ((Long, Long) -> Unit)? = null) =
        (this / div).also { modulo?.invoke(it.toLong(), (this % div).toLong()) }

inline fun <T, U, V> whenNotNull(val0: T?, val1: U?, block: (val0: T, val1: U) -> V): V? =
        if (val0 != null && val1 != null) block(val0, val1) else null

inline fun <T, U, V, W> whenNotNull(val0: T?, val1: U?, val2: V?, block: (val0: T, val1: U, val2: V) -> W): W? =
        if (val0 != null && val1 != null && val2 != null) block(val0, val1, val2) else null

inline fun <T, U, V, W, X> whenNotNull(val0: T?, val1: U?, val2: V?, val3: W?, block: (val0: T, val1: U, val2: V, val3: W) -> X): X? =
        if (val0 != null && val1 != null && val2 != null && val3 != null) block(val0, val1, val2, val3) else null

inline fun <A, B, C, D, E, Z> whenNotNull(val0: A?, val1: B?, val2: C?, val3: D?, val4: E?,
                                          block: (val0: A, val1: B, val2: C, val3: D, val4: E) -> Z): Z? =
        if (val0 != null && val1 != null && val2 != null && val3 != null && val4 != null)
            block(val0, val1, val2, val3, val4) else null

inline fun <A, B, C, D, E, F, Z> whenNotNull(val0: A?, val1: B?, val2: C?, val3: D?, val4: E?, val5: F?,
                                             block: (val0: A, val1: B, val2: C, val3: D, val4: E, val5: F) -> Z): Z? =
        if (val0 != null && val1 != null && val2 != null && val3 != null && val4 != null && val5 != null)
            block(val0, val1, val2, val3, val4, val5) else null

inline fun <A, B, C, D, E, F, G, Z> whenNotNull(val0: A?, val1: B?, val2: C?, val3: D?, val4: E?, val5: F?, val6: G?,
                                                block: (val0: A, val1: B, val2: C, val3: D, val4: E, val5: F, val6: G) -> Z): Z? =
        if (val0 != null && val1 != null && val2 != null && val3 != null && val4 != null && val5 != null && val6 != null)
            block(val0, val1, val2, val3, val4, val5, val6) else null

inline fun <A, B, C, D, E, F, G, H, Z> whenNotNull(val0: A?, val1: B?, val2: C?, val3: D?, val4: E?, val5: F?, val6: G?, val7: H?,
                                                   block: (val0: A, val1: B, val2: C, val3: D, val4: E, val5: F, val6: G, val7: H) -> Z): Z? =
        if (val0 != null && val1 != null && val2 != null && val3 != null && val4 != null && val5 != null && val6 != null && val7 != null)
            block(val0, val1, val2, val3, val4, val5, val6, val7) else null

inline fun <A, B, C, D, E, F, G, H, I, Z> whenNotNull(val0: A?, val1: B?, val2: C?, val3: D?, val4: E?, val5: F?, val6: G?, val7: H?, val8: I?,
                                                      block: (val0: A, val1: B, val2: C, val3: D, val4: E, val5: F, val6: G, val7: H, val8: I) -> Z): Z? =
        if (val0 != null && val1 != null && val2 != null && val3 != null && val4 != null && val5 != null && val6 != null && val7 != null && val8 != null)
            block(val0, val1, val2, val3, val4, val5, val6, val7, val8) else null

fun List<Double?>.average(sizeForce: Int? = null, selector: ((Double?) -> Boolean)? = null): Double? {
    val selected = filterNotNull().filter { selector?.invoke(it) ?: true }
    if (selected.isEmpty()) return null
    return selected.sumByDouble { it } / (sizeForce ?: selected.size)
}

fun Double?.average(arg2: Double): Double = this?.let { (it + arg2) / 2 } ?: arg2

fun Any?.equalsOne(vararg args: Any) = args.any { args == this }

fun <E, K> List<E>.merge(key: (E) -> K, combine: (List<E>) -> E?): List<E> {
    val list = ArrayList<E>()
    val groupBy = groupBy(key)
    groupBy.forEach { entry -> combine(entry.value)?.let { list.add(it) } }
    return list
}

fun File.writeString(content: String) = printWriter().use { it.print(content) }

fun File.readString() = bufferedReader().readLines().joinToString("\n")

fun <T> List<T>.joinToStringIndexed(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((Int, T) -> CharSequence)? = null): String {
    var idx = 0
    val transformInternal: ((T) -> CharSequence)? = transform?.let { { value -> transform.invoke(idx++, value) } }
    return joinToString(separator, prefix, postfix, limit, truncated, transformInternal)
}

fun <T, U : Any> List<T>.mapPrev(convert: (cur: T, prev: T) -> U) = mapIndexed { index, value ->
    getOrNull(index - 1)?.let { convert(value, it) }
}.filterNotNull()

fun <K, V> ConcurrentMap<K, V>.getOrPut(key: K, defaultValue: V) = get(key)
        ?: put(key, defaultValue).let { defaultValue }

suspend fun <T> Mutex.withLockIfLocked(block: suspend () -> T) = if (isLocked) withLock { block() } else block()

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum: Long = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun <T> List<T>.lastOrNull(pre: Int = 0) = getOrNull(lastIndex - pre)

fun <T> List<T>.last(prevIndex: Int) = get(lastIndex - prevIndex)

fun Any?.formatSize(length: Int) = "%${length}s".format(this.toString())

fun Any?.asUnit() = Unit

fun Double?.posOr(or: Double = 0.0) = this?.let { max(or, it) } ?: or

fun <T> List<T>.lastOr(alt: T, prevIndex: Int = 0) = lastOrNull(prevIndex) ?: alt

fun <T> List<T>.getOr(index: Int, alt: T) = getOrNull(index) ?: alt

fun Double.abs() = absoluteValue

inline fun <T> List<T>.indexOfLast(predicate: (T, Int) -> Boolean): Int {
    var index = lastIndex
    return indexOfLast { value ->
        predicate(value, index--)
    }
}

inline fun <K, V> ConcurrentHashMap<K, V>.removeInMap(condition: (Map.Entry<K, V>) -> Boolean): ConcurrentHashMap<K, V> {
    forEach { if (condition(it)) remove(it.key) }
    return this
}

fun minInt(vararg values: Number?) = values.minBy { it?.toInt() ?: Int.MAX_VALUE }?.toInt()!!

fun minDouble(vararg values: Number?) = values.minBy { it?.toDouble() ?: Double.MAX_VALUE }?.toDouble()!!

fun maxInt(vararg values: Number?) = values.maxBy { it?.toInt() ?: Int.MIN_VALUE }?.toInt()!!

fun maxDouble(vararg values: Number?) = values.maxBy { it?.toDouble() ?: Double.MIN_VALUE }?.toDouble()!!

fun Double.exp10(power: Double) = this * Math.pow(10.0, power)

fun Double?.zeroToNull() = if (this?.let { it > 0 } == true) this else null

fun String?.writeToFile(path: String) = this?.let { content ->
    File(path).writeString(content)
    true
} ?: false

suspend fun <K0, V0, K1, V1> ConcurrentHashMap<K0, V0>.mapToMap(
        block: suspend (MutableMap.MutableEntry<K0, V0>) -> Pair<K1, V1>): ConcurrentHashMap<K1, V1> {
    val newMap = ConcurrentHashMap<K1, V1>()

    entries.forEach { oldMapEntry ->
        block(oldMapEntry).also { toPut ->
            newMap[toPut.first] = toPut.second
        }
    }

    return newMap
}

fun Double.decimalPlaces(): Int {
    val df = DecimalFormat("#.########")
    val formatted = df.format(this)
    var pointIndex = formatted.indexOf(".")
    if (pointIndex < 0)
        pointIndex = formatted.indexOf(",")
    return if (pointIndex < 0) 0 else formatted.lastIndex - pointIndex
}

fun Double.round(stepSize: Double) = BigDecimal(this).let { it.minus(it.divideAndRemainder(BigDecimal(stepSize))[1]) }.toDouble()