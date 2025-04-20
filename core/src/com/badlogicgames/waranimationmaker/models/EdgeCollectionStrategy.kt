package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogicgames.waranimationmaker.AbstractTypeSerializable
import com.badlogicgames.waranimationmaker.AnimationScreen
import com.badlogicgames.waranimationmaker.AreaColor
import com.badlogicgames.waranimationmaker.interpolator.InterpolatedFloat
import earcut4j.Earcut
import java.lang.reflect.Type

open class EdgeCollectionContext(@Transient var edges: MutableList<Edge> = mutableListOf(), var color: AreaColor = AreaColor.RED) : AbstractTypeSerializable {
    override fun getAbstractType(): Type {
        return EdgeCollectionContext::class.java
    }
}

class AreaContext : EdgeCollectionContext() {
    override fun getAbstractType(): Type {
        return AreaContext::class.java
    }
}

class LineContext(var width: Float = 5.0f) : EdgeCollectionContext() {
    @Transient
    var xInterpolator = InterpolatedFloat(0.0f, 0)
    @Transient
    var yInterpolator = InterpolatedFloat(0.0f, 0)
    override fun getAbstractType(): Type {
        return LineContext::class.java
    }
}

interface AnyEdgeCollectionStrategy : AbstractTypeSerializable {
    fun updateAny(time: Int, animation: Animation, context: EdgeCollectionContext)
    fun drawAny(shapeRenderer: ShapeRenderer, context: EdgeCollectionContext)
}

open class EdgeCollectionStrategy<T : EdgeCollectionContext> : AnyEdgeCollectionStrategy {
    open fun update(time: Int, animation: Animation, context: T) {
        for (edge in context.edges) {
            edge.screenCoords.clear()
            edge.screenCoords.add(animation.getNodeByID(edge.segment.first)!!.screenPosition)
            edge.screenCoords.add(animation.getNodeByID(edge.segment.second)!!.screenPosition)
        }
    }
    open fun draw(shapeRenderer: ShapeRenderer, context: T) {
        context.edges.forEach { it.drawAsSelected(shapeRenderer, true) }
    }

    override fun updateAny(time: Int, animation: Animation, context: EdgeCollectionContext) {
        update(time, animation, context as T)
    }

    override fun drawAny(shapeRenderer: ShapeRenderer, context: EdgeCollectionContext) {
        draw(shapeRenderer, context as T)
    }

    override fun getAbstractType():Class<*> = EdgeCollectionStrategy::class.java
}

class AreaStrategy : EdgeCollectionStrategy<AreaContext>() {
    @Transient
    var drawCoords: MutableList<Coordinate> = mutableListOf()
    @Transient
    var drawPoly: MutableList<FloatArray> = mutableListOf()
    override fun update(time: Int, animation: Animation, context: AreaContext) {
        if (drawCoords == null) {
            drawCoords = mutableListOf()
        } else {
            drawCoords.clear()
        }

        val edges = context.edges

        for (edge in edges) {
            if (!edge.death.value) {
                drawCoords.add(animation.getNodeByID(edge.segment.first)!!.screenPosition)
                if (edge.screenCoords.size <= 2) { // If there are no interpolated coordinates on the edge, see if there is a duplicate interpolated line edge to use
                    val firstNode = animation.getNodeByID(edge.segment.first)
                    val otherInterpolatedEdge = firstNode!!.edges.find { (it != edge && it.segment.second.value == edge.segment.second.value) && it.screenCoords.size > 2 }
                    if (otherInterpolatedEdge != null) {
                        edge.screenCoords = otherInterpolatedEdge.screenCoords
                    } else {
                        edge.screenCoords = mutableListOf(
                            animation.getNodeByID(edge.segment.first)!!.screenPosition,
                            animation.getNodeByID(edge.segment.second)!!.screenPosition
                        )
                    }
                }
                drawCoords.addAll(edge.screenCoords.subList(0, edge.screenCoords.size - 1)) // Exclude second node of each edge since it is the first node of the next edge
            }
        }
        if (edges.isNotEmpty() && !edges.last().death.value) {
            drawCoords.add(animation.getNodeByID(edges.last().segment.second)!!.screenPosition) // Add the second node of the last edge since there is no next edge
        }

        val poly = drawCoords.flatMap { listOf(it.x.toDouble(), it.y.toDouble()) }.toDoubleArray()

        val earcut = Earcut.earcut(poly) //turns polygon into series of triangles represented by polygon vertex indexes

        if (drawPoly == null) {
            drawPoly = mutableListOf()
        } else {
            drawPoly.clear()
        }

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

    override fun draw(shapeRenderer: ShapeRenderer, context: AreaContext) {
        shapeRenderer.color = context.color.color
        for (triangle in drawPoly) {
            shapeRenderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5])
        }
    }

    override fun getAbstractType() = AreaStrategy::class.java
}

class LineStrategy : EdgeCollectionStrategy<LineContext>() {
    override fun update(time: Int, animation: Animation, context: LineContext) {
        var xInterpolator = context.xInterpolator
        var yInterpolator = context.yInterpolator
        val edges = context.edges

        if (xInterpolator == null) {
            xInterpolator = InterpolatedFloat(0.0f, 0)
            yInterpolator = InterpolatedFloat(0.0f, 0)
        }

        var node: Node?
        for (index in edges.indices) {
            node = animation.getNodeByID(edges[index].segment.first)
            if (node != null) {
                xInterpolator.setPoints[index * AnimationScreen.LINES_PER_NODE] = node.screenPosition.x
                yInterpolator.setPoints[index * AnimationScreen.LINES_PER_NODE] = node.screenPosition.y
            }
            if (index == edges.size - 1) {
                node = animation.getNodeByID(edges[index].segment.second)
                if (node != null) {
                    xInterpolator.setPoints[(index + 1) * AnimationScreen.LINES_PER_NODE] = node.screenPosition.x
                    yInterpolator.setPoints[(index + 1) * AnimationScreen.LINES_PER_NODE] = node.screenPosition.y
                }
            }
        }

        xInterpolator.updateInterpolator()
        yInterpolator.updateInterpolator()

        if (edges.size >= AnimationScreen.MIN_LINE_SIZE) {
            for (i in edges.indices) {
                val edge = edges[i]
                edge.screenCoords.clear()
                for (j in 0 until AnimationScreen.LINES_PER_NODE) {
                    edges[i].screenCoords.add(Coordinate(
                        xInterpolator.interpolator.interpolateAt(i * AnimationScreen.LINES_PER_NODE + j),
                        yInterpolator.interpolator.interpolateAt(i * AnimationScreen.LINES_PER_NODE + j)
                    ))
                }
                edge.screenCoords.add(animation.getNodeByID(edge.segment.second)!!.screenPosition)
            }
        }
    }

    override fun draw(shapeRenderer: ShapeRenderer, context: LineContext) {
        val edges = context.edges
        if (edges.size >= AnimationScreen.MIN_LINE_SIZE) {
            shapeRenderer.color = context.color.color
            for (edge in edges) {
                for (i in 0 until edge.screenCoords.size - 1)
                    shapeRenderer.rectLine(
                        edge.screenCoords[i].x,
                        edge.screenCoords[i].y,
                        edge.screenCoords[i + 1].x,
                        edge.screenCoords[i + 1].y,
                        context.width
                    )
            }
        }
    }

    override fun getAbstractType() = LineStrategy::class.java
}