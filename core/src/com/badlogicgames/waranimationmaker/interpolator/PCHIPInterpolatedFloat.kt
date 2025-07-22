package com.badlogicgames.waranimationmaker.interpolator

import com.badlogicgames.waranimationmaker.utilities.toDoubleArray

class PCHIPInterpolatedFloat(initValue: Float, initTime: Int) : InterpolatedValue<Int, Float>(initValue, initTime) {

    @Transient
    override var interpolationFunction = PCHIPInterpolationFunction(arrayOf(initTime.toDouble()), arrayOf(initValue.toDouble())).map({
        it.toInt()
    }, {
        it.toFloat()
    }, {
        it.toDouble()
    })

    init {
        setPoints[initTime] = initValue
        updateInterpolationFunction()
    }

    override fun updateInterpolationFunction() {
        interpolationFunction = PCHIPInterpolationFunction(
            setPoints.keys.toDoubleArray(),
            setPoints.values.toDoubleArray()
        ).map({ it.toInt() }, { it.toFloat() }, { it.toDouble() })
    }
}