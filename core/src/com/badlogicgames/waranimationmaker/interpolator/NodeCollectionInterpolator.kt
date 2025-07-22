package com.badlogicgames.waranimationmaker.interpolator

import com.badlogicgames.waranimationmaker.models.Coordinate
import com.badlogicgames.waranimationmaker.models.NodeCollectionSetPoint
import com.badlogicgames.waranimationmaker.utilities.toDoubleArray
import java.util.*

class NodeCollectionInterpolator : HasSetPoints<Int, NodeCollectionSetPoint> {
    override var setPoints: SortedMap<Int, NodeCollectionSetPoint> = TreeMap()

    override fun updateInterpolationFunction() {
    }

    fun evaluate(time: Int): Array<Coordinate> {
        val num = 100

        val parameter = Array(num) { index -> index.toDouble() / num }
        val points = Array(num) { _ -> Coordinate(0f, 0f)}

        for (i in 0 until num) { // For every point to draw, build an interpolator for the point which evaluates from a specific parameter value (0 to 1) through time
            val xInTime = Array(setPoints.size) { 0.0 }
            val yInTime = Array(setPoints.size) { 0.0 }

            var index = 0
            for (frame in setPoints) {
                val frameTime = frame.key
                frame.value.updateInterpolators()
                // frame.value's interpolators are through space at a specific time
                xInTime[index] = frame.value.xInterpolator.evaluate(parameter[i])
                yInTime[index] = frame.value.yInterpolator.evaluate(parameter[i])
                index++
            }

            val xInterpolatorTime = PCHIPInterpolationFunction<Int>(setPoints.keys.toTypedArray(), xInTime)
            val yInterpolatorTime = PCHIPInterpolationFunction<Int>(setPoints.keys.toTypedArray(), yInTime)

            points[i] = Coordinate(xInterpolatorTime.evaluate(time).toFloat(), yInterpolatorTime.evaluate(time).toFloat())
        }

        return points
    }
}