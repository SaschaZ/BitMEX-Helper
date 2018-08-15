@file:Suppress("unused")

package com.gapps.utils

import com.gapps.utils.DateFormat.*
import com.gapps.utils.TimeUnit.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

enum class DateFormat {
    COMPLETE,
    DATE_ONLY,
    TIME_ONLY,
    PLOT,
    FILENAME
}

fun formatDate(format: DateFormat = COMPLETE) = System.currentTimeMillis().formatDate(format)

fun Long.formatDate(format: DateFormat = COMPLETE) = SimpleDateFormat(when (format) {
    COMPLETE -> "dd.MM-HH:mm:ss"
    DATE_ONLY -> "dd.MM.YYYY"
    TIME_ONLY -> "HH:mm:ss"
    PLOT -> "YYYY-MM-dd HH:mm:ss"
    FILENAME -> "YYYYMMddHHmm"
}).format(Date(this))!!

fun Long.wasInLastDays(days: Int) = this > System.currentTimeMillis() - days.toMs(D)

fun Long.percentOfDays(days: Int): Double {
    val now = System.currentTimeMillis()
    val end = now - days.toMs(D).toDouble()
    return (this - end) / (now - end) * 100
}

fun Double.format(digits: Int = 2, shortForm: Boolean = true) = String.format(Locale.ENGLISH, "%.${digits}f",
        when {
            shortForm && abs() > 1000000 -> this / 1000000.0
            shortForm && abs() > 1000 -> this / 1000.0
            else -> this
        }).let {
    "$it${
    when {
        shortForm && abs() > 1000000 -> "M"
        shortForm && abs() > 1000 -> "K"
        else -> ""
    }}"
}

fun Double.formatExp(digits: Int = 2) = if (this == 0.0) "0" else
    DecimalFormat("0.${(0 until digits).joinToString("") { "0" }}E0").format(this)!!

fun Long.formatTimeSpan(align: Boolean = false, onlyDaysAndHours: Boolean = false, withSeconds: Boolean = false): String {
    val elements = mutableListOf<String>()
    msTo(D) { days, daysRest ->
        if (days > 0)
            elements.add("${days}d")
        daysRest.msTo(H) { hours, hoursRest ->
            if (hours > 0 || elements.isNotEmpty())
                elements.add("${(if (align) "%2d" else "%d").format(hours)}h")
            if (!onlyDaysAndHours) hoursRest.msTo(M) { minutes, minutesRest ->
                if (minutes > 0 || elements.isNotEmpty())
                    elements.add("${(if (align) "%2d" else "%d").format(minutes)}min")
                if (withSeconds) minutesRest.msTo(S) { seconds, millis ->
                    if (seconds > 0 || elements.isNotEmpty())
                        elements.add("${(if (align) "%2d" else "%d").format(seconds)}s")
                    if (millis > 0 && elements.isEmpty())
                        elements.add("${(if (align) "%3d" else "%d").format(millis)}ms")
                }
            }
        }
    }
    return elements.joinToString(" ")
}

enum class TimeUnit(val msFactor: Long) {
    S(1000),
    SECONDS(S.msFactor),
    M(60 * S.msFactor),
    MINUTES(M.msFactor),
    H(60 * M.msFactor),
    HOURS(H.msFactor),
    D(24 * H.msFactor),
    DAYS(D.msFactor),
    W(7 * D.msFactor),
    WEEKS(W.msFactor),
    MONTHS(30 * D.msFactor)
}

fun Int.toMs(unit: TimeUnit): Long = toDouble().toMs(unit)
fun Long.toMs(unit: TimeUnit): Long = toDouble().toMs(unit)
fun Double.toMs(unit: TimeUnit): Long = (this * unit.msFactor).toLong()

fun Long.msTo(unit: TimeUnit, result: ((div: Long, rest: Long) -> Unit)? = null): Double = divMod(unit.msFactor.toDouble(), result)