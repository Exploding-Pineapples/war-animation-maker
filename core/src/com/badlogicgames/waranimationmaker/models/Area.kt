package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.InputElement
import earcut4j.Earcut

data class Area (override val id: AreaID) : NodeCollection() {
    @Transient override var inputElements: MutableList<InputElement<*>> = mutableListOf()
    override var alpha: Float = 0.2f
    @Transient var drawPoly: MutableList<FloatArray> = mutableListOf()

    fun update(time: Int, animation: Animation) { // Takes coordinates from drawCoords and turns it into an earcut polygon in drawPoly
        super.update(time)

        for (edge in edges) {
            drawCoords.add(animation.getNodeByID(edge.segment.first)!!.screenPosition)
            if (edge.screenCoords.isEmpty()) { // If there are no interpolated coordinates on the edge, see if there is a duplicate interpolated line edge to use
                val firstNode = animation.getNodeByID(edge.segment.first)
                val otherInterpolatedEdge = firstNode!!.edges.find { it != edge && it.segment.second.value == edge.segment.second.value }
                if (otherInterpolatedEdge != null) {
                    drawCoords.addAll(otherInterpolatedEdge.screenCoords)
                    edge.screenCoords = otherInterpolatedEdge.screenCoords
                } else {
                    edge.screenCoords = mutableListOf(animation.getNodeByID(edge.segment.first)!!.screenPosition, animation.getNodeByID(edge.segment.second)!!.screenPosition)
                }
            }
            drawCoords.addAll(edge.screenCoords.subList(0, edge.screenCoords.size - 1))
        }
        if (edges.isNotEmpty()) {
            drawCoords.add(animation.getNodeByID(edges.last().segment.second)!!.screenPosition)
        }

        val poly = drawCoords.flatMap { listOf(it.x.toDouble(), it.y.toDouble()) }.toDoubleArray()
        drawCoords.clear()

        val earcut = Earcut.earcut(poly) //turns polygon into series of triangles represented by polygon vertex indexes

        if (drawPoly == null) {
            drawPoly = mutableListOf()
        }
        drawPoly.clear()

        var j = 0
        while (j < earcut.size) {
            drawPoly.add(
                floatArrayOf(
                    poly[earcut[j] * 2].toFloat(),
                    poly[earcut[j] * 2 + 1].toFloat(),
                    poly[earcut[j + 1] * 2].toFloat(),
                    poly[earcut[j + 1] * 2 + 1].toFloat(),
                    poly[earcut[j + 2] * 2].toFloat(),
                    poly[earcut[j + 2] * 2 + 1].toFloat()
                )
            ) //3 pairs of floats represent a triangle
            j += 3
        }
    }

    override fun showInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor) {
        uiVisitor.show(verticalGroup, this)
    }

    fun draw(shapeRenderer: ShapeRenderer) {
        shapeRenderer.color = color.color
        for (triangle in drawPoly) {
            shapeRenderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5])
        }
    }
}