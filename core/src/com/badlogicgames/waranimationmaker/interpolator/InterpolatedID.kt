package com.badlogicgames.waranimationmaker.interpolator

import com.badlogicgames.waranimationmaker.models.NodeID
import com.badlogicgames.waranimationmaker.utilities.toDoubleArray

class InterpolatedID(val initTime: Int, val initID: NodeID) : InterpolatedValue<Int, NodeID>(initID, initTime) {
    @Transient
    override var interpolator: Interpolator<Int, NodeID> = StepInterpolator(arrayOf(initTime), arrayOf(initID))

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