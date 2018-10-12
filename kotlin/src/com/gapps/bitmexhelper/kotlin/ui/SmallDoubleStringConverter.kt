package com.gapps.bitmexhelper.kotlin.ui

import com.gapps.utils.decimalPlaces
import javafx.util.StringConverter
import java.text.DecimalFormat

class SmallDoubleStringConverter(step: Double) : StringConverter<Double>() {

    private val decimalPlaces = step.decimalPlaces()
    private val df = DecimalFormat("#${if (decimalPlaces > 0)"." else ""}${(0 until decimalPlaces).joinToString("") { "#" }}")

    override fun fromString(value: String?) =
            value?.let { if (value.trim().isBlank()) null else value.replace(",", ".").toDouble() }

    override fun toString(value: Double?) = value?.let { df.format(value) } ?: ""
}