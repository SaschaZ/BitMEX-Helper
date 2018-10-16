package com.gapps.bitmexhelper.kotlin.ui

import com.gapps.utils.decimalPlaces
import com.gapps.utils.round
import javafx.util.StringConverter
import java.math.BigDecimal
import java.text.DecimalFormat

class BigDecimalStringConverter(private val step: BigDecimal) : StringConverter<BigDecimal>() {

    private val decimalPlaces = step.decimalPlaces()
    private val df = DecimalFormat("#0${if (decimalPlaces > 0)"." else ""}" +
            (0 until decimalPlaces).joinToString("") { "0" } +
            (0 until decimalPlaces).joinToString("") { "#" })

    override fun fromString(value: String?) =
            value?.let { if (value.trim().isBlank()) null else value.replace(",", ".").toBigDecimal() }

    override fun toString(value: BigDecimal?) = value?.let { df.format(value.round(step)) } ?: ""
}