package com.badlogicgames.waranimationmaker.interpolator

import java.util.*

interface HasSetPoints<I : Number, O> {
    var setPoints: SortedMap<I, O>

    fun updateInterpolationFunction()

    fun removeFrame(x: I): Boolean {
        if (setPoints.size > 1) {
            if (setPoints.remove(x) != null) { // Remove was successful or not
                updateInterpolationFunction()
                return true
            } else {
                return false
            }
        } else {
            return false
        }
    }

    fun newSetPoint(time: I, value: O) {
        setPoints[time] = value
        updateInterpolationFunction()
    }

    fun newSetPoint(time: I, value: O, removeDuplicates: Boolean) {
        newSetPoint(time, value)

        if (removeDuplicates) { // Remove all set points after the new set point that have the same value in a row
            var found = false
            for (definedTime in setPoints.keys) {
                if (definedTime.toDouble() >= time.toDouble()) {
                    if (found) { // If the last set point already exists
                        if (setPoints[definedTime] == value) {
                            setPoints.remove(definedTime)
                        } else {
                            return
                        }
                    } else {
                        found = true
                    }
                }
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
                updateInterpolationFunction()

                println("Added hold frame: $setPoints")
                return
            }

            prevTime = definedTime
            prevValue = setPoints[prevTime]
        }
        // If the input time was not in the defined period, add a movement to the end
        setPoints[time] = setPoints.entries.last().value
        updateInterpolationFunction()
    }
}