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
        println("building line inpuits")
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

        for (index in nodeIDs.indices) {
            val node = animation.getNodeByID(nodeIDs[index])
            if (node != null) {
                xInterpolator.setPoints[index * AnimationScreen.LINES_PER_NODE] = node.screenPosition.x
                yInterpolator.setPoints[index * AnimationScreen.LINES_PER_NODE] = node.screenPosition.y
            }
        }
        xInterpolator.updateInterpolator()
        yInterpolator.updateInterpolator()
    }

    fun draw(shapeRenderer: ShapeRenderer) {
        if (nodeIDs.size >= AnimationScreen.MIN_LINE_SIZE) {
            shapeRenderer.color = color.color
            for (i in 0 until AnimationScreen.LINES_PER_NODE * nodeIDs.size) {
                shapeRenderer.rectLine(
                    xInterpolator.interpolator.interpolateAt(i),
                    yInterpolator.interpolator.interpolateAt(i),
                    xInterpolator.interpolator.interpolateAt(i + 1),
                    yInterpolator.interpolator.interpolateAt(i + 1),
                    lineThickness
                )
            }
        }
    }
}