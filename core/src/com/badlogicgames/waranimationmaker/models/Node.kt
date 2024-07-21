package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

data class Node(
    override var position: Coordinate,
    override val initTime: Int,
) : ScreenObject(), Object  {
    var color: Color = Color.GREEN
    override var xInterpolator = InterpolatedFloat(position.x, initTime)
    override var yInterpolator = InterpolatedFloat(position.y, initTime)

    init {
        screenPosition = Coordinate(0f, 0f)
    }

    fun update(time: Int, camera: OrthographicCamera) { // Goes to time, and if animation mode is active, draws colored circle
        color = if (goToTime(time, camera.zoom, camera.position.x, camera.position.y)) {
            Color.GREEN
        } else {
            Color.YELLOW
        }
        if (death != null) {
            if (time > death!!) {
                color = Color.RED
            }
        }
    }

    fun draw(shapeRenderer: ShapeRenderer, animationMode: Boolean) {
        if (animationMode) {
            shapeRenderer.color = color
            shapeRenderer.circle(screenPosition.x, screenPosition.y, 7.0f)
        }
    }
}