@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package javafx.scene.control

import com.gapps.bitmexhelper.kotlin.ui.BigDecimalStringConverter
import com.gapps.utils.max
import com.gapps.utils.min
import javafx.beans.property.SimpleObjectProperty
import java.math.BigDecimal

class BigDecimalValueFactory(_min: BigDecimal,
                             _max: BigDecimal,
                             _initial: BigDecimal,
                             _step: BigDecimal) : SpinnerValueFactory<BigDecimal>() {

    private val minimum: SimpleObjectProperty<BigDecimal> = object : SimpleObjectProperty<BigDecimal>(this, "minimum") {
        override fun invalidated() {
            val currentValue = value ?: return
            value = min(currentValue, maximum.value)
        }
    }

    private val maximum = object : SimpleObjectProperty<BigDecimal>(this, "maximum") {
        override fun invalidated() {
            val currentValue = value ?: return
            value = max(currentValue, minimum.value)
        }
    }

    private val amountToStepBy = SimpleObjectProperty<BigDecimal>(this, "amountToStepBy")

    init {
        minimum.value = _min
        maximum.value = _max
        amountToStepBy.value = _step

        converter = BigDecimalStringConverter(_step)

        valueProperty().addListener { _, _, newValue ->
            // when the value is set, we need to react to ensure it is a
            // valid value (and if not, blow up appropriately)
            if (newValue < _min) {
                value = _min
            } else if (newValue > _max) {
                value = _max
            }
        }
        value = if (_initial in _min.._max) _initial else _min
    }

    fun setMin(value: Double): Unit = minimum.set(value.toBigDecimal())

    fun getMin() = minimum.get().toDouble()

    fun setMax(value: Double): Unit = maximum.set(value.toBigDecimal())

    fun getMax(): Double = maximum.get().toDouble()

    fun setAmountToStepBy(value: Double): Unit = amountToStepBy.set(value.toBigDecimal())

    fun getAmountToStepBy(): Double = amountToStepBy.get().toDouble()

    override fun decrement(steps: Int) {
        val newValue = value.subtract(amountToStepBy.value.multiply(steps.toBigDecimal()))
        value = max(minimum.value, min(maximum.value, newValue))
    }

    override fun increment(steps: Int) {
        val newValue = value.add(amountToStepBy.value.multiply(steps.toBigDecimal()))
        value = max(minimum.value, min(maximum.value, newValue))
    }
}