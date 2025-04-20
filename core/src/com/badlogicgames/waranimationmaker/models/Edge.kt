package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogicgames.waranimationmaker.interpolator.InterpolatedBoolean
import com.badlogicgames.waranimationmaker.interpolator.InterpolatedID
import java.awt.geom.Line2D.ptSegDist

class Edge(val collectionID: InterpolatedID, var segment: Pair<NodeID, NodeID>, @Transient var screenCoords: MutableList<Coordinate> = mutableListOf(),
           override var death: InterpolatedBoolean = InterpolatedBoolean(false, 0)) : ObjectWithDeath, ObjectClickable {
    override fun clicked(x: Float, y: Float): Boolean {
        if (screenCoords.size > 1) {
            var lowestDist = ptSegDist(
                screenCoords[0].x.toDouble(),
                screenCoords[0].y.toDouble(),
                screenCoords[1].x.toDouble(),
                screenCoords[1].y.toDouble(),
                x.toDouble(),
                y.toDouble()
            )
            for (i in 2..<screenCoords.size) {
                val dist = ptSegDist(screenCoords[i - 1].x.toDouble(),
                    screenCoords[i - 1].y.toDouble(),
                    screenCoords[i].x.toDouble(),
                    screenCoords[i].y.toDouble(),
                    x.toDouble(),
                    y.toDouble())
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
        if (death == null) {
            death = InterpolatedBoolean(false, 0)
        }
        if (screenCoords == null) {
            screenCoords = mutableListOf()
        }
        death.update(time)
        collectionID.update(time)
    }
    fun drawAsSelected(shapeRenderer: ShapeRenderer, animationMode: Boolean) {
        if (animationMode) {
            for (i in 0..<screenCoords.size - 1) {
                shapeRenderer.rectLine(screenCoords[i].x, screenCoords[i].y, screenCoords[i + 1].x, screenCoords[i + 1].y, 5f)
            }
        }
    }
}