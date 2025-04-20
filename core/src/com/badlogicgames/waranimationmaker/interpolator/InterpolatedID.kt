package com.badlogicgames.waranimationmaker.interpolator


class InterpolatedID(initTime: Int, initID: Int) : InterpolatedValue<Int, Int>(initID, initTime) {
    @Transient
    override var interpolator: Interpolator<Int, Int> = StepInterpolator(arrayOf(initTime), arrayOf(initID))

    init {
        println("Created new Interpolated Value")
        setPoints[initTime] = initID
        updateInterpolator()
    }

    override fun updateInterpolator() {
        interpolator = StepInterpolator(
            setPoints.keys.toTypedArray(),
            setPoints.values.toTypedArray()
        )
    }
}