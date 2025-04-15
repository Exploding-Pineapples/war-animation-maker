package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.AnimationScreen
import com.badlogicgames.waranimationmaker.InputElement
import com.badlogicgames.waranimationmaker.TextInput

data class Line(
    override val id: LineID
) : NodeCollection() {
    @Transient var xInterpolator = InterpolatedFloat(0.0f, 0)
    @Transient var yInterpolator = InterpolatedFloat(0.0f, 0)
    @Transient override var inputElements: MutableList<InputElement<*>> = mutableListOf()
    var lineThickness: Float = 5.0f
    override var alpha: Float = 1.0f

    override fun buildInputs() {
        super.buildInputs()

        inputElements.add(
            TextInput(null, { input ->
                if (input != null) {
                    lineThickness = input
                }
            }, label@{
                return@label lineThickness.toString()
            }, Float::class.java, "Set line width")
        )
    }

    override fun showInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor) {
        uiVisitor.show(verticalGroup, this)
    }

    fun update(time: Int, animation: Animation) {
        super.update(time)

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
                edges[i].interpolatedCoords.clear()
                for (j in 0 until AnimationScreen.LINES_PER_NODE) {
                    edges[i].interpolatedCoords.add(Coordinate(
                        xInterpolator.interpolator.interpolateAt(i * AnimationScreen.LINES_PER_NODE + j),
                        yInterpolator.interpolator.interpolateAt(i * AnimationScreen.LINES_PER_NODE + j)
                    ))
                }
                edges[i].interpolatedCoords.add(animation.getNodeByID(edges[i].segment.second)!!.screenPosition)
            }
        }
    }

    fun draw(shapeRenderer: ShapeRenderer) {
        if (edges.size >= AnimationScreen.MIN_LINE_SIZE) {
            shapeRenderer.color = color.color
            for (edge in edges) {
                for (i in 0 until edge.interpolatedCoords.size - 1)
                shapeRenderer.rectLine(
                    edge.interpolatedCoords[i].x,
                    edge.interpolatedCoords[i].y,
                    edge.interpolatedCoords[i + 1].x,
                    edge.interpolatedCoords[i + 1].y,
                    lineThickness
                )
            }
        }
    }
}