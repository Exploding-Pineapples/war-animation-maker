package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.InputElement
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolatedFloat
import kotlin.math.absoluteValue

data class Node(
    override var position: Coordinate,
    override val initTime: Int,
    override val id: NodeID
) : ScreenObject(), HasID  {
    var color: Color = Color.GREEN
    override var xInterpolator = PCHIPInterpolatedFloat(position.x, initTime)
    override var yInterpolator = PCHIPInterpolatedFloat(position.y, initTime)
    @Transient override var inputElements: MutableList<InputElement<*>> = mutableListOf()
    @Transient var visitedBy = mutableListOf<NodeCollectionID>()
    var edges = mutableListOf<Edge>()

    override fun showInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor) {
        uiVisitor.show(verticalGroup, this)
    }

    init {
        screenPosition = Coordinate(0f, 0f)
    }

    override fun clicked(x: Float, y: Float): Boolean
    {
        if (screenPosition == null) {
            return false
        }
        return (x - screenPosition.x).absoluteValue <= 10 && (y - screenPosition.y).absoluteValue <= 10
    }

    fun init() {
        if (edges == null) { // Edges not serialized
            edges = mutableListOf()
        }

        edges.forEach { it.prepare(initTime) }

        if (visitedBy == null) { // Visited by not serialized
            visitedBy = mutableListOf()
        }

        if (screenPosition == null) {
            screenPosition = Coordinate(0f, 0f)
        }
    }

    fun update(time: Int, camera: OrthographicCamera) { // Goes to time, and if animation mode is active, draws colored circle
        visitedBy.clear() // Clear to prepare to be traversed
        updateScreenPosition(camera.zoom, camera.position.x, camera.position.y)
        edges.forEach {
            it.prepare(time)
            it.screenCoords.clear()
        }
    }
}