package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.AreaColor
import com.badlogicgames.waranimationmaker.InputElement
import com.badlogicgames.waranimationmaker.TextInput
import com.badlogicgames.waranimationmaker.interpolator.LinearInterpolatedFloat
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolatedFloat
import kotlin.math.min
import kotlin.math.sqrt

class Arrow(x: Float, y: Float, time: Int): ScreenObject(), ObjectWithAlpha, ObjectWithColor {
    override var position: Coordinate = Coordinate(x, y)
    override var xInterpolator = PCHIPInterpolatedFloat(x, time)
    override var yInterpolator = PCHIPInterpolatedFloat(y, time)
    override val alpha = LinearInterpolatedFloat(1f, time)
    override val initTime = time
    override val id: ID = NodeID(-1)
    override var color = AreaColor.RED
    var thickness = 10f
    @Transient
    override var inputElements: MutableList<InputElement<*>> = mutableListOf()

    override fun shouldDraw(time: Int): Boolean {
        return true
    }

    override fun showInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor) {
        uiVisitor.show(verticalGroup, this)
    }

    fun goToTime(time: Int, zoom: Float, cx: Float, cy: Float, paused: Boolean): Boolean {
        if (!paused) { alpha.update(time) }
        return super.goToTime(time, zoom, cx, cy)
    }

    override fun buildInputs() {
        super<ScreenObject>.buildInputs()
        super<ObjectWithAlpha>.buildInputs()
        super<ObjectWithColor>.buildInputs()

        inputElements.add(
            TextInput(null, { input ->
                if (input != null) {
                    thickness = input
                }
            }, label@{
                return@label thickness.toString()
            }, Float::class.java, "Set thickness")
        )
    }

    fun draw(shapeRenderer: ShapeRenderer, camera: OrthographicCamera, curTime: Int) {
        var previous = projectToScreen(Coordinate(
            xInterpolator.interpolator.interpolateAt(xInterpolator.setPoints.keys.first()),
            yInterpolator.interpolator.interpolateAt(xInterpolator.setPoints.keys.first())
        ), camera.zoom, camera.position.x, camera.position.y)

        shapeRenderer.color = color.color.apply { a = alpha.value }

        val endTime = min(curTime, xInterpolator.setPoints.keys.last())

        for (time in xInterpolator.setPoints.keys.first().toInt() ..endTime) { // Draws entire body of arrow
            val position = projectToScreen(Coordinate(xInterpolator.interpolator.interpolateAt(time), yInterpolator.interpolator.interpolateAt(time)), camera.zoom, camera.position.x, camera.position.y)
            shapeRenderer.rectLine(previous.x, previous.y, position.x, position.y, thickness)
            if (time == endTime) {
                val triangle = generateTriangle(previous, position, thickness * 2, thickness * 3)
                shapeRenderer.triangle(triangle[0].x, triangle[0].y, triangle[1].x, triangle[1].y, triangle[2].x, triangle[2].y)
            }
            previous = position
        }
    }

    fun generateTriangle(a: Coordinate, b: Coordinate, baseWidth: Float, height: Float): Array<Coordinate> {
        // Direction from A to B
        val dx: Float = b.x - a.x
        val dy: Float = b.y - a.y
        val length = sqrt(dx * dx + dy * dy)
        val ux = dx / length
        val uy = dy / length

        // Perpendicular direction (rotated 90°)
        val px = -uy
        val py = ux

        // Base endpoints, perpendicular to direction, centered at B
        val halfBase = baseWidth / 2.0f
        val p1 = Coordinate(b.x + px * halfBase, b.y + py * halfBase)
        val p2 = Coordinate(b.x - px * halfBase, b.y - py * halfBase)

        // Tip of triangle, extending in A→B direction from B
        val tip = Coordinate(b.x + ux * height, b.y + uy * height)

        return arrayOf(p1, p2, tip)
    }
}