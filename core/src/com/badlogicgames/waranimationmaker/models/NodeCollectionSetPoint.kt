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
            totalDistance += distance
            distances.add(totalDistance)
        }
        length = totalDistance

        val tVals = mutableListOf<Double>()
        val xVals = mutableListOf<Double>()
        val yVals = mutableListOf<Double>()

        val coordinates = nodes.map { it.screenPosition }

        if (coordinates.isNotEmpty()) {
            var t:Double

            for (i in coordinates.indices) {
                t = i.toDouble() / (coordinates.size - 1)

                tVals.add(t)
                xVals.add(coordinates[i].x.toDouble())
                yVals.add(coordinates[i].y.toDouble())
            }
        }

        xInterpolator.i = tVals.toTypedArray()
        yInterpolator.i = tVals.toTypedArray()
        xInterpolator.o = xVals.toTypedArray()
        yInterpolator.o = yVals.toTypedArray()
    }

    fun duplicate(time: Int, animation: Animation): NodeCollectionSetPoint {
        // TODO this will not work with multiple NCs sharing one node because duplication will create new nodes for each one
        val newSetPoint = NodeCollectionSetPoint(time, NodeCollectionID(id.value))
        for (node in nodes) {
            newSetPoint.nodes.add(animation.newNode(node.position.x, node.position.y, time))
        }
        for (index in 0..<newSetPoint.nodes.size - 1) {
            val node = newSetPoint.nodes[index]
            val nextNode = newSetPoint.nodes[index + 1]
            node.edges.add(Edge(id.duplicate(), Pair(node.id.duplicate(), nextNode.id.duplicate())))
        }
        return newSetPoint
    }
}