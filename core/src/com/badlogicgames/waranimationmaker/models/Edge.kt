package com.badlogicgames.waranimationmaker.models

import com.badlogicgames.waranimationmaker.interpolator.InterpolatedBoolean
import com.badlogicgames.waranimationmaker.interpolator.InterpolatedID
import org.joml.Vector2f

class Edge(val collectionID: InterpolatedID,
           var segment: Pair<NodeID, NodeID>,
           @Transient var screenCoords: MutableList<Coordinate> = mutableListOf(),
           override var death: InterpolatedBoolean = InterpolatedBoolean(false, 0)
) : HasDeath, ObjectClickable {
    override fun clicked(x: Float, y: Float): Boolean {
        if (screenCoords.size > 1) {
            var lowestDist = distanceFromPointToSegment(
                Vector2f(x, y),
                Vector2f(screenCoords[0].x, screenCoords[0].y),
                Vector2f(screenCoords[1].x, screenCoords[1].y),
            )
            for (i in 2..<screenCoords.size) {
                val dist = distanceFromPointToSegment(
                    Vector2f(x, y),
                    Vector2f(screenCoords[i - 1].x, screenCoords[i - 1].y),
                    Vector2f(screenCoords[i].x, screenCoords[i].y),
                    )
                if (dist < lowestDist) {
                    lowestDist = dist
                }
            }
            return lowestDist <= 10
        }
        return false
    }

    override fun toString(): String {
        return "Edge of collection ${collectionID.value} from node ${segment.first.value} to node ${segment.second.value}"
    }
    fun contains(nodeID: NodeID): Boolean {
        return  (nodeID.value == segment.first.value || nodeID.value == segment.second.value)
    }
    fun update(time: Int) {
        if (screenCoords == null) {
            screenCoords = mutableListOf()
        }
        death.update(time)
        collectionID.update(time)
    }
    fun shouldDraw(time: Int): Boolean {
        val firstTime = death.setPoints.keys.firstOrNull() ?: return false
        if (time < firstTime) {
            return false
        }
        return !death.value
    }

    companion object {
        fun distanceFromPointToSegment(point: Vector2f, a: Vector2f, b: Vector2f): Float {
            val ab = Vector2f(b).sub(a)
            val ap = Vector2f(point).sub(a)
            val t = ap.dot(ab) / ab.lengthSquared()

            return if (t < 0f) {
                // Closest to point A
                point.distance(a)
            } else if (t > 1f) {
                // Closest to point B
                point.distance(b)
            } else {
                // Closest to point on segment
                val projection = Vector2f(ab).mul(t).add(a)
                point.distance(projection)
            }
        }
    }
}