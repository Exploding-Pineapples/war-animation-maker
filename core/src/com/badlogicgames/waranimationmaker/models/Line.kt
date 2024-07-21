package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogicgames.waranimationmaker.AnimationScreen
import com.badlogicgames.waranimationmaker.AreaColor
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolator

data class Line(
    val id: Int,
    override val nodes: MutableList<Node> = mutableListOf(),
) : NodeCollection() {
    @Transient var interpolatedX: Array<Float> = arrayOf()
    @Transient var interpolatedY: Array<Float> = arrayOf()
    var lineThickness: Float = 5.0f
    var color: AreaColor = AreaColor.RED
    override var alpha: Float = 1.0f

    fun update(linesPerNode: Int, time: Int) : Boolean {
        lineThickness = 5.0f
        color = AreaColor.RED
        //reset values to nothing by default
        interpolatedX = arrayOf()
        interpolatedY = arrayOf()

        updateDrawNodes(time)
        updateNonDrawNodes(time)
        val numLines = drawNodes.size * linesPerNode

        val xValues = DoubleArray(drawNodes.size)
        val yValues = DoubleArray(drawNodes.size)
        val evalAt = DoubleArray(drawNodes.size)

        var i = 0
        while (i < drawNodes.size) {
            evalAt[i] = i.toDouble() //numbers from 0 - drawNodes.size() are used as interpolation points
            i += 1
        }

        var node: Node
        for (nodeIndex in drawNodes.indices) {
            node = drawNodes[nodeIndex]
            xValues[nodeIndex] = node.screenPosition.x.toDouble()
            yValues[nodeIndex] = node.screenPosition.y.toDouble()
        }

        if (drawNodes.size >= AnimationScreen.MIN_LINE_SIZE) {
            interpolatedX = Array(numLines + 1) { 0.0f }
            interpolatedY = Array(numLines + 1) { 0.0f }

            val xInterpolator = PCHIPInterpolator(evalAt, xValues)
            val yInterpolator = PCHIPInterpolator(evalAt, yValues)

            i = 0
            var eval: Double
            while (i < numLines) {
                eval = (drawNodes.size - 1.00) * i / numLines
                interpolatedX[i] = xInterpolator.interpolateAt(eval).toFloat()
                interpolatedY[i] = yInterpolator.interpolateAt(eval).toFloat()
                i++
            }

            interpolatedX[numLines] = xInterpolator.interpolateAt((drawNodes.size.toFloat() - 1.00)).toFloat()
            interpolatedY[numLines] = yInterpolator.interpolateAt((drawNodes.size.toFloat() - 1.00)).toFloat()
            return true
        }
        return false
    }

    fun draw(shapeRenderer: ShapeRenderer) {
        if (drawNodes.size >= AnimationScreen.MIN_LINE_SIZE) {
            shapeRenderer.color = color.color
            for (i in 0 until AnimationScreen.LINES_PER_NODE * drawNodes.size) {
                shapeRenderer.rectLine(
                    interpolatedX[i],
                    interpolatedY[i],
                    interpolatedX[i + 1],
                    interpolatedY[i + 1],
                    lineThickness
                )
            }
        }
    }
}