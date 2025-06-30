package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogicgames.waranimationmaker.interpolator.InterpolatedBoolean
import com.badlogicgames.waranimationmaker.interpolator.InterpolatedID

class EdgeHandler(val animation: Animation) {

    fun buildInputs() {
        for (node in animation.nodes) {
            node.buildInputs()
        }
    }

    fun add(node: Node)
    {
        animation.nodes[animation.nodeId] = node
        animation.nodeId++
    }

    fun addEdge(fromNode: Node, toNode: Node, time: Int, id: Int) {
        if (!fromNode.edges.map { it.collectionID.value }.contains(id)) { // Adding an edge from a node that already has an edge with the same collectionID is not allowed
            fromNode.edges.add(
                Edge(
                    InterpolatedID(time, id),
                    Pair(fromNode.id, toNode.id),
                    death = InterpolatedBoolean(false, time)
                )
            )
        }
    }

    fun remove(removeNode: Node): Boolean
    {
        for (node in animation.nodes) {
            node.edges.removeIf { it.segment.second.value == removeNode.id.value } // Remove all edges that point to the node
        }
        removeNode.edges.clear()
        return animation.nodes.remove(removeNode)
    }

    fun removeEdge(removeEdge: Edge) : Boolean {
        var removed = false
        for (node in animation.nodes) {
            if (node.edges.remove(removeEdge)) {
                removed = true
            }
        }
        return removed
    }

    private fun traverse(node: Node, nodeCollections: MutableList<NodeCollection>, currentBranch: NodeCollection, time: Int) {
        val visited = (node.visitedBy.find { (it.value == currentBranch.id.value) } != null)
        if (visited) {
            val matchingNodeCollections = nodeCollections.filter { it.id.value == currentBranch.id.value }
            for (nodeCollection in matchingNodeCollections) {
                if (node.id.value == nodeCollection.nodes.first().id.value) { // If the current node is the first node of an existing Node Collection with the same CollectionID, this branch is part of that Node Collection, so add this branch at the beginning of it
                    nodeCollection.nodes.addAll(0, currentBranch.nodes)
                    return
                }
            }
            if (currentBranch.nodes.isNotEmpty()) {
                if (node.id.value == currentBranch.nodes.first().id.value) { // If the current node is the first node of the current branch, it is forming a loop, so add it to the list
                    nodeCollections.add(currentBranch)
                    return
                }
                nodeCollections.add(currentBranch)
                println("Warning: Ambiguous topology")
            }
            return
        }

        var reachedEnd = true

        node.visitedBy.add(currentBranch.id)

        if (node.shouldDraw(time)) {
            for (edge in node.edges) { // Traverses every available edge from the node
                val nextNode = animation.getNodeByID(edge.segment.second)!!
                if (nextNode.shouldDraw(time) && !edge.death.value) {
                    if (edge.collectionID.value == currentBranch.id.value) { // If edge continues the Node Collection that is being constructed, then continue recursion with this branch
                        reachedEnd = false
                        traverse(nextNode, nodeCollections, currentBranch.apply { nodes.add(node) }, time)
                    }
                }
            }
        }

        if (reachedEnd && currentBranch.nodes.isNotEmpty()) { // If no edges continue the Node Collection that is being constructed, that means the end has been reached, so add the current branch and stop
            nodeCollections.add(currentBranch)
        }
    }

    fun update(time: Int, camera: OrthographicCamera, paused: Boolean) {
        val nodeCollections = mutableListOf<NodeCollection>()
        for (node in animation.nodes) { // Update all nodes and edges
            node.update(time, camera)
            node.edges.forEach {
                it.update(time)
                it.screenCoords.clear()
                it.screenCoords.add(animation.getNodeByID(it.segment.first)!!.screenPosition)
                it.screenCoords.add(animation.getNodeByID(it.segment.second)!!.screenPosition)
            }
        }
        for (node in animation.nodes) { // Build all edge collections from edges
            if (node.shouldDraw(time)) {
                for (edge in node.edges) {
                    traverse(
                        node,
                        nodeCollections,
                        NodeCollection(EdgeCollectionID(edge.collectionID.value)),
                        time)
                }
            }
        }

        nodeCollections.removeIf { it.nodes.isEmpty() }
        animation.nodeCollections.forEach { it.prepare() }

        val usedIDs = mutableListOf<Int>()
        for (i in 0..<nodeCollections.size) {
            val nodeCollection = nodeCollections[i]
            if (nodeCollection.id.value in usedIDs) {
                val newId = animation.getEdgeCollectionId()
                nodeCollection.nodes.forEach { node ->
                    node.edges.filter { it.collectionID.value == nodeCollection.id.value }.forEach {
                        it.collectionID.newSetPoint(time, newId); it.collectionID.update(time)
                        it.update(time)
                    }
                }
                nodeCollections[i] = NodeCollection(EdgeCollectionID(newId)).apply { nodes.addAll(nodeCollection.nodes) }
                println("Resolved duplicate ID ${nodeCollection.id.value}, New ID: ${nodeCollections[i].id.value}, new edge collection size: ${nodeCollections[i].nodes.size}")
                usedIDs.add(newId)
            } else {
                usedIDs.add(nodeCollection.id.value)
            }
        }

        for (nodeCollection in nodeCollections) {
            val existingNodeCollection = animation.getEdgeCollectionByID(nodeCollection.id)
            if (existingNodeCollection == null) { // Create new edge collection if it does not exist
                nodeCollection.nodes.forEach { node ->
                    node.edges.filter { it.collectionID.value == nodeCollection.id.value }.forEach {
                        it.collectionID.newSetPoint(time, nodeCollection.id.value); it.collectionID.update(time)
                        it.update(time)
                    }
                }
                println("Warning: Created edge collection ${nodeCollection.id.value}")
                animation.nodeCollections.add(nodeCollection)
                nodeCollection.buildInputs()
            } else {
                existingNodeCollection.nodes.addAll(nodeCollection.nodes)
            }
        }
        //println("Added edge collections of size: " + animation.edgeCollections.map {it.edges.size})

        animation.nodeCollections.filter { it.edgeCollectionStrategy.javaClass == LineStrategy::class.java }.forEach {
            it.update(time, paused)
        } // Lines must be updated first so that areas can use their interpolated edges
        animation.nodeCollections.filter { it.edgeCollectionStrategy.javaClass == AreaStrategy::class.java }.forEach {
            it.update(time, paused)
        }
    }
}
