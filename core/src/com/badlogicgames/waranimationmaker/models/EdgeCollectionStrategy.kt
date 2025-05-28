package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.AbstractTypeSerializable
import com.badlogicgames.waranimationmaker.AnimationScreen
import com.badlogicgames.waranimationmaker.AreaColor
import com.badlogicgames.waranimationmaker.InputElement
import com.badlogicgames.waranimationmaker.interpolator.LinearInterpolatedFloat
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolatedFloat
import earcut4j.Earcut
import java.lang.reflect.Type

interface AnyEdgeCollectionContext : AbstractTypeSerializable, HasInputs {
    var edges: MutableList<Edge>
    var color: AreaColor

    fun init()
}

open class EdgeCollectionContext(@Transient override var edges: MutableList<Edge> = mutableListOf(), override var color: AreaColor = AreaColor.RED) : AnyEdgeCollectionContext {
    override fun init() {}

    override fun getAbstractType(): Type {
        return EdgeCollectionContext::class.java
    }

    override var inputElements: MutableList<InputElement<*>> = mutableListOf()

    override fun showInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor) {
    }
}

class AreaContext : EdgeCollectionContext() {
    override fun init() {}
    override fun getAbstractType(): Type {
        return AreaContext::class.java
    }
}

class LineContext(var width: Float = 5.0f) : EdgeCollectionContext(), HasAlpha {
    @Transient var xInterpolator = PCHIPInterpolatedFloat(0.0f, 0)
    @Transient var yInterpolator = PCHIPInterpolatedFloat(0.0f, 0)
    override var alpha = LinearInterpolatedFloat(1f, 0)
    @Transient override var inputElements: MutableList<InputElement<*>> = mutableListOf()

    override fun getAbstractType(): Type {
        return LineContext::class.java
    }

    init {
        init()
    }

    override fun init() {
        super<EdgeCollectionContext>.buildInputs()
        super<HasAlpha>.buildInputs()

        if (alpha == null) {
            alpha = LinearInterpolatedFloat(1f, 0)
        }
        alpha.update(0)
    }

    override fun showInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor) {
        uiVisitor.show(verticalGroup, this)
    }
}

interface AnyEdgeCollectionStrategy : AbstractTypeSerializable {
    fun updateAny(time: Int, animation: Animation, paused: Boolean, context: AnyEdgeCollectionContext)
    fun drawAny(drawer: Drawer, context: AnyEdgeCollectionContext)
}

open class EdgeCollectionStrategy<T : AnyEdgeCollectionContext> : AnyEdgeCollectionStrategy {
    open fun update(time: Int, animation: Animation, context: T) {
        for (edge in context.edges) {
            edge.screenCoords.clear()
            edge.screenCoords.add(animation.getNodeByID(edge.segment.first)!!.screenPosition)
            edge.screenCoords.add(animation.getNodeByID(edge.segment.second)!!.screenPosition)
        }
    }

    open fun draw(drawer: Drawer, context: T) {
        context.edges.forEach { drawer.drawAsSelected(it) }
    }

    override fun updateAny(time: Int, animation: Animation, paused: Boolean, context: AnyEdgeCollectionContext) {
        update(time, animation, context as T)
    }

    override fun drawAny(drawer: Drawer, context: AnyEdgeCollectionContext) {
        draw(drawer, context as T)
    }

    override fun getAbstractType():Class<*> = EdgeCollectionStrategy::class.java
}

class AreaStrategy : EdgeCollectionStrategy<AreaContext>() {
    @Transient
    var drawCoords: MutableList<Coordinate> = mutableListOf()
    @Transient
    var drawPoly: MutableList<FloatArray> = mutableListOf()

    fun update(animation: Animation, context: AreaContext) {
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

    override fun draw(drawer: Drawer, context: AreaContext) {
        drawer.drawArea(this, context)
    }

    override fun getAbstractType() = AreaStrategy::class.java
}

class LineStrategy : EdgeCollectionStrategy<LineContext>() {
    fun update(time: Int, animation: Animation, paused: Boolean, context: LineContext) {
        if (!paused) context.alpha.update(time)

        var xInterpolator = context.xInterpolator
        var yInterpolator = context.yInterpolator
        val edges = context.edges

        if (xInterpolator == null) {
            xInterpolator = PCHIPInterpolatedFloat(0.0f, 0)
            yInterpolator = PCHIPInterpolatedFloat(0.0f, 0)
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

    override fun updateAny(time: Int, animation: Animation, paused: Boolean, context: AnyEdgeCollectionContext) {
        update(time, animation, paused, context as LineContext)
    }

    override fun draw(drawer: Drawer, context: LineContext) {
        drawer.drawLine(context)
    }

    override fun getAbstractType() = LineStrategy::class.java
}