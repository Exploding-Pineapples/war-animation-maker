package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.InputElement
import com.badlogicgames.waranimationmaker.interpolator.InterpolatedFloat

data class Node(
    override var position: Coordinate,
    override val initTime: Int,
    override val id: NodeID
) : ScreenObject(), Object  {
    var color: Color = Color.GREEN
    override var xInterpolator = InterpolatedFloat(position.x, initTime)
    override var yInterpolator = InterpolatedFloat(position.y, initTime)
    @Transient override var inputElements: MutableList<InputElement<*>> = mutableListOf()
    @Transient var visitedBy = mutableListOf<NodeCollectionID>()
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
        color = if (goToTime(time, camera.zoom, camera.position.x, camera.position.y)) {
            Color.GREEN
        } else {
            Color.YELLOW
        }
        if (death.value) {
            color = Color.RED
        }
    }

    fun drawAsLineNode(shapeRenderer: ShapeRenderer) {
        shapeRenderer.color = color
        shapeRenderer.circle(screenPosition.x, screenPosition.y, 7.0f)
    }

    fun drawAsAreaNode(shapeRenderer: ShapeRenderer) {
        shapeRenderer.color = Color.BLUE
        shapeRenderer.circle(screenPosition.x, screenPosition.y, 7.0f)
    }
}