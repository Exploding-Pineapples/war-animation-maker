package com.badlogicgames.waranimationmaker.interpolator

import com.badlogicgames.waranimationmaker.utilities.toDoubleArray

class InterpolatedBoolean(initValue: Boolean, initTime: Int) : InterpolatedValue<Int, Boolean>(initValue, initTime) {

    @Transient
    override var interpolator: Interpolator<Int, Boolean> = StepInterpolator(arrayOf(initTime), arrayOf(initValue))

    init {
        setPoints[initTime] = initValue
        updateInterpolator()
    }

    override fun updateInterpolator() {
        interpolator = StepInterpolator(setPoints.keys.toTypedArray(), setPoints.values.toTypedArray())
    }
}