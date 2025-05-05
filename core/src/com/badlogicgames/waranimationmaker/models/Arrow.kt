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

class Arrow(x: Float, y: Float, time: Int): ScreenObject(), HasAlpha, ObjectWithColor {
    override var position: Coordinate = Coordinate(x, y)
    override var xInterpolator = PCHIPInterpolatedFloat(x, time)
    override var yInterpolator = PCHIPInterpolatedFloat(y, time)
    override val alpha = LinearInterpolatedFloat(1f, time)
    override val initTime = time
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
        super<HasAlpha>.buildInputs()
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

    }
}