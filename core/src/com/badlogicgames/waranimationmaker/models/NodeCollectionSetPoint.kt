package com.badlogicgames.waranimationmaker.models

import com.badlogicgames.waranimationmaker.interpolator.InterpolationFunction
import com.badlogicgames.waranimationmaker.interpolator.LinearInterpolationFunction
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolationFunction
import kotlin.math.hypot

class NodeCollectionSetPoint(val time: Int, val id: NodeCollectionID, var nodes: MutableList<Node> = mutableListOf()) {
    var tInterpolator: InterpolationFunction<Int, Double> = LinearInterpolationFunction(arrayOf(0), arrayOf(0.0))
    var xInterpolator: InterpolationFunction<Double, Double> = PCHIPInterpolationFunction(arrayOf(0.0), doubleArrayOf(0.0))
    var yInterpolator: InterpolationFunction<Double, Double> = PCHIPInterpolationFunction(arrayOf(0.0), doubleArrayOf(0.0))
    var length: Double = 0.0

    init {
        updateInterpolators()
    }

    fun updateInterpolators() {
        val tSetPoints = sortedMapOf(Pair(0, 0.0), Pair(nodes.size - 1, 1.0))
        val distances = mutableListOf<Double>()
        var totalDistance = 0.0

        for (index in 0..<nodes.size - 1) {
            val node = nodes[index]
            if (node.tSetPoint != null) {
                tSetPoints[index] = node.tSetPoint!!
            }

            val nextNode = nodes[index + 1]
            totalDistance += hypot(nextNode.position.x - node.position.x, nextNode.position.y - node.position.y).toDouble()
            distances.add(totalDistance)
        }

        tInterpolator.i = tSetPoints.keys.toTypedArray()
        tInterpolator.o = tSetPoints.values.toTypedArray()
        length = totalDistance

        val tVals = mutableListOf<Double>()
        val xVals = mutableListOf<Double>()
        val yVals = mutableListOf<Double>()

        val coordinates = nodes.map { it.position }

        for (i in coordinates.indices) {
            tVals.add(tInterpolator.evaluate(i))
            xVals.add(coordinates[i].x.toDouble())
            yVals.add(coordinates[i].y.toDouble())
        }

        xInterpolator.i = tVals.toTypedArray()
        yInterpolator.i = tVals.toTypedArray()
        xInterpolator.o = xVals.toTypedArray()
        yInterpolator.o = yVals.toTypedArray()

        xInterpolator.init()
        yInterpolator.init()
    }

    fun tOfNode(node: Node): Double {
        return tInterpolator.evaluate(nodes.indexOf(node))
    }

    fun insert(at: Node, node: Node) { // Insert node after at
        val atEdge = at.edges.find { it.collectionID.value == id.value }
        if (atEdge != null) {
            node.edges.add(Edge(id.duplicate(), Pair(node.id.duplicate(), atEdge.segment.second.duplicate())))
            atEdge.segment = Pair(at.id.duplicate(), node.id.duplicate())
        } else {
            at.edges.add(Edge(id.duplicate(), Pair(at.id.duplicate(), node.id.duplicate())))
        }

        nodes.add(nodes.indexOf(at), node)
    }

    fun duplicate(time: Int, animation: Animation): NodeCollectionSetPoint {
        // TODO this will not work with multiple NCs sharing one node because duplication will create new nodes for each one
        val newSetPoint = NodeCollectionSetPoint(time, NodeCollectionID(id.value))
        for (node in nodes) {
            newSetPoint.nodes.add(animation.newNode(node.position.x, node.position.y, time).apply { tSetPoint = node.tSetPoint })
        }
        for (index in 0..<newSetPoint.nodes.size - 1) {
            val node = newSetPoint.nodes[index]
            val nextNode = newSetPoint.nodes[index + 1]
            node.edges.add(Edge(id.duplicate(), Pair(node.id.duplicate(), nextNode.id.duplicate())).apply { updateScreenCoords(animation) })
        }
        return newSetPoint
    }

    fun delete(animation: Animation) {
        nodes.forEach {
            animation.nodeEdgeHandler.removeNode(it, false)
        }
    }
}