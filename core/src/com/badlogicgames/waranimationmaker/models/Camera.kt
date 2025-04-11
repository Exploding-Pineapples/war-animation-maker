package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.InputElement

data class Camera(
    override var position: Coordinate = Coordinate(x = 960.0f, y = 540.0f),
    override var zoom: Float = 1.0f,
    override val initTime: Int
) : ScreenObject(), ObjectWithZoom, ObjectWithScreenPosition {
    override val id: ID = NodeID(-1)
    override var xInterpolator: InterpolatedFloat = InterpolatedFloat(position.x, initTime)
    override var yInterpolator: InterpolatedFloat = InterpolatedFloat(position.y, initTime)
    override var zoomInterpolator: InterpolatedFloat = InterpolatedFloat(zoom, initTime)
    @Transient override var inputElements: MutableList<InputElement<*>> = mutableListOf()

    override fun goToTime(time: Int): Boolean {
        super.goToTime(time, zoom, position.x, position.y) // Call ScreenObject's goToTime to set screen position
        if (zoomInterpolator == null) {
            zoomInterpolator = InterpolatedFloat(zoom, initTime)
        }
        zoom = zoomInterpolator.update(time)
        return true
    }

    override fun holdPositionUntil(time: Int) {  // Create a new movement that keeps the object at its last defined position until the current time
        super.holdPositionUntil(time)
        zoomInterpolator.holdValueUntil(time)
    }

    override fun showInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor) {
        uiVisitor.show(verticalGroup, this)
    }

    override fun removeFrame(time: Int): Boolean {
        val zoomResult = zoomInterpolator.removeFrame(time)
        val positionResult = super.removeFrame(time)
        return zoomResult || positionResult // If either a zoom or position frame is removed it is a success
    }

    fun newSetPoint(time: Int, x: Float, y: Float, zoom: Float) {
        super.newSetPoint(time, x, y)
        zoomInterpolator.newSetPoint(time, zoom)
    }
}