@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package javafx.scene.control

import com.gapps.bitmexhelper.kotlin.ui.SmallDoubleStringConverter
import com.gapps.utils.decimalPlaces
import javafx.beans.property.SimpleDoubleProperty
import java.math.BigDecimal
import kotlin.math.max
import kotlin.math.min

class SmallDoubleValueFactory(_min: Double,
                                   _max: Double,
                                   _initial: Double,
                                   _step: Double) : SpinnerValueFactory<Double>() {

    private val min = object : SimpleDoubleProperty(this, "min") {
        override fun invalidated() {
            val currentValue = value ?: return
            value = min(currentValue, getMax())
        }
    }

    private val max = object : SimpleDoubleProperty(this, "max") {
        override fun invalidated() {
            val currentValue = value ?: return
            value = max(currentValue, getMin())
        }
    }

    private val amountToStepBy = SimpleDoubleProperty(this, "amountToStepBy")

    init {
        setMin(_min)
        setMax(_max)
        setAmountToStepBy(_step)

        converter = SmallDoubleStringConverter(_step)

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

    fun setMin(value: Double): Unit = min.set(value)

    fun getMin() = min.get()

    fun setMax(value: Double): Unit = max.set(value)

    fun getMax(): Double = max.get()

    fun setAmountToStepBy(value: Double): Unit = amountToStepBy.set(value)

    fun getAmountToStepBy(): Double = amountToStepBy.get()


    /** {@inheritDoc}  */
    override fun decrement(steps: Int) {
        val currentValue = BigDecimal.valueOf(value)
        val minBigDecimal = BigDecimal.valueOf(getMin())
        val maxBigDecimal = BigDecimal.valueOf(getMax())
        val amountToStepByBigDecimal = BigDecimal.valueOf(getAmountToStepBy())
        val newValue = currentValue.subtract(amountToStepByBigDecimal.multiply(BigDecimal.valueOf(steps.toLong())))
        value = when {
            newValue >= minBigDecimal -> newValue.toDouble()
            isWrapAround -> Spinner.wrapValue(newValue, minBigDecimal, maxBigDecimal).toDouble()
            else -> getMin()
        }
    }

    /** {@inheritDoc}  */
    override fun increment(steps: Int) {
        val currentValue = BigDecimal.valueOf(value)
        val minBigDecimal = BigDecimal.valueOf(getMin())
        val maxBigDecimal = BigDecimal.valueOf(getMax())
        val amountToStepByBigDecimal = BigDecimal.valueOf(getAmountToStepBy())
        val newValue = currentValue.add(amountToStepByBigDecimal.multiply(BigDecimal.valueOf(steps.toLong())))
        value = when {
            newValue <= maxBigDecimal -> newValue.toDouble()
            isWrapAround -> Spinner.wrapValue(newValue, minBigDecimal, maxBigDecimal).toDouble()
            else -> getMax()
        }
    }
}