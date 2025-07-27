package com.badlogicgames.waranimationmaker.models

import com.badlogicgames.waranimationmaker.interpolator.InterpolationFunction
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolationFunction
import kotlin.math.hypot

class NodeCollectionSetPoint(val time: Int, val id: NodeCollectionID, var nodes: MutableList<Node> = mutableListOf()) {
    var xInterpolator: InterpolationFunction<Double, Double> = PCHIPInterpolationFunction(arrayOf(0.0), arrayOf(0.0))
    var yInterpolator: InterpolationFunction<Double, Double> = PCHIPInterpolationFunction(arrayOf(0.0), arrayOf(0.0))
    var length: Double = 0.0

    init {
        updateInterpolators()
    }

    fun updateInterpolators() {
        val distances = mutableListOf<Double>()
        var totalDistance = 0.0

        for (j in 0..<nodes.size - 1) {
            val node = nodes[j]
            val nextNode = nodes[j + 1]
            val distance = hypot(nextNode.screenPosition.x - node.screenPosition.x, nextNode.screenPosition.y - node.screenPosition.y).toDouble()
            distances.add(distance)
            totalDistance += distance
        }
        length = totalDistance

        val tVals = mutableListOf<Double>()
        val xVals = mutableListOf<Double>()
        val yVals = mutableListOf<Double>()

        val coordinates = nodes.map { it.screenPosition }

        if (coordinates.isNotEmpty()) {
            var t = 0.0

            for (i in 0..<coordinates.size - 1) {
                tVals.add(t)
                xVals.add(coordinates[i].x.toDouble())
                yVals.add(coordinates[i].y.toDouble())

                t = 0.0
                for (j in 0..i) {
                    t += distances[j]
                }
                t /= totalDistance
            }
            tVals.add(1.0)
            xVals.add(coordinates.last().x.toDouble())
            yVals.add(coordinates.last().y.toDouble())
        }

        xInterpolator.i = tVals.toTypedArray()
        yInterpolator.i = tVals.toTypedArray()
        xInterpolator.o = xVals.toTypedArray()
        yInterpolator.o = yVals.toTypedArray()
    }

    fun duplicate(time: Int, animation: Animation): NodeCollectionSetPoint {
        val newSetPoint = NodeCollectionSetPoint(time, NodeCollectionID(id.value))
        for (node in nodes) {
            newSetPoint.nodes.add(animation.newNode(node.position.x, node.position.y, time).apply {
                edges.addAll(node.edges.map {
                    Edge(NodeCollectionID(it.collectionID.value), it.segment
                ) })
            })
        }
        return newSetPoint
    }
}