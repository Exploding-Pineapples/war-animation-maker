package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.InputElement
import com.badlogicgames.waranimationmaker.interpolator.InterpolatedBoolean
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolatedFloat

data class Node(
    override var position: Coordinate,
    override val initTime: Int,
    override val id: NodeID
) : ScreenObject(), HasDeath, HasID  {
    var color: Color = Color.GREEN
    override var xInterpolator = PCHIPInterpolatedFloat(position.x, initTime)
    override var yInterpolator = PCHIPInterpolatedFloat(position.y, initTime)
    override var death = InterpolatedBoolean(false, 0)
    @Transient override var inputElements: MutableList<InputElement<*>> = mutableListOf()
    @Transient var visitedBy = mutableListOf<EdgeCollectionID>()
    var edges = mutableListOf<Edge>()

    override fun showInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor) {
        uiVisitor.show(verticalGroup, this)
    }

    init {
        screenPosition = Coordinate(0f, 0f)
    }

    fun update(time: Int, camera: OrthographicCamera) { // Goes to time, and if animation mode is active, draws colored circle
        if (edges == null) { // Edges not serialized
            edges = mutableListOf()
        }
        if (visitedBy == null) { // Visited by not serialized
            visitedBy = mutableListOf()
        }
        visitedBy.clear() // Clear to prepare to be traversed
        color = if (goToTime(time, camera.zoom, camera.position.x, camera.position.y)) {
            Color.GREEN
        } else {
            Color.YELLOW
        }
        death.update(time)
        if (death.value) {
            color = Color.RED
        }
    }

    fun draw(shapeRenderer: ShapeRenderer) {

    }
}