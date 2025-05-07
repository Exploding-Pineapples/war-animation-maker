package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.Assets
import com.badlogicgames.waranimationmaker.InputElement
import com.badlogicgames.waranimationmaker.SelectBoxInput
import com.badlogicgames.waranimationmaker.TextInput
import com.badlogicgames.waranimationmaker.interpolator.LinearInterpolatedFloat
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolatedFloat

class Image(x: Float, y: Float, time: Int, var path: String) : ScreenObject(), HasAlpha {
    override var position: Coordinate = Coordinate(x, y)
    override var xInterpolator = PCHIPInterpolatedFloat(x, time)
    override var yInterpolator = PCHIPInterpolatedFloat(y, time)
    override var alpha = LinearInterpolatedFloat(1f, time)
    @Transient override var inputElements: MutableList<InputElement<*>> = mutableListOf()
    override val initTime = time
    var scale: Float = 1f

    @Transient var texture: Texture? = Assets.loadTexture(path)

    override fun showInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor) {
        uiVisitor.show(verticalGroup, this)
    }

    fun loadTexture() {
        if (path == "") {
            println("Image path is empty")
        }
        texture = Assets.loadTexture(path)
        println("updated image path to $texture")
    }

    fun updateTexture(newPath: String) {
        path = newPath
        loadTexture()
    }

    fun goToTime(time: Int, zoom: Float, cx: Float, cy: Float, paused: Boolean): Boolean {
        if (!paused) { alpha.update(time); println(alpha.value) }
        return super.goToTime(time, zoom, cx, cy)
    }

    override fun buildInputs() {
        super<ScreenObject>.buildInputs()
        super<HasAlpha>.buildInputs()

        inputElements.add(SelectBoxInput(null, { input ->
            updateTexture(Assets.mapsPath(input))
        }, label@{
            return@label path.substringAfter("assets/maps/")
        }, String::class.java, "Image", Assets.images()))
        inputElements.add(
            TextInput(null, { input ->
                if (input != null) {
                    if (input >= 0) {
                        scale = input
                    }
                }
            }, label@{
                return@label scale.toString()
            }, Float::class.java, "Set scale")
        )
    }
}