package com.badlogicgames.waranimationmaker.models

import com.badlogicgames.waranimationmaker.interpolator.InterpolationFunction
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolationFunction

class NodeCollectionSetPoint(val time: Int, val id: NodeCollectionID, var nodes: MutableList<Node> = mutableListOf()) {
    var xInterpolator: InterpolationFunction<Double, Double> = PCHIPInterpolationFunction(arrayOf(0.0), arrayOf(0.0))
    var yInterpolator: InterpolationFunction<Double, Double> = PCHIPInterpolationFunction(arrayOf(0.0), arrayOf(0.0))

    init {
        updateInterpolators()
    }

    fun updateInterpolators() {
        val parameterVals = mutableListOf<Double>()
        val xVals = mutableListOf<Double>()
        val yVals = mutableListOf<Double>()

        val coordinates = nodes.map { it.position }

        if (coordinates.isNotEmpty()) {
            var parameter = 0.0
            var index = 0

            while (parameter <= 1.0) {
                parameterVals.add(parameter)
                xVals.add(coordinates[index].x.toDouble())
                yVals.add(coordinates[index].y.toDouble())

                index++
                parameter = index.toDouble() / (coordinates.size - 1.0)
            }
        }

        xInterpolator.i = parameterVals.toTypedArray()

        xInterpolator = PCHIPInterpolationFunction(parameterVals.toTypedArray(), xVals.toTypedArray())
        yInterpolator = PCHIPInterpolationFunction(parameterVals.toTypedArray(), yVals.toTypedArray())
    }
}