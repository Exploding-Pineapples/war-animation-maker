package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.AreaColor
import com.badlogicgames.waranimationmaker.InputElement
import com.badlogicgames.waranimationmaker.TextInput
import com.badlogicgames.waranimationmaker.interpolator.LinearInterpolatedFloat
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolatedFloat

class MapLabel(x: Float, y: Float, time: Int) : ScreenObject(), HasAlpha, ObjectWithColor {
    override var position: Coordinate = Coordinate(x, y)
    override var xInterpolator = PCHIPInterpolatedFloat(x, time)
    override var yInterpolator = PCHIPInterpolatedFloat(y, time)
    override val alpha = LinearInterpolatedFloat(1f, time)
    override val initTime = time
    var text = ""
    override var color = AreaColor.RED
    var size = 50f
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
        super<ObjectWithColor>.buildInputs()
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

    fun draw(batcher: SpriteBatch, shapeRenderer: ShapeRenderer, sizefactor: Float, font: BitmapFont, fontShader: ShaderProgram, layout: GlyphLayout) {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(color.color.r, color.color.g, color.color.b, alpha.value)
        shapeRenderer.circle(screenPosition.x, screenPosition.y, size * 10)
        shapeRenderer.end()

        batcher.setColor(1f, 1f, 1f, alpha.value)
        batcher.begin()

        font.color = Color(Color.WHITE.r, Color.WHITE.g, Color.WHITE.b, alpha.value)
        font.data.setScale(size)
        batcher.setShader(fontShader)
        fontShader.setUniformf("outlineDistance", 0.05f)
        fontShader.setUniformf("outlineColor", color.color.r, color.color.g, color.color.b, alpha.value)

        layout.setText(font, text)
        font.draw(batcher, layout, screenPosition.x - layout.width / 2, screenPosition.y + layout.height * (3f / 2) + size * 5)

        batcher.setShader(null)
        batcher.end()
    }
}