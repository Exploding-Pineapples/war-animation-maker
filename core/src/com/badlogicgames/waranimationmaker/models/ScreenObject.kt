package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH
import com.badlogicgames.waranimationmaker.interpolator.InterpolatedBoolean
import kotlin.math.absoluteValue

abstract class ScreenObject : Object, ObjectWithScreenPosition, ObjectWithDeath, ObjectClickable {
    override var death: InterpolatedBoolean = InterpolatedBoolean(false, 0)
    var alpha: Float = 1f
    @Transient override var screenPosition: Coordinate = Coordinate(0f, 0f)

    override fun clicked(x: Float, y: Float): Boolean
    {
        return (x - screenPosition.x).absoluteValue <= 10 && (y - screenPosition.y).absoluteValue <= 10
    }

    fun goToTime(time: Int, zoom: Float, cx: Float, cy: Float): Boolean {
        super.goToTime(time)
        updateScreenPosition(zoom, cx, cy)
        alpha = (1f - (xInterpolator.setPoints.keys.first() - time) / 100).coerceIn(0f, 1f)
        death.update(time)
        var deathTime: Int? = null
        for (i in 0..<death.interpolator.x.size) { // Search for the last time the object was dead, used for fading out
            if (death.interpolator.y[i] && death.interpolator.x[i] >= time) {
                deathTime = death.interpolator.x[i]
            }
        }
        if (deathTime != null) {
            if (time > deathTime - 100) {
                alpha = ((deathTime - time) / 100f).coerceIn(0f, 1f)
            }
        }

        return shouldDraw(time)
    }

    private fun updateScreenPosition(zoom: Float, cx: Float, cy: Float) {
        if (screenPosition == null) { // Is null when animation is first opened because screenPosition is @Transient
            screenPosition = Coordinate(0f, 0f)
        }

        screenPosition.x = position.x * zoom - cx * (zoom - 1) + (DISPLAY_WIDTH / 2 - cx)
        screenPosition.y = position.y * zoom - cy * (zoom - 1) + (DISPLAY_HEIGHT / 2 - cy)
    }

    // Draw only if selected
    fun drawAsSelected(shapeRenderer: ShapeRenderer, animationMode: Boolean, currentZoom: Float, currentCX: Float, currentCY: Float) {
        if (animationMode) {
            shapeRenderer.color = Color.SKY
            for (time in xInterpolator.setPoints.keys.first().toInt()..xInterpolator.setPoints.keys.last().toInt() step 4) { // Draws entire path of the selected object over time
                val position = projectToScreen(Coordinate(xInterpolator.interpolator.interpolateAt(time), yInterpolator.interpolator.interpolateAt(time)), currentZoom, currentCX, currentCY)
                shapeRenderer.circle(position.x, position.y, 2f)
            }
            shapeRenderer.color = Color.PURPLE
            for (time in xInterpolator.setPoints.keys) { // Draws all set points of the selected object
                val position = projectToScreen(Coordinate(xInterpolator.interpolator.interpolateAt(time), yInterpolator.interpolator.interpolateAt(time)), currentZoom, currentCX, currentCY)
                shapeRenderer.circle(position.x, position.y, 4f)
            }
            shapeRenderer.color = Color.ORANGE
            shapeRenderer.rect(screenPosition.x - 6.0f, screenPosition.y - 6.0f, 12f, 12f) // Draws an orange square to symbolize being selected
        }
    }

    override fun shouldDraw(time: Int): Boolean {
        if (time < xInterpolator.setPoints.keys.first()) {
            return false
        }
        return !death.value
    }

    override fun toString(): String {
        val output = StringBuilder()
        output.append("Movements: " + xInterpolator.setPoints.keys + "\n")
        output.append("       xs: " + xInterpolator.setPoints.values + "\n")
        output.append("       ys: " + yInterpolator.setPoints.values + "\n")
        return output.toString()
    }
}