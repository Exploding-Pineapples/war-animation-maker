package com.badlogicgames.waranimationmaker.interpolator

import com.badlogicgames.waranimationmaker.utilities.toDoubleArray

class LinearInterpolatedFloat(initValue: Float, initTime: Int) : InterpolatedValue<Int, Float>(initValue, initTime) {

    @Transient
    override var interpolator = LinearInterpolator(arrayOf(initTime.toDouble()), arrayOf(initValue.toDouble())).map({
        it.toInt()
    }, {
        it.toFloat()
    }, {
        it.toDouble()
    })

    init {
        setPoints[initTime] = initValue
        updateInterpolator()
    }

    override fun updateInterpolator() {
        interpolator = LinearInterpolator(
            setPoints.keys.toDoubleArray(),
            setPoints.values.toDoubleArray()
        ).map({ it.toInt() }, { it.toFloat() }, { it.toDouble() })
    }
}