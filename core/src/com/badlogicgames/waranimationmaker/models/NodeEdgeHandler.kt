package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.OrthographicCamera

class NodeEdgeHandler(val animation: Animation) {

    fun buildInputs() {
        animation.nodes.forEach { it.buildInputs() }
    }

    fun addNode(node: Node)
    {
        animation.nodes.add(node)
        animation.nodeId++
    }

    fun removeNode(removeNode: Node): Boolean
    {
        for (node in animation.nodes) {
            node.edges.removeIf { it.segment.second.value == removeNode.id.value } // Remove all edges that point to the node
        }
        removeNode.edges.clear()
        val result = animation.nodes.remove(removeNode)
        updateNodeCollections()
        return result
    }

    fun addEdge(fromNode: Node, toNode: Node, id: Int) {
        if (!fromNode.edges.map { it.collectionID.value }.contains(id) && fromNode.initTime == toNode.initTime) { // Adding an edge from a node that already has an edge with the same collectionID is not allowed
            fromNode.edges.add(
                Edge(
                    NodeCollectionID(id),
                    Pair(fromNode.id, toNode.id),
                )
            )
        }

        updateNodeCollections()
    }

    fun removeEdge(removeEdge: Edge) : Boolean {
        var removed = false
        for (node in animation.nodes) {
            if (node.edges.remove(removeEdge)) {
                removed = true
            }
        }

        updateNodeCollections()
        return removed
    }

    private fun traverse(node: Node, nodeCollections: MutableList<NodeCollectionSetPoint>, currentBranch: NodeCollectionSetPoint) {
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
                    nodeCollections.add(currentBranch.apply { nodes.add(node) })
                    return
                }
                nodeCollections.add(currentBranch)
                println("Warning: Ambiguous topology")
            }
            return
        }

        var reachedEnd = true

        node.visitedBy.add(currentBranch.id)

        for (edge in node.edges) { // Traverses every available edge from the node
            val nextNode = animation.getNodeByID(edge.segment.second)!!
            if (edge.collectionID.value == currentBranch.id.value && nextNode.initTime == currentBranch.time) { // If edge continues the Node Collection that is being constructed, then continue recursion with this branch
                reachedEnd = false
                traverse(nextNode, nodeCollections, currentBranch.apply { nodes.add(node) })
            }
        }

        if (reachedEnd && currentBranch.nodes.isNotEmpty()) { // If no edges continue the Node Collection that is being constructed, that means the end has been reached, so add the current branch and stop
            nodeCollections.add(currentBranch.apply { nodes.add(node) })
        }
    }

    fun updateNodeCollections() {
        val nodeCollectionSetPoints = mutableListOf<NodeCollectionSetPoint>()

        for (node in animation.nodes) { // Build all node collections in all time
            for (edge in node.edges) {
                traverse(
                    node,
                    nodeCollectionSetPoints,
                    NodeCollectionSetPoint(node.initTime, NodeCollectionID(edge.collectionID.value))
                )
            }
        }

        nodeCollectionSetPoints.removeIf { it.nodes.isEmpty() }

        val usedIDsAtTime = mutableListOf<Pair<Int, Int>>() // Keep track of duplicate IDs, but only if at the same time. Set points for the same Node Collection should have the same ID. Pair(time, id)
        for (i in 0..<nodeCollectionSetPoints.size) {
            val nodeCollectionSetPoint = nodeCollectionSetPoints[i]
            if (Pair(nodeCollectionSetPoint.time, nodeCollectionSetPoint.id.value) in usedIDsAtTime) {
                val newId = animation.nodeCollectionID
                animation.nodeCollectionID++
                nodeCollectionSetPoints[i] = NodeCollectionSetPoint(nodeCollectionSetPoint.time, NodeCollectionID(newId)).apply { nodes.addAll(nodeCollectionSetPoint.nodes) }
                println("Resolved duplicate ID ${nodeCollectionSetPoint.id.value}, New ID: ${nodeCollectionSetPoints[i].id.value}, new edge collection size: ${nodeCollectionSetPoints[i].nodes.size}")
                usedIDsAtTime.add(Pair(nodeCollectionSetPoint.time, newId))
            } else {
                usedIDsAtTime.add(Pair(nodeCollectionSetPoint.time, nodeCollectionSetPoint.id.value))
            }
        }

        for (nodeCollectionSetPoint in nodeCollectionSetPoints) {
            val existingNodeCollection = animation.getEdgeCollectionByID(nodeCollectionSetPoint.id)
            if (existingNodeCollection == null) { // Create new node collection if it does not exist
                nodeCollectionSetPoint.nodes.forEach { node ->
                    node.edges.filter { it.collectionID.value == nodeCollectionSetPoint.id.value }.forEach {
                        it.collectionID = NodeCollectionID(nodeCollectionSetPoint.id.value)
                        //it.prepare(time)
                    }
                }

                val newNodeCollection = NodeCollection(NodeCollectionID(animation.nodeCollectionID))
                animation.nodeCollectionID++
                println("Warning: Created node collection ${newNodeCollection.id.value}")
                newNodeCollection.interpolator.newSetPoint(nodeCollectionSetPoint.time, nodeCollectionSetPoint)
                animation.nodeCollections.add(newNodeCollection)
                newNodeCollection.buildInputs()
            } else {
                existingNodeCollection.interpolator.newSetPoint(nodeCollectionSetPoint.time, nodeCollectionSetPoint)
            }
        }
    }

    fun update(time: Int, camera: OrthographicCamera, paused: Boolean) {
        for (node in animation.nodes) { // Update all nodes and edges
            node.update(camera)
            if (time == node.initTime) {
                node.edges.forEach {
                    it.screenCoords.add(animation.getNodeByID(it.segment.first)!!.screenPosition)
                    it.screenCoords.add(animation.getNodeByID(it.segment.second)!!.screenPosition)
                    it.prepare()
                }
            }
        }

        animation.nodeCollections.forEach { it.update(time, paused) }
    }
}
