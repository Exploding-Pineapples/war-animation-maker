package com.badlogicgames.waranimationmaker.interpolator

import java.util.*

abstract class InterpolatedValue<I : Number, O>(initValue: O, initTime: I) : HasSetPoints<I, O> {
    abstract var interpolationFunction: InterpolationFunction<I, O>
    override var setPoints: SortedMap<I, O> = TreeMap()
    @Transient var value: O = initValue

    open fun update(time: I): O { // Updates value based on time and returns it
        if (setPoints.isEmpty()) {
            throw IllegalArgumentException("Set points can not be empty when update is called")
        }
        if (interpolationFunction == null) { // Interpolator not serialized
            updateInterpolationFunction()
        }

        value = interpolationFunction.evaluate(time)

        return value
    }
}