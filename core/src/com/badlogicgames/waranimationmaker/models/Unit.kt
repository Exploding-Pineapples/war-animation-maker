package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.*
import com.badlogicgames.waranimationmaker.interpolator.LinearInterpolatedFloat
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolatedFloat

data class Unit(
    override var position: Coordinate,
    override val initTime: Int,
    var image: String = ""
) : ScreenObject(), HasAlpha, HasColor {
    override var xInterpolator: PCHIPInterpolatedFloat = PCHIPInterpolatedFloat(position.x, initTime)
    override var yInterpolator: PCHIPInterpolatedFloat = PCHIPInterpolatedFloat(position.y, initTime)
    override val alpha: LinearInterpolatedFloat = LinearInterpolatedFloat(1f, initTime)
    @Transient override var inputElements: MutableList<InputElement<*>> = mutableListOf()

    override var color: AreaColor = AreaColor.BLUE
    var name: String? = null
    var type: String = "infantry.png"
    var size: String = "XX"
    var drawSize: Float? = 1.0f
    @Transient private var typeTexture: Texture? = null
    @Transient var countryTexture: Texture? = null
    @Transient var width: Float = AnimationScreen.DEFAULT_UNIT_WIDTH.toFloat()
    @Transient var height: Float = AnimationScreen.DEFAULT_UNIT_HEIGHT.toFloat()

    override fun showInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor) {
        uiVisitor.show(verticalGroup ,this)
    }

    fun init(initTime: Int) {
        if (color == null) {
            color = AreaColor.BLUE
        }

        alpha.update(initTime)
    }

    override fun buildInputs() {
        super<ScreenObject>.buildInputs()
        super<HasAlpha>.buildInputs()
        super<HasColor>.buildInputs()

        inputElements.add(TextInput(null, { input ->
            size = input?: ""
        }, label@{
            return@label size
        }, String::class.java, "Set size"))
        inputElements.add(TextInput(null, { input ->
            drawSize = if (input != null && input != 0f) {
                input
            } else {
                null
            }
        }, label@{
            return@label drawSize.toString()
        }, Float::class.java, "Set draw size"))
        inputElements.add(SelectBoxInput(null, { input ->
            if (input != null) {
                type = input
                updateTypeTexture()
            }
        }, label@{
            println("inputting: $type")
            return@label type
        }, String::class.java, "Set type", Assets.unitTypes()))
        inputElements.add(SelectBoxInput(null, { input ->
            image = Assets.flagsPath(input)
            updateCountryTexture()
        }, label@{
            return@label image.substringAfter("assets/flags/")
        }, String::class.java, "Set country", Assets.countryNames))
        inputElements.add(TextInput(null, { input ->
            name = input ?: ""
        }, label@{
            return@label name
        }, String::class.java, "Set name"))
    }

    override fun clicked(x: Float, y: Float): Boolean {
        return ((x in (screenPosition.x - width * 0.5f)..(screenPosition.x + width * 0.5f)) && (y in (screenPosition.y - height * 0.5f)..(screenPosition.y + height * 0.5f)))
    }

    fun goToTime(time: Int, zoom: Float, cx: Float, cy: Float, paused: Boolean): Boolean {
        if (!paused) { alpha.update(time) }
        return super.goToTime(time, zoom, cx, cy)
    }

    fun updateCountryTexture() {
        countryTexture = Assets.loadTexture(image)
    }

    fun countryTexture(): Texture? {
        if (countryTexture == null) {
            updateCountryTexture()
        }
        return countryTexture
    }

    fun updateTypeTexture() {
        typeTexture = Assets.loadTexture(Assets.unitKindsPath(type))
    }

    fun typeTexture(): Texture? {
        if (typeTexture == null) {
            updateTypeTexture()
        }
        return typeTexture
    }

    companion object {
        val sizePresets = mapOf(
            "XX" to 1.0f,
            "X" to 0.8f,
            "III" to 0.7f,
            "II" to 0.55f,
            "I" to 0.5f
        )
    }
}