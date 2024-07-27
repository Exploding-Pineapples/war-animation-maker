package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import earcut4j.Earcut
import java.util.*

data class Area (override val id: AreaID) : NodeCollection() {
    var orderOfLineSegments: SortedMap<Int, MutableList<LineSegment>> = sortedMapOf() // Stores the interpolated line segments to draw to
    override var alpha: Float = 0.2f
    @Transient var drawPoly: MutableList<FloatArray> = mutableListOf()

    override fun update() { // Takes coordinates from drawCoords and turns it into an earcut polygon in drawPoly
        super.update()

        if (orderOfLineSegments == null) {
            orderOfLineSegments = sortedMapOf()
        }

        val poly = mutableListOf<Double>()

        for (coordinate in drawCoords) {
            //flattens points into 1D array
            poly.add(coordinate.x.toDouble())
            poly.add(coordinate.y.toDouble())
        }

        drawCoords.clear()

        val earcut = Earcut.earcut(poly.toDoubleArray()) //turns polygon into series of triangles represented by polygon vertex indexes

        drawPoly = mutableListOf()

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

    fun draw(shapeRenderer: ShapeRenderer) {
        shapeRenderer.color = color.color
        for (triangle in drawPoly) {
            shapeRenderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5])
        }
    }

    override fun toString(): String {
        return orderOfLineSegments.toString()
    }
}