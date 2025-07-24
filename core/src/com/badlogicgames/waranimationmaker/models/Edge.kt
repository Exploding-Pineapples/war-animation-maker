package com.badlogicgames.waranimationmaker.models

import com.badlogicgames.waranimationmaker.interpolator.InterpolatedBoolean
import org.joml.Vector2f

class Edge(
    var collectionID: NodeCollectionID,
    var segment: Pair<NodeID, NodeID>,
    @Transient var screenCoords: MutableList<Coordinate> = mutableListOf(),
    override var death: InterpolatedBoolean = InterpolatedBoolean(false, 0)
) : HasDeath, ObjectClickable {

    override fun clicked(x: Float, y: Float): Boolean {
        return clickedCoordinates(x, y, screenCoords.toTypedArray())
    }

    override fun toString(): String {
        return "Edge of collection ${collectionID.value} from node ${segment.first.value} to node ${segment.second.value}"
    }
    fun contains(nodeID: NodeID): Boolean {
        return  (nodeID.value == segment.first.value || nodeID.value == segment.second.value)
    }
    fun prepare(time: Int) {
        if (screenCoords == null) {
            screenCoords = mutableListOf()
        }
        death.update(time)
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
        var lowestDist = distanceFromPointToSegment(
            Vector2f(x, y),
            Vector2f(coordinates[0].x, coordinates[0].y),
            Vector2f(coordinates[1].x, coordinates[1].y),
        )
        for (i in 2..<coordinates.size) {
            val dist = distanceFromPointToSegment(
                Vector2f(x, y),
                Vector2f(coordinates[i - 1].x, coordinates[i - 1].y),
                Vector2f(coordinates[i].x, coordinates[i].y),
            )
            if (dist < lowestDist) {
                lowestDist = dist
            }
        }
        return lowestDist <= 10
    }
    return false
}