package com.badlogicgames.waranimationmaker.interpolator

import com.badlogicgames.waranimationmaker.models.Coordinate
import com.badlogicgames.waranimationmaker.models.NodeCollectionSetPoint
import java.util.*

class NodeCollectionInterpolator : HasSetPoints<Int, NodeCollectionSetPoint> {
    override var setPoints: SortedMap<Int, NodeCollectionSetPoint> = TreeMap()

    val num = 100

    override fun updateInterpolationFunction() {
    }

    fun evaluate(time: Int): Array<Coordinate> {
        val parameter = Array(num) { index -> index.toDouble() / num }
        val points = Array(num) { _ -> Coordinate(0f, 0f)}


        for (i in 0 until num) { // For every point to draw, build an interpolator for the point which evaluates from a specific parameter value (0 to 1) through time
            val xInterpolatorTime = PCHIPInterpolatedFloat(0f, 0)
            val yInterpolatorTime = PCHIPInterpolatedFloat(0f, 0)
            xInterpolatorTime.setPoints.clear()
            yInterpolatorTime.setPoints.clear()

            for (frame in setPoints) {
                val frameTime = frame.key
                frame.value.updateInterpolators()
                // frame.value's interpolators are through space at a specific time
                xInterpolatorTime.setPoints[frameTime] = frame.value.xInterpolator.evaluate(parameter[i]).toFloat()
                yInterpolatorTime.setPoints[frameTime] = frame.value.yInterpolator.evaluate(parameter[i]).toFloat()
            }

            xInterpolatorTime.updateInterpolationFunction()
            yInterpolatorTime.updateInterpolationFunction()

            points[i] = Coordinate(xInterpolatorTime.interpolationFunction.evaluate(time), yInterpolatorTime.interpolationFunction.evaluate(time))
        }

        return points
    }
}