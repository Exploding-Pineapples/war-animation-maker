package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.AreaColor
import com.badlogicgames.waranimationmaker.InputElement
import com.badlogicgames.waranimationmaker.TextInput
import com.badlogicgames.waranimationmaker.interpolator.LinearInterpolatedFloat
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolatedFloat

class MapLabel(x: Float, y: Float, time: Int) : ScreenObject(), HasAlpha, HasColor {
    override var position: Coordinate = Coordinate(x, y)
    override var xInterpolator = PCHIPInterpolatedFloat(x, time)
    override var yInterpolator = PCHIPInterpolatedFloat(y, time)
    override val alpha = LinearInterpolatedFloat(1f, time)
    override val initTime = time
    var text = ""
    override var color = AreaColor.RED
    var size = 1f
    @Transient
    override var inputElements: MutableList<InputElement<*>> = mutableListOf()

    override fun showInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor) {
        uiVisitor.show(verticalGroup, this)
    }

    fun goToTime(time: Int, zoom: Float, cx: Float, cy: Float, paused: Boolean): Boolean {
        if (!paused) { alpha.update(time) }
        return super.goToTime(time, zoom, cx, cy)
    }

    override fun buildInputs() {
        super<ScreenObject>.buildInputs()
        super<HasColor>.buildInputs()
        super<HasAlpha>.buildInputs()

        inputElements.add(
            TextInput(null, { input ->
                if (input != null) {
                    if (input > 0) {
                        size = input
                    }
                }
            }, label@{
                return@label size.toString()
            }, Float::class.java, "Set size")
        )
        inputElements.add(TextInput(null, { input ->
            text = input ?: ""
        }, label@{
            return@label text
        }, String::class.java, "Set text"))
    }
}