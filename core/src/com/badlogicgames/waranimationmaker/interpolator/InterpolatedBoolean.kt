package com.badlogicgames.waranimationmaker.interpolator

class InterpolatedBoolean(initValue: Boolean, initTime: Int) : InterpolatedValue<Int, Boolean>(initValue, initTime) {

    @Transient
    override var interpolationFunction: InterpolationFunction<Int, Boolean> =
        StepInterpolationFunction(
            arrayOf(initTime),
            arrayOf(initValue)
        )

    init {
        setPoints[initTime] = initValue
        updateInterpolationFunction()
    }

    override fun updateInterpolationFunction() {
        interpolationFunction =
            StepInterpolationFunction(
                setPoints.keys.toTypedArray(),
                setPoints.values.toTypedArray()
            )
    }
}