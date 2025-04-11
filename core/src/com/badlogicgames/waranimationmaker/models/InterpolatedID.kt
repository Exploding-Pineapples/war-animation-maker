package com.badlogicgames.waranimationmaker.models

import com.badlogicgames.waranimationmaker.interpolator.StepInterpolator
import com.badlogicgames.waranimationmaker.interpolator.map
import com.badlogicgames.waranimationmaker.utilities.toDoubleArray

class InterpolatedID(val initTime: Int, val initID: NodeID) : InterpolatedValue<Int, NodeID>(initID, initTime) {
    @Transient
    override var interpolator = StepInterpolator(arrayOf(initTime), arrayOf(initID)).map({
        it
    }, {
        it
    }, {
        it
    })

    init {
        println("Created new Interpolated Value")
        setPoints[initTime] = initID
        updateInterpolator()
    }

    override fun updateInterpolator() {
        interpolator = StepInterpolator(
            setPoints.keys.toDoubleArray(),
            setPoints.values.toTypedArray()
        ).map({ it.toInt() }, { it }, { it.toDouble() })
    }
}