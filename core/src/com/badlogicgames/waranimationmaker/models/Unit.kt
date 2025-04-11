package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.*

data class Unit(
    override val id: UnitID,
    override var position: Coordinate,
    override val initTime: Int,
    var image: String,
) : ScreenObject() {
    override var xInterpolator: InterpolatedFloat = InterpolatedFloat(position.x, initTime)
    override var yInterpolator: InterpolatedFloat = InterpolatedFloat(position.y, initTime)
    @Transient override var inputElements: MutableList<InputElement<*>> = mutableListOf()

    override fun showInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor) {
        uiVisitor.show(verticalGroup ,this)
    }

    var color: AreaColor = AreaColor.BLUE
    var name: String? = null
    var type: String = "infantry.png"
    var size: String = "XX"

    companion object {
        val sizePresets = mapOf(
            "XX" to 1.0f,
            "X" to 0.75f,
            "III" to 0.5f,
        )
    }
    @Transient private var typeTexture: Texture? = null
    @Transient var countryTexture: Texture? = null
    private var width: Float = AnimationScreen.DEFAULT_UNIT_WIDTH.toFloat()
    private var height: Float = AnimationScreen.DEFAULT_UNIT_HEIGHT.toFloat()

    override fun buildInputs() {
        super.buildInputs()

        inputElements.add(TextInput(null, { input ->
            if (input != null) {
                size = input
            }
        }, label@{
            return@label size
        }, String::class.java, "Set size"))
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
            if (input != null) {
                name = input
            }
        }, label@{
            return@label name
        }, String::class.java, "Set name"))
        inputElements.add(TextInput(null, { input ->
            if (input != null) {
                for (color in AreaColor.entries) {
                    if (input == color.name) {
                        this.color = color
                    }
                }
            }
        }, label@{
            return@label color.name
        }, String::class.java, "Set color"))
    }

    override fun clicked(x: Float, y: Float): Boolean {
        return ((x in (screenPosition.x - width * 0.8f)..(screenPosition.x + width * 0.8f)) && (y in (screenPosition.y - height * 0.45f)..(screenPosition.y + height * 0.45f)))
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

    fun draw(batcher: SpriteBatch, shapeRenderer: ShapeRenderer, sizefactor: Float, font: BitmapFont, fontShader: ShaderProgram, layout: GlyphLayout) {
        // Draw only for the correct country
        var sizePresetFactor = 1.0f
        if (size in sizePresets) {
            sizePresetFactor = sizePresets[size]!!
        }
        width = AnimationScreen.DEFAULT_UNIT_WIDTH * sizefactor * sizePresetFactor
        height = AnimationScreen.DEFAULT_UNIT_HEIGHT * sizefactor * sizePresetFactor


        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        shapeRenderer.color = Color(color.color.r, color.color.g, color.color.b, alpha)
        shapeRenderer.rect(screenPosition.x - width * 0.8f, screenPosition.y - height * 0.45f, width * 1.6f, height * 0.9f)
        shapeRenderer.color = Color(Color.LIGHT_GRAY.r, Color.LIGHT_GRAY.g, Color.LIGHT_GRAY.b, alpha)
        val padding = 0.75f
        shapeRenderer.rect(screenPosition.x - width * 0.8f + padding * sizefactor * sizePresetFactor,
            screenPosition.y - height * 0.45f + padding * sizefactor * sizePresetFactor,
            width * 1.6f - 2 * padding * sizefactor * sizePresetFactor,
            height * 0.9f - 2 * padding  * sizefactor * sizePresetFactor)

        shapeRenderer.end()

        batcher.begin()

        batcher.setColor(1f, 1f, 1f, alpha)
        if (typeTexture() != null) {
            batcher.draw(typeTexture, screenPosition.x, screenPosition.y - height / 3, width / 1.5f, height / 1.5f)
        }
        if (countryTexture() != null) {
            batcher.draw(countryTexture, screenPosition.x - width / 1.5f, screenPosition.y - height / 3, width / 1.5f, height / 1.5f)
        }

        if (color == null) {
            color = AreaColor.BLUE
        }

        font.color = Color(Color.WHITE.r, Color.WHITE.g, Color.WHITE.b, alpha)
        font.data.setScale(0.12f * sizefactor * sizePresetFactor)
        batcher.setShader(fontShader)
        fontShader.setUniformf("scale", 0.12f * sizefactor * sizePresetFactor)
        fontShader.setUniformf("outlineDistance", 0.2f)
        fontShader.setUniformf("outlineColor", color.color.r, color.color.g, color.color.b, alpha)
        layout.setText(font, size)

        font.draw(batcher, layout, screenPosition.x - layout.width / 2, screenPosition.y + height / 2 + layout.height)
        if (name != null) {
            layout.setText(font, name)
            font.draw(batcher, name, screenPosition.x - layout.width / 2, screenPosition.y - height / 2)
        }
        batcher.setShader(null)

        batcher.end()
    }
}