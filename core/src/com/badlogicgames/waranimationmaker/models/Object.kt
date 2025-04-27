package com.badlogicgames.waranimationmaker.models

import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolatedFloat

interface Object : HasInputs {
    var position: Coordinate
    var xInterpolator: PCHIPInterpolatedFloat
    var yInterpolator: PCHIPInterpolatedFloat
    val initTime: Int

    val id: ID

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