package com.gapps.bitmexhelper.kotlin.ui

import javafx.util.StringConverter
import java.text.DecimalFormat

class SmallDoubleStringConverter : StringConverter<Double>() {

    private val df = DecimalFormat("#.########")

    override fun fromString(value: String?) =
            value?.let { if (value.trim().isBlank()) null else value.toDouble() }

    override fun toString(value: Double?) = value?.let { df.format(value) } ?: ""
}