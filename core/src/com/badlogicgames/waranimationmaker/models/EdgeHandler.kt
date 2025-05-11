package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH


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

    private fun traverse(node: Node, edgeCollections: MutableList<EdgeCollection>, currentBranch: EdgeCollection, time: Int) {
        val visited = (node.visitedBy.find { (it.value == currentBranch.id.value && it.javaClass == currentBranch.id::class.java) } != null)
        if (visited) {
            //println("visited: $visited, this id: ${currentBranch.javaClass.simpleName} ${currentBranch.id.value}, those ids: ${node.visitedBy.map { it.value }}")
            val matchingNodeCollections = edgeCollections.filter { it.id.value == currentBranch.id.value }
            //println("Matching collections: " + matchingNodeCollections.map { it.javaClass.simpleName + " ${it.id.value}" })
            for (nodeCollection in matchingNodeCollections) {
                //println("Matching id collection found for ${currentBranch.javaClass.simpleName} ${currentBranch.id.value}")
                //println("This node id value: ${node.id.value} First id value: ${nodeCollection.edges.first().segment.first.value}")
                if (node.id.value == nodeCollection.edges.first().segment.first.value) { // If the current node is the first node of an existing Node Collection with the same CollectionID, this branch is part of that Node Collection, so add this branch at the beginning of it
                    //println("${currentBranch.javaClass.simpleName} ${currentBranch.id.value} added to beginning of another node collection")
                    nodeCollection.edges.addAll(0, currentBranch.edges)
                    return
                }
            }
            if (currentBranch.edges.isNotEmpty()) {
                if (node.id.value == currentBranch.edges.first().segment.first.value) { // If the current node is the first node of the current branch, it is forming a loop, so add it to the list
                    //println("${currentBranch.javaClass.simpleName} ${currentBranch.id.value} ended by loop forming")
                    edgeCollections.add(currentBranch)
                } else {
                    //println("Not added: ${currentBranch.edges}")
                }
            } else {
                //println("Warning: ${currentBranch.javaClass.simpleName} ${currentBranch.id.value} is empty")
            }
            return
        }

        node.visitedBy.add(currentBranch.id)

        var reachedEnd = true

        if (node.shouldDraw(time)) {
            for (edge in node.edges) { // Traverses every available edge from the node
                val nextNode = animation.getNodeByID(edge.segment.second)!!
                if (nextNode.shouldDraw(time) && edge.shouldDraw(time)) {
                    if (edge.collectionID.value == currentBranch.id.value) { // If edge continues the Node Collection that is being constructed, then continue recursion with this branch
                        reachedEnd = false
                        traverse(nextNode, edgeCollections, currentBranch.apply { edges.add(edge) }, time)
                    }
                }
            }
        }

        if (reachedEnd && currentBranch.edges.isNotEmpty()) { // If no edges continue the Node Collection that is being constructed, that means the end has been reached, so add the current branch and stop
            edgeCollections.add(currentBranch)
            //print("${currentBranch.javaClass.simpleName} ${currentBranch.id.value} ended by reaching end. ")
            return
        }
    }

    fun update(time: Int, camera: OrthographicCamera, paused: Boolean) {
        val edgeCollections = mutableListOf<EdgeCollection>()
        for (node in animation.nodes) { // Update all nodes and edges
            node.update(time, camera)
            node.edges.forEach { it.update(time) }
        }
        for (node in animation.nodes) {
            if (node.shouldDraw(time)) {
                for (edge in node.edges) {
                    traverse(
                        node,
                        edgeCollections,
                        EdgeCollection(EdgeCollectionID(edge.collectionID.value)),
                        time)
                }
            }
        }
        // Run update to cause all node collections to initialize their non-serialized variables and clear all edges
        animation.edgeCollections.forEach { it.prepare() }

        val usedIDs = mutableListOf<Int>()
        for (i in 0..<edgeCollections.size) {
            val edgeCollection = edgeCollections[i]
            if (edgeCollection.id.value in usedIDs) {
                val existingNodeCollection = animation.getEdgeCollectionByID(edgeCollection.id)!!
                val newId = animation.getEdgeCollectionId()
                println("existing node collection edges before: ${existingNodeCollection.edges}")
                edgeCollections[i] = EdgeCollection(EdgeCollectionID(newId))
                println("existing node collection edges after: ${existingNodeCollection.edges}")
                edgeCollection.edges.forEach { it.collectionID.newSetPoint(time, newId, true); it.collectionID.update(time) }
                println("Resolved duplicate ID, New ID: $newId, new edge collection size: ${edgeCollection.edges.size}")
            }
            usedIDs.add(edgeCollection.id.value)
        }

        for (edgeCollection in edgeCollections) {
            val existingNodeCollection = animation.getEdgeCollectionByID(edgeCollection.id)
            if (existingNodeCollection == null) {
                val newId = animation.getEdgeCollectionId()
                edgeCollection.edges.forEach { // Do not reuse a previous ID, as they will point to the same Edge Collection object
                    it.collectionID.setPoints.clear()
                    it.collectionID.newSetPoint(time, newId, true)
                    it.collectionID.update(time)
                }
                println("Warning: Created edge collection $newId")
                animation.edgeCollections.add(edgeCollection)
                edgeCollection.buildInputs()
            } else {
                existingNodeCollection.edges.addAll(edgeCollection.edges)
            }
        }
        //println("Added edge collections of size: " + animation.edgeCollections.map {it.edges.size})

        animation.edgeCollections.forEach { it.update(time, animation, paused) }
    }

    fun draw(batcher: SpriteBatch, shapeRenderer: ShapeRenderer, colorLayer: FrameBuffer, animationMode: Boolean) {
        // Draw area polygons
        colorLayer.begin()
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color.CLEAR
        shapeRenderer.rect(0F, 0f, DISPLAY_WIDTH.toFloat(), DISPLAY_HEIGHT.toFloat()) // Clear the color layer

        for (edgeCollection in animation.edgeCollections) {
            if (edgeCollection.edgeCollectionStrategy.javaClass == AreaStrategy::class.java) {
                edgeCollection.draw(shapeRenderer)
            }
        }

        shapeRenderer.end()
        colorLayer.end()

        // Draw the color layer to the screen
        batcher.begin()
        val textureRegion = TextureRegion(colorLayer.colorBufferTexture)
        textureRegion.flip(false, true)
        batcher.setColor(1f, 1f, 1f, 0.2f) // Draw the color layers with transparency
        batcher.draw(textureRegion, 0f, 0f, DISPLAY_WIDTH.toFloat(), DISPLAY_HEIGHT.toFloat())
        batcher.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        for (edgeCollection in animation.edgeCollections) {
            if (edgeCollection.edgeCollectionStrategy.javaClass == LineStrategy::class.java) {
                edgeCollection.draw(shapeRenderer)
            }
        }

        shapeRenderer.end()
    }
}
