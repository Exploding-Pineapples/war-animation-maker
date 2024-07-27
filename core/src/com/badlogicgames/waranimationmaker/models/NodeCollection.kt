package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogicgames.waranimationmaker.AreaColor
import com.badlogicgames.waranimationmaker.InputElement

abstract class NodeCollection : HasInputs {
    abstract var alpha: Float
    val nodeIDs: MutableList<NodeID> = mutableListOf()
    @Transient var drawCoords: MutableList<Coordinate> = mutableListOf()
    abstract val id: ID
    override var inputElements: MutableList<InputElement<*>> = mutableListOf()
    var color: AreaColor = AreaColor.RED

    open fun buildInputs(skin: Skin) {
        inputElements.add(
            InputElement(skin, { input ->
                if (input != null) {
                    for (color in AreaColor.entries) {
                        if (input == color.name) {
                            this.color = color
                        }
                    }
                }
            }, label@{
                return@label color.name
            }, String::class.java, "Set node collection color")
        )
    }

    open fun update() {
        if (drawCoords == null) {
            drawCoords = mutableListOf()
        }
    }
}