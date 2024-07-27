package com.badlogicgames.waranimationmaker.models

import com.badlogicgames.waranimationmaker.AreaColor
import com.badlogicgames.waranimationmaker.InputElement

abstract class NodeCollection : HasInputs {
    abstract var alpha: Float
    val nodeIDs: MutableList<NodeID> = mutableListOf()
    @Transient var drawCoords: MutableList<Coordinate> = mutableListOf()
    abstract val id: ID
    @Transient override var inputElements: MutableList<InputElement<*>> = mutableListOf()
    var color: AreaColor = AreaColor.RED

    override fun buildInputs() {
        super.buildInputs()

        inputElements.add(
            InputElement(null, { input ->
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

    fun indexOf(id: NodeID): Int {
        for (index in nodeIDs.indices) {
            if (nodeIDs[index].value == id.value) {
                return index
            }
        }
        return -1
    }

    open fun update() {
        if (drawCoords == null) {
            drawCoords = mutableListOf()
        }
    }
}