package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH


class NodeHandler(val animation: Animation) {

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

    fun getDrawNodes(time: Int): MutableList<Node> {
        val out = mutableListOf<Node>()
        for (node in animation.nodes) {
            if (node.shouldDraw(time)) {
                out.add(node)
            }
        }
        return out
    }

    fun getNonDrawNodes(time: Int): MutableList<Node> {
        val out = mutableListOf<Node>()
        for (node in animation.nodes) {
            if (!node.shouldDraw(time)) {
                out.add(node)
            }
        }
        return out
    }

    fun traverse(node: Node, nodeCollections: MutableList<NodeCollection>, currentBranch: NodeCollection) {
        val visited = (node.visitedBy.find { (it.value == currentBranch.id.value && it.javaClass == currentBranch.id::class.java) } != null)
        //println("visited: $visited, this id: ${currentBranch.javaClass} ${currentBranch.id.value}, those ids: ${node.visitedBy.map {it.value}}")
        if (visited) {
            val nodeCollection = nodeCollections.firstOrNull {it.id.value == currentBranch.id.value}
            if (nodeCollection != null) {
                if (node.id.value == nodeCollection.edges.first().segment.first.value) { // If the current node is the first node of an existing Node Collection with the same CollectionID, this branch is part of that Node Collection, so add this branch at the beginning of it
                    //println("$currentBranch added to beginning of another node collection")
                    nodeCollection.edges.addAll(0, currentBranch.edges)
                }
            } else {
                if (node.id.value == currentBranch.edges.first().segment.first.value) { // If the current node is the first node of the current branch, it is forming a loop, so add it to the list
                    //println("$currentBranch ended by loop forming")
                    nodeCollections.add(currentBranch)
                }
            }
            return
        }
        node.visitedBy.add(currentBranch.id)

        var reachedEnd = true

        for (edge in node.edges) { // Traverses every available edge from the node
            val nextNode = animation.getNodeByID(edge.segment.second)!!
            if (edge.collectionID.value == currentBranch.id.value) { // If edge continues the Node Collection that is being constructed, then continue recursion with this branch
                reachedEnd = false
                traverse(nextNode, nodeCollections, currentBranch.apply { edges.add(edge) })
            } else { // If edge is not a member of the current branch, start constructing a new Node Collection with that node id
                if (edge.collectionID.javaClass == AreaID::class.java) {
                    traverse(
                        nextNode,
                        nodeCollections,
                        Area(edge.collectionID as AreaID).apply { edges.add(edge) })
                }
                if (edge.collectionID.javaClass == LineID::class.java) {
                    traverse(
                        nextNode,
                        nodeCollections,
                        Line(edge.collectionID as LineID).apply { edges.add(edge) })
                }
            }
        }

        if (reachedEnd) { // If no edges continue the Node Collection that is being constructed, that means the end has been reached, so add the current branch to the end and stop
            val existingNodeCollection = nodeCollections.firstOrNull { it.id.value == currentBranch.id.value}
            if (existingNodeCollection == null) {
                nodeCollections.add(currentBranch)
            } else {
                existingNodeCollection.edges.addAll(currentBranch.edges)
            }
            //println("$currentBranch ended by reaching end")
            return
        }
    }

    fun update(time: Int, camera: OrthographicCamera) {
        val nodeCollections = mutableListOf<NodeCollection>()
        for (node in animation.nodes) {
            if (node.visitedBy == null) {
                node.visitedBy = mutableListOf()
            }
            node.visitedBy.clear()
        }
        for (node in animation.nodes) {
            node.update(time, camera)
            if (node.visitedBy.isEmpty()) {
                for (edge in node.edges) {
                    if (edge.collectionID.javaClass == AreaID::class.java) {
                        traverse(
                            node,
                            nodeCollections,
                            Area(edge.collectionID as AreaID))
                    }
                    if (edge.collectionID.javaClass == LineID::class.java) {
                        traverse(
                            node,
                            nodeCollections,
                            Line(edge.collectionID as LineID))
                    }
                }
            }
        }

        for (nodeCollection in nodeCollections) {
            if (nodeCollection.javaClass == Line::class.java) {
                val existingLine = animation.lines.firstOrNull{ it.id.value == nodeCollection.id.value }
                if (existingLine != null) {
                    //println("Used existing line: " + existingLine.id.value)
                    existingLine.edges = nodeCollection.edges
                    existingLine.update(time, animation)
                } else {
                    println("Created new line: " + nodeCollection.id.value)
                    animation.lines.add(nodeCollection as Line)
                    nodeCollection.update(time, animation)
                }
            }
        }
        //println("Added lines of size: " + animation.lines.map {it.edges.size})

        for (nodeCollection in nodeCollections) {
            if (nodeCollection.javaClass == Area::class.java) {
                val existingArea = animation.areas.firstOrNull{ it.id.value == nodeCollection.id.value }
                if (existingArea != null) {
                    existingArea.edges = nodeCollection.edges
                    existingArea.update(time, animation)
                } else {
                    println("Created new line: " + nodeCollection.id.value)
                    animation.areas.add(nodeCollection as Area)
                    nodeCollection.buildInputs()
                    nodeCollection.update(time, animation)
                }
            }
        }
        //println("Added areas of size: " + animation.areas.map {it.edges.size})
    }

    fun draw(batcher: SpriteBatch, shapeRenderer: ShapeRenderer, colorLayer: FrameBuffer, animationMode: Boolean) {
        // Draw area polygons
        colorLayer.begin()
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color.CLEAR
        shapeRenderer.rect(0F, 0f, DISPLAY_WIDTH.toFloat(), DISPLAY_HEIGHT.toFloat()) // Clear the color layer

        for (area in animation.areas) {
            area.draw(shapeRenderer)
        }

        shapeRenderer.end()
        colorLayer.end()

        // Draw the color layer to the screen TODO replace color layer
        batcher.begin()
        val textureRegion = TextureRegion(colorLayer.colorBufferTexture)
        textureRegion.flip(false, true)
        batcher.setColor(1f, 1f, 1f, 0.2f) // Draw the color layers with transparency
        batcher.draw(textureRegion, 0f, 0f, DISPLAY_WIDTH.toFloat(), DISPLAY_HEIGHT.toFloat())
        batcher.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        for (line in animation.lines) {
            line.draw(shapeRenderer)
        }

        //Draw the debug circles
        if (animationMode) {
            animation.nodes.forEach { it.drawAsLineNode(shapeRenderer) }
        }
        shapeRenderer.end()
    }
}
