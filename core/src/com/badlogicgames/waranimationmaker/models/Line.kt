package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogicgames.waranimationmaker.AnimationScreen
import com.badlogicgames.waranimationmaker.AreaColor
import com.badlogicgames.waranimationmaker.InputElement

data class Line(
    override val id: LineID
) : NodeCollection() {
    var xInterpolator = InterpolatedFloat(0.0f, 0)
    var yInterpolator = InterpolatedFloat(0.0f, 0)
    var lineThickness: Float = 5.0f
    override var alpha: Float = 1.0f

    override fun buildInputs(skin: Skin) {
        super.buildInputs(skin)

        inputElements.add(
            InputElement(skin, { input ->
                if (input != null) {
                    lineThickness = input
                }
            }, label@{
                return@label lineThickness.toString()
            }, Float::class.java, "Set line width")
        )
    }

    fun update(animation: Animation) {
        super.update()
        lineThickness = 5.0f
        color = AreaColor.RED
        //reset values to nothing by default

        for (index in nodeIDs.indices) {
            val node = animation.getNodeByID(nodeIDs[index])
            if (node != null) {
                xInterpolator.setPoints[index * AnimationScreen.LINES_PER_NODE.toDouble()] = node.screenPosition.x.toDouble()
                yInterpolator.setPoints[index * AnimationScreen.LINES_PER_NODE.toDouble()] = node.screenPosition.y.toDouble()
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
                    xInterpolator.interpolator.interpolateAt(i.toDouble()).toFloat(),
                    yInterpolator.interpolator.interpolateAt(i.toDouble()).toFloat(),
                    xInterpolator.interpolator.interpolateAt(i + 1.0).toFloat(),
                    yInterpolator.interpolator.interpolateAt(i + 1.0).toFloat(),
                    lineThickness
                )
            }
        }
    }
}