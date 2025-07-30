package com.badlogicgames.waranimationmaker.models

import org.joml.Vector2f

class Edge(
    var collectionID: NodeCollectionID,
    var segment: Pair<NodeID, NodeID>,
    @Transient var screenCoords: MutableList<Coordinate> = mutableListOf(),
) : AnyObject, Clickable {

    override fun clicked(x: Float, y: Float): Boolean {
        return clickedCoordinates(x, y, screenCoords.toTypedArray())
    }

    override fun toString(): String {
        return "Edge of collection ${collectionID.value} from node ${segment.first.value} to node ${segment.second.value}"
    }
    fun contains(nodeID: NodeID): Boolean {
        return  (nodeID.value == segment.first.value || nodeID.value == segment.second.value)
    }
    fun prepare() {
        if (screenCoords == null) {
            screenCoords = mutableListOf()
        }
    }

    fun updateScreenCoords(animation: Animation) {
        screenCoords.clear()
        screenCoords.add(animation.getNodeByID(segment.first)!!.screenPosition)
        screenCoords.add(animation.getNodeByID(segment.second)!!.screenPosition)
    }
}

fun distanceFromPointToSegment(point: Vector2f, a: Vector2f, b: Vector2f): Float { // ChatGPT wrote this
    val ab = Vector2f(b).sub(a)
    val ap = Vector2f(point).sub(a)
    val t = ap.dot(ab) / ab.lengthSquared()

    return if (t < 0f) {
        point.distance(a)
    } else if (t > 1f) {
        point.distance(b)
    } else {
        val projection = Vector2f(ab).mul(t).add(a)
        point.distance(projection)
    }
}

fun clickedCoordinates(x: Float, y: Float, coordinates: Array<Coordinate>): Boolean {
    if (coordinates.isNotEmpty()) {
        for (i in 1..coordinates.lastIndex) {
            val dist = distanceFromPointToSegment(
                Vector2f(x, y),
                Vector2f(coordinates[i - 1].x, coordinates[i - 1].y),
                Vector2f(coordinates[i].x, coordinates[i].y),
            )
            if (dist <= 10) {
                return true
            }
        }
    }
    return false
}