package com.badlogicgames.waranimationmaker.models

import com.badlogicgames.waranimationmaker.AreaColor
import com.badlogicgames.waranimationmaker.TextInput

abstract class NodeCollection : HasInputs {
    abstract var alpha: Float
    @Transient var edges: MutableList<Edge> = mutableListOf()
    @Transient var drawCoords: MutableList<Coordinate> = mutableListOf()
    abstract val id: NodeCollectionID
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
        for (index in edges.indices) {
            if (edges[index].segment.first.value == id.value) {
                return index
            }
        }
        return -1
    }

    fun insert(index: Int, time: Int, nodeID: NodeID, animation: Animation) {
        if (index == 0) { // If inserting to the beginning, make the node have an edge that points to
            animation.getNodeByID(nodeID)?.edges?.add(Edge(id, Pair(nodeID, edges[1].segment.first)))
        } else {
            if (index == edges.size - 1) {
                val lastNode = animation.getNodeByID(edges.last().segment.second)
                lastNode?.edges?.add(Edge(id, Pair(lastNode.id, nodeID)))
            }
        }
    }

    open fun update(time: Int) {
        if (drawCoords == null) {
            drawCoords = mutableListOf()
        }
    }
}