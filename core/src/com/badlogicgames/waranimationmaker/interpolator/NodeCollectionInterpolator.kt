package com.badlogicgames.waranimationmaker.interpolator

import com.badlogicgames.waranimationmaker.AnimationScreen
import com.badlogicgames.waranimationmaker.models.Coordinate
import com.badlogicgames.waranimationmaker.models.NodeCollectionSetPoint
import java.util.*
import kotlin.math.max
import kotlin.math.round

class NodeCollectionInterpolator : HasSetPoints<Int, NodeCollectionSetPoint> {
    override var setPoints: SortedMap<Int, NodeCollectionSetPoint> = TreeMap()
    var coordinates: Array<Coordinate> = arrayOf()

    override fun updateInterpolationFunction() {
    }

    fun evaluate(time: Int): Array<Coordinate> {
        setPoints.values.forEach { it.updateInterpolators() }

        var num = 0

        if (setPoints.containsKey(time)) {
            num = round((setPoints[time]?.length ?: 0.0) / AnimationScreen.LINE_RESOLUTION).toInt()
        } else {
            if (setPoints.size < 2) {
                num = round((setPoints.values.first().length / AnimationScreen.LINE_RESOLUTION)).toInt()
            } else {
                var index = 0
                var time0 = 0
                var length0 = 0.0
                var found = false

                for (frame in setPoints) {
                    if (found) {
                        val deltaTime = frame.value.time - time0
                        num = round((length0 + ((time - time0).toDouble() / deltaTime) * (frame.value.length - length0)) / AnimationScreen.LINE_RESOLUTION).toInt()
                        break
                    }
                    if (frame.key < time) {
                        time0 = frame.value.time
                        length0 = frame.value.length
                        found = true
                    }
                    index++
                }
            }
        }

        num = max(0, num)

        val parameter = Array(num) { index -> index.toDouble() / num }
        val coordinates = mutableListOf<Coordinate>()

        for (i in 0 until num) { // For every point to draw, build an interpolator for the point which evaluates from a specific parameter value (0 to 1) through time
            val xInTime = Array(setPoints.size) { 0.0 }
            val yInTime = Array(setPoints.size) { 0.0 }

            var index = 0
            for (frame in setPoints) {

                // frame.value's interpolators are through space at a specific time
                xInTime[index] = frame.value.xInterpolator.evaluate(parameter[i])
                yInTime[index] = frame.value.yInterpolator.evaluate(parameter[i])
                index++
            }


            val xInterpolatorTime = PCHIPInterpolationFunction<Int>(setPoints.keys.toTypedArray(), xInTime)
            val yInterpolatorTime = PCHIPInterpolationFunction<Int>(setPoints.keys.toTypedArray(), yInTime)

            val coordinate = Coordinate(xInterpolatorTime.evaluate(time).toFloat(), yInterpolatorTime.evaluate(time).toFloat())
            if (AnimationScreen.onScreen(coordinate)) {
                coordinates.add(coordinate)
            }
        }
        this.coordinates = coordinates.toTypedArray()

        return this.coordinates
    }
}