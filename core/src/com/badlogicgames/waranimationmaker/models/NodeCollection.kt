package com.badlogicgames.waranimationmaker.models

import com.badlogicgames.waranimationmaker.AreaColor
import com.badlogicgames.waranimationmaker.TextInput

abstract class NodeCollection : HasInputs {
    abstract var alpha: Float
    var nodeIDInterpolators: MutableList<InterpolatedID> = mutableListOf()
    var nodeIDs: MutableList<NodeID> = mutableListOf()
    @Transient var drawCoords: MutableList<Coordinate> = mutableListOf()
    abstract val id: ID
    var color: AreaColor = AreaColor.RED

    override fun buildInputs() {
        super.buildInputs()

        inputElements.add(
            TextInput(null, { input ->
                if (input != null) {
                    for (color in AreaColor.entries) {
                        if (input == color.name) {
                            this.color = color
                        }
                    }
                }
            }, label@{
                return@label color.name
            }, String::class.java, "Set color")
        )
    }

    fun indexOf(id: NodeID): Int {
        for (index in nodeIDs.indices) {
            if (nodeIDInterpolators[index].value.value == id.value) {
                return index
            }
        }
        return -1
    }

    fun add(index: Int, time: Int, nodeID: NodeID) {
        nodeIDInterpolators.add(index, InterpolatedID(time, nodeID))
        nodeIDs.add(index, nodeID)
    }

    open fun update(time: Int) {
        if (drawCoords == null) {
            drawCoords = mutableListOf()
            println("initialized drawcoords")
        }

        if (nodeIDInterpolators == null) {
            nodeIDInterpolators = nodeIDs.map { InterpolatedID(0, it)}.toMutableList()
            println("initialized nodeIDInterpolatorss")
        }

        if (nodeIDInterpolators.size != nodeIDs.size) {
            nodeIDInterpolators = nodeIDs.map { InterpolatedID(0, it)}.toMutableList()
            //println("filled interpolators" + nodeIDInterpolators.get(0).setPoints)
        }

        for (index in nodeIDs.indices) {
            nodeIDInterpolators[index].update(time)
        }
    }


}