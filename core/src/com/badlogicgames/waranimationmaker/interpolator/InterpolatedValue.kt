package com.badlogicgames.waranimationmaker.interpolator

import java.util.*

abstract class InterpolatedValue<I : Number, O>(initValue: O, initTime: I) {
    abstract var interpolator: Interpolator<I, O>
    val setPoints: SortedMap<I, O> = TreeMap()
    var value: O = initValue

    abstract fun updateInterpolator()
    open fun update(time: I): O { // Updates value based on time and returns it
        if (setPoints.isEmpty()) {
            throw IllegalArgumentException("Movement frames can not be empty when goToTime is called")
        }
        if (interpolator == null) { // Interpolator may not be serialized
            updateInterpolator()
        }

        value = interpolator.interpolateAt(time)

        return value
    }

    open fun removeFrame(x: I): Boolean {
        if (setPoints.size > 1) {
            if (setPoints.remove(x) != null) { // Remove was successful or not
                updateInterpolator()
                return true
            } else {
                return false
            }
        } else {
            return false
        }
    }

    fun newSetPoint(time: I, value: O) {
        if (time.toDouble() > setPoints.keys.last().toDouble()) { // Adds time and value to the end
            setPoints[time] = value
            updateInterpolator()
            println("Appended, new motions: $setPoints")
            return
        }

        for (definedTime in setPoints.keys) {
            if (time.toDouble() == definedTime.toDouble()) {
                setPoints[definedTime] = value
                updateInterpolator()
                return
            }
            if (definedTime.toDouble() > time.toDouble()) {
                setPoints[time] = value
                updateInterpolator()

                return
            }
        }
    }

    // When you add a time coordinate pair to an object which hasn't had a defined movement for a long time, it will interpolate a motion the whole way, which can be undesirable
    // Ex. last defined position was at time 0, you want it to move to another position at 800
    // But you only want it to move starting from time 600
    // The below function is used hold the object at the last position until the desired time
    fun holdValueUntil(time: I) {
        var prevTime: I? = null
        var prevValue: O? = null

        val frameTimes = setPoints.keys.toList()

        for (i in frameTimes.indices) {
            val definedTime = frameTimes[i]

            if (definedTime.toDouble() == time.toDouble()) { // If the time is already defined, don't do anything
                return
            }

            if ((definedTime.toDouble() > time.toDouble()) && (prevTime != null)) { // If the input time is not defined but is in the defined period, modify the movement to stay at the position just before the input time until the input time
                setPoints[time] = prevValue!!
                updateInterpolator()

                println("Added hold frame: $setPoints")
                return
            }

            prevTime = definedTime
            prevValue = setPoints[prevTime]
        }
        // If the input time was not in the defined period, add a movement to the end
        setPoints[time] = setPoints.entries.last().value
        updateInterpolator()
    }
}