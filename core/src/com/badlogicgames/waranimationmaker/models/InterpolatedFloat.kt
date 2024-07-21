package com.badlogicgames.waranimationmaker.models

import com.badlogicgames.waranimationmaker.interpolator.*
import java.util.*

class InterpolatedFloat(initValue: Float, initTime: Int) {
    var interpolator: PCHIPInterpolator = PCHIPInterpolator(doubleArrayOf(initTime.toDouble()), doubleArrayOf(initValue.toDouble()))
    val setPoints: SortedMap<Double, Double> = TreeMap()
    @Transient var value: Float = initValue

    init {
        println("Created new Interpolated Value")
        setPoints[initTime.toDouble()] = initValue.toDouble()
        updateInterpolator()
    }

    fun updateInterpolator() {
        print("updating interpolator")
        println(setPoints)
        interpolator = PCHIPInterpolator(doubleArrayOf(setPoints.keys.first() - 200) + setPoints.keys.toDoubleArray() + doubleArrayOf(setPoints.keys.last() + 200),
            doubleArrayOf(setPoints.values.first()) + setPoints.values.toDoubleArray() + doubleArrayOf(setPoints.values.last()))
        // This gives a flat slope at the end to try to keep the object in position
    }

    fun update(time: Int): Float { // Updates value based on time and returns it
        if (setPoints.isEmpty()) {
            throw IllegalArgumentException("Movement frames can not be empty when goToTime is called")
        }
        if (interpolator == null) { // Is null when animation is first opened because interpolator is @Transient
            updateInterpolator()
        }

        value = interpolator.interpolateAt(time.toDouble()).toFloat()

        return value
    }

    fun removeFrame(time: Int): Boolean {
        if (setPoints.size > 1) {
            if (setPoints.remove(time.toDouble()) != null) { // Remove was successful or not
                updateInterpolator()
                return true
            } else {
                return false
            }
        } else {
            return false
        }
    }

    fun newSetPoint(time: Int, value: Float) {
        if (time > (setPoints.keys.last())) { // Adds time and value to the end
            setPoints[time.toDouble()] = value.toDouble()
            updateInterpolator()
            println("Appended, new motions: $setPoints")
            return
        }

        for (definedTime in setPoints.keys) {
            if (time.toDouble() == definedTime) {
                setPoints[definedTime] = value.toDouble()
                updateInterpolator()
                println("Overwrote, new motions: $setPoints")
                return
            }
            if (definedTime > time) {
                setPoints[time.toDouble()] = value.toDouble()
                updateInterpolator()

                println("Inserted, new motions: $setPoints")
                return
            }
        }
    }

    // When you add a time coordinate pair to an object which hasn't had a defined movement for a long time, it will interpolate a motion the whole way, which can be undesirable
    // Ex. last defined position was at time 0, you want it to move to another position at 800
    // But you only want it to move starting from time 600
    // The below function is used hold the object at the last position until the desired time
    fun holdValueUntil(time: Int) {
        var prevTime: Double? = null
        var prevValue: Double? = null

        val frameTimes = setPoints.keys.toList()

        for (i in frameTimes.indices) {
            val definedTime = frameTimes[i]

            if (definedTime == time.toDouble()) { // If the time is already defined, don't do anything
                return
            }

            if ((definedTime > time) && (prevTime != null)) { // If the input time is not defined but is in the defined period, modify the movement to stay at the position just before the input time until the input time
                setPoints[time.toDouble()] = prevValue!!
                updateInterpolator()

                println("Added hold frame: $setPoints")
                return
            }

            prevTime = definedTime
            prevValue = setPoints[prevTime]
        }
        // If the input time was not in the defined period, add a movement to the end
        setPoints[time.toDouble()] = setPoints.entries.last().value
        updateInterpolator()
    }
}