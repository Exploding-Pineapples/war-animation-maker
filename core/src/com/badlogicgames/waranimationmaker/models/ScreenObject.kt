package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH
import kotlin.math.absoluteValue

abstract class ScreenObject : Object, HasScreenPosition, ObjectClickable, HasInputs {
    @Transient
    override var screenPosition: Coordinate = Coordinate(0f, 0f)

    override fun clicked(x: Float, y: Float): Boolean
    {
        return (x - screenPosition.x).absoluteValue <= 10 && (y - screenPosition.y).absoluteValue <= 10
    }

    open fun goToTime(time: Int, zoom: Float, cx: Float, cy: Float): Boolean {
        super.goToTime(time)
        updateScreenPosition(zoom, cx, cy)
        return shouldDraw(time)
    }

    protected fun updateScreenPosition(zoom: Float, cx: Float, cy: Float) {
        if (screenPosition == null) { // Is null when animation is first opened because screenPosition is @Transient
            screenPosition = Coordinate(0f, 0f)
        }

        screenPosition.x = position.x * zoom - cx * (zoom - 1) + (DISPLAY_WIDTH / 2 - cx)
        screenPosition.y = position.y * zoom - cy * (zoom - 1) + (DISPLAY_HEIGHT / 2 - cy)
    }

    override fun shouldDraw(time: Int): Boolean {
        return time >= xInterpolator.setPoints.keys.first() - 100
    }

    override fun toString(): String {
        val output = StringBuilder()
        output.append("Movements: " + xInterpolator.setPoints.keys + "\n")
        output.append("       xs: " + xInterpolator.setPoints.values + "\n")
        output.append("       ys: " + yInterpolator.setPoints.values + "\n")
        return output.toString()
    }
}