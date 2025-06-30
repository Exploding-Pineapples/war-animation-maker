package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.AbstractTypeSerializable
import com.badlogicgames.waranimationmaker.AnimationScreen
import com.badlogicgames.waranimationmaker.AreaColor
import com.badlogicgames.waranimationmaker.InputElement
import com.badlogicgames.waranimationmaker.interpolator.LinearInterpolatedFloat
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolator
import earcut4j.Earcut
import java.lang.reflect.Type

interface AnyEdgeCollectionContext : AbstractTypeSerializable, HasInputs {
    var nodes: MutableList<Node>
    var color: AreaColor

    fun init()
}

open class EdgeCollectionContext(@Transient override var nodes: MutableList<Node> = mutableListOf(), override var color: AreaColor = AreaColor.RED) : AnyEdgeCollectionContext {
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
    fun updateAny(time: Int, paused: Boolean, context: AnyEdgeCollectionContext)
    fun drawAny(drawer: Drawer, context: AnyEdgeCollectionContext)
}

open class EdgeCollectionStrategy<T : AnyEdgeCollectionContext> : AnyEdgeCollectionStrategy {
    open fun update(time: Int, context: T) {

    }

    open fun draw(drawer: Drawer, context: T) {
        context.nodes.forEach { drawer.drawAsSelected(it) }
    }

    override fun updateAny(time: Int, paused: Boolean, context: AnyEdgeCollectionContext) {
        update(time, context as T)
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

    fun update(context: AreaContext) {
        if (drawCoords == null) {
            drawCoords = mutableListOf()
        } else {
            drawCoords.clear()
        }

        val nodes = context.nodes

        for (index in 0..<nodes.lastIndex) {
            val edge = nodes[index].edges.find { it.segment.second.value == nodes[index + 1].id.value }!!
            drawCoords.addAll(edge.screenCoords.subList(0, edge.screenCoords.size - 1))
        }
        drawCoords.add(nodes.last().screenPosition)

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

    override fun updateAny(time: Int, paused: Boolean, context: AnyEdgeCollectionContext) {
        update(context as AreaContext)
    }

    override fun draw(drawer: Drawer, context: AreaContext) {
        drawer.drawArea(this, context)
    }

    override fun getAbstractType() = AreaStrategy::class.java
}

class LineStrategy : EdgeCollectionStrategy<LineContext>() {
    fun update(time: Int, paused: Boolean, context: LineContext) {
        if (!paused) context.alpha.update(time)

        val parameterVals = mutableListOf<Double>()
        val xVals = mutableListOf<Double>()
        val yVals = mutableListOf<Double>()

        val nodes = context.nodes

        if (nodes.isNotEmpty() && context.alpha.value > 0) {
            var parameter = 0.0
            var index = 0

            while (parameter <= 1.0) {
                parameterVals.add(parameter)
                xVals.add(nodes[index].screenPosition.x.toDouble())
                yVals.add(nodes[index].screenPosition.y.toDouble())

                index++
                parameter = index.toDouble() / (nodes.size - 1)
            }

            val xInterpolator = PCHIPInterpolator(parameterVals.toTypedArray(), xVals.toTypedArray())
            val yInterpolator = PCHIPInterpolator(parameterVals.toTypedArray(), yVals.toTypedArray())

            for (i in 0..<nodes.lastIndex) {
                for (edge in nodes[i].edges) {
                    if (!edge.death.value && edge.segment.second.value == nodes[i + 1].id.value) { // Update any edge that points towards the next node in the node collection to have the same interpolated points
                        edge.screenCoords.clear()
                        for (j in 0 until AnimationScreen.LINES_PER_NODE) {
                            edge.screenCoords.add(
                                Coordinate(
                                    xInterpolator.interpolateAt((i * ((nodes.size + 1.0) / nodes.size) + j.toDouble() / AnimationScreen.LINES_PER_NODE) / nodes.size)
                                        .toFloat(),
                                    yInterpolator.interpolateAt((i * ((nodes.size + 1.0) / nodes.size) + j.toDouble() / AnimationScreen.LINES_PER_NODE) / nodes.size)
                                        .toFloat()
                                )
                            )
                        }
                        edge.screenCoords.add(nodes[i + 1].screenPosition)
                    }
                }
            }
        }
    }

    override fun updateAny(time: Int, paused: Boolean, context: AnyEdgeCollectionContext) {
        update(time, paused, context as LineContext)
    }

    override fun draw(drawer: Drawer, context: LineContext) {
        drawer.drawLine(context)
    }

    override fun getAbstractType() = LineStrategy::class.java
}