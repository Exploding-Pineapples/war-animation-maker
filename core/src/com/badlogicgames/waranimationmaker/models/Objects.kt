package com.badlogicgames.waranimationmaker.models

import com.badlogicgames.waranimationmaker.AreaColor
import com.badlogicgames.waranimationmaker.TextInput
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH
import com.badlogicgames.waranimationmaker.interpolator.InterpolatedValue
import com.badlogicgames.waranimationmaker.interpolator.LinearInterpolatedFloat
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolatedFloat
import kotlin.math.absoluteValue

interface AnyObject

interface HasPosition {
    var position: Coordinate
}

interface HasScreenPosition : HasPosition {
    var screenPosition: Coordinate

    fun updateScreenPosition(zoom: Float, cx: Float, cy: Float) {
        if (screenPosition == null) { // Is null when animation is first opened because screenPosition is @Transient
            screenPosition = Coordinate(0f, 0f)
        }

        screenPosition.x = position.x * zoom - cx * (zoom - 1) + (DISPLAY_WIDTH / 2 - cx)
        screenPosition.y = position.y * zoom - cy * (zoom - 1) + (DISPLAY_HEIGHT / 2 - cy)
    }
}

interface InterpolatedObject : AnyObject, HasPosition {
    val initTime: Int
    var xInterpolator: PCHIPInterpolatedFloat
    var yInterpolator: PCHIPInterpolatedFloat

    fun goToTime(time: Int): Boolean { // Can only be called after at least one key frame has been added
        if (xInterpolator == null) {
            xInterpolator = PCHIPInterpolatedFloat(position.x, initTime)
            yInterpolator = PCHIPInterpolatedFloat(position.y, initTime)
        }
        position.x = xInterpolator.update(time)
        position.y = yInterpolator.update(time)

        return true
    }

    fun shouldDraw(time: Int): Boolean

    fun removeFrame(time: Int): Boolean {
        return xInterpolator.removeFrame(time) && yInterpolator.removeFrame(time) // Both should be paired
    }

    fun newSetPoint(time: Int, x: Float, y: Float) {
        xInterpolator.newSetPoint(time, x)
        yInterpolator.newSetPoint(time, y)
    }

    // When you add a time coordinate pair to an object which hasn't had a defined movement for a long time, it will interpolate a motion the whole way, which can be undesirable
    // Ex. last defined position was at time 0, you want it to move to another position at 800
    // But you only want it to move starting from time 600
    // The below function is used hold the object at the last position until the desired time
    fun holdPositionUntil(time: Int) {  // Create a new movement that keeps the object at its last defined position until the current time
        xInterpolator.holdValueUntil(time)
        yInterpolator.holdValueUntil(time)
    }
}

abstract class ScreenObject : InterpolatedObject, HasScreenPosition, Clickable, HasInputs {
    @Transient override var screenPosition: Coordinate = Coordinate(0f, 0f)

    override fun clicked(x: Float, y: Float): Boolean
    {
        return (x - screenPosition.x).absoluteValue <= 10 && (y - screenPosition.y).absoluteValue <= 10
    }

    open fun goToTime(time: Int, zoom: Float, cx: Float, cy: Float): Boolean {
        super.goToTime(time)
        updateScreenPosition(zoom, cx, cy)
        return shouldDraw(time)
    }

    override fun shouldDraw(time: Int): Boolean {
        return time >= xInterpolator.setPoints.keys.first()
    }

    override fun toString(): String {
        val output = StringBuilder()
        output.append("Movements: " + xInterpolator.setPoints.keys + "\n")
        output.append("       xs: " + xInterpolator.setPoints.values + "\n")
        output.append("       ys: " + yInterpolator.setPoints.values + "\n")
        return output.toString()
    }
}

interface HasID {
    val id: ID
}

interface HasZoom {
    var zoom: Float
    var zoomInterpolator: PCHIPInterpolatedFloat
}

interface HasAlpha : HasInputs {
    val alpha: LinearInterpolatedFloat

    override fun buildInputs() {
        inputElements.add(TextInput(null, { input ->
            if (input != null) {
                alpha.value = input
            }
        }, label@{
            return@label alpha.value.toString()
        }, Float::class.java, "Set alpha set point"))
    }
}

interface HasColor : HasInputs {
    var color: AreaColor

    override fun buildInputs() {
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
}

interface Clickable {
    fun clicked(x: Float, y: Float) : Boolean
}

