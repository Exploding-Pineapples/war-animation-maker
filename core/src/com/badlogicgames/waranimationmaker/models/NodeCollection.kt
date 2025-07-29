package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Array
import com.badlogicgames.waranimationmaker.AreaColor
import com.badlogicgames.waranimationmaker.InputElement
import com.badlogicgames.waranimationmaker.SelectBoxInput
import com.badlogicgames.waranimationmaker.interpolator.LinearInterpolatedFloat
import com.badlogicgames.waranimationmaker.interpolator.NodeCollectionInterpolator

open class NodeCollection(override val id: NodeCollectionID) : AnyObject, HasInputs, HasID, HasAlpha, HasColor, Clickable {
    override var alpha = LinearInterpolatedFloat(1f, 0)
    @Transient var interpolator: NodeCollectionInterpolator = NodeCollectionInterpolator()
    override var color: AreaColor = AreaColor.RED
    var type: String = "None"
    var width: Float? = null
    @Transient override var inputElements: MutableList<InputElement<*>> = mutableListOf()

    override fun buildInputs() {
        super<HasInputs>.buildInputs()
        super<HasAlpha>.buildInputs()
        super<HasColor>.buildInputs()

        inputElements.add(
            SelectBoxInput(null, { input ->
                type = input?: "None"
                if (type == "Line") {
                    width = 5f
                }
            }, label@{
                return@label type
            }, String::class.java, "Set node collection type", Array<String>().apply { add("Area", "Line") })
        )
    }

    fun init(initTime: Int) {
        alpha.update(initTime)
        if (interpolator == null) {
            interpolator = NodeCollectionInterpolator()
        }
    }

    override fun showInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor) {
        uiVisitor.show(verticalGroup, this)
    }

    override fun hideInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor) {
        super<HasInputs>.hideInputs(verticalGroup, uiVisitor)
        super<HasAlpha>.hideInputs(verticalGroup, uiVisitor)
    }

    fun update(time: Int, paused: Boolean) {
        if (!paused) {
            interpolator.updateInterpolationFunction()
            alpha.update(time)
        }
        interpolator.evaluate(time)
    }

    fun draw(drawer: Drawer, time: Int) {
        drawer.draw(this)
    }

    fun clicked(x: Float, y: Float, camera: Camera): Boolean {
        return clickedCoordinates(x, y, interpolator.coordinates.map { projectToScreen(it, camera.zoom, camera.position.x, camera.position.y) }.toTypedArray())
    }

    override fun clicked(x: Float, y: Float): Boolean {
        return false
    }
}