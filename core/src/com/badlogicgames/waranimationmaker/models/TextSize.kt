package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.g2d.BitmapFont

data class TextSize(val width: Float, val height: Float)

fun measureText(font: BitmapFont, text: String): TextSize {
    val scaleX = font.data.scaleX
    val scaleY = font.data.scaleY
    var width = 0f
    var maxHeight = 0f

    for (char in text) {
        val glyph = font.data.getGlyph(char)
        if (glyph != null) {
            width += glyph.xadvance * scaleX
            val glyphHeight = glyph.height * scaleY
            if (glyphHeight > maxHeight) maxHeight = glyphHeight
        }
    }

    return TextSize(width, maxHeight)
}