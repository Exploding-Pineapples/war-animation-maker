package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogicgames.waranimationmaker.AreaColor
import earcut4j.Earcut
import java.util.ArrayList

data class Area (
    override val nodes: MutableList<Node>
) : NodeCollection() {
    var color: AreaColor = AreaColor.RED
    var lineIDAndOrder: List<Pair<Int, Int>> = mutableListOf() // Stores the lineIDs which correspond to lines and an integer which represents at what index in nodes should the line's points be inserted
    override var alpha: Float = 0.2f
    @Transient var drawPoly: MutableList<FloatArray> = mutableListOf()

    fun calculatePolygon(time: Int, animation: Animation) {
        // Converts lineIDs into Line objects from the lineIDAndOrder list
        val convertedLineIDs: MutableList<Pair<Line, Int>> = ArrayList()

        if (lineIDAndOrder == null) {
            lineIDAndOrder = mutableListOf()
        }

        for ((first, second) in lineIDAndOrder) {
            val line = animation.getLineByID(first)

            if (line != null) {
                convertedLineIDs.add(Pair(line, second))
            } else {
                println("Line with ID $first not found")
            }
        }

        val border1D = DoubleArray(nodes.size * 2)
        var poly = DoubleArray(0)

        var n = 0
        updateDrawNodes(time)
        updateNonDrawNodes(time)
        while (n < drawNodes.size) {
            val node = nodes[n]
            border1D[2 * n] = node.screenPosition.x.toDouble()
            border1D[2 * n + 1] = node.screenPosition.y.toDouble()
            n++
        }

        var lastBorderIndex = 0
        for (lineIntPair in convertedLineIDs) {
            //flattens interpolatedX and interpolatedY points into 1D array
            val line: Line = lineIntPair.first
            val linePoly = DoubleArray(line.interpolatedX.size * 2)
            for (i in line.interpolatedX.indices) {
                linePoly[i * 2] = line.interpolatedX[i].toDouble()
                linePoly[i * 2 + 1] = line.interpolatedY[i].toDouble()
            }

            poly += border1D.slice(lastBorderIndex until lineIntPair.second * 2)
            lastBorderIndex = lineIntPair.second * 2
            poly += linePoly
        }
        poly += border1D.slice(lastBorderIndex until border1D.size)

        val earcut = Earcut.earcut(poly) //turns polygon into series of triangles represented by polygon vertex indexes

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

    fun update(time: Int, zoom: Float, cx: Float, cy: Float, animation: Animation) {
        for (node in nodes) {
            node.goToTime(time, zoom, cx, cy)
        }
        calculatePolygon(time, animation)
    }

    fun draw(shapeRenderer: ShapeRenderer) {
        shapeRenderer.color = color.color
        for (triangle in drawPoly) {
            shapeRenderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5])
        }
    }
}