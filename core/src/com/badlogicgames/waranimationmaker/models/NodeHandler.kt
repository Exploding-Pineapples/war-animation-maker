package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.AnimationScreen
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH


class NodeHandler(val animation: Animation) {
    fun newNode() {
    }

    fun buildInputs(skin: Skin) {
        for (node in animation.nodes) {
            node.buildInputs(skin)
        }
    }

    fun add(node: Node)
    {
        animation.nodes[animation.nodeId] = node
        animation.nodeId++
    }

    fun remove(removeNode: Object): Boolean
    {
        for (nodeCollection in animation.getParents(removeNode.id)) {
            nodeCollection.nodeIDs.removeIf { it.value == removeNode.id.value}
        }

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

    fun update(time: Int, camera: OrthographicCamera) {
        for (node in animation.nodes) {
            node.update(time, camera)
        }

        for (line in animation.lines) { // Interpolate every line
            line.update(animation)
        }

        var prevIndex = 0

        animation.areas.removeIf { it.nodeIDs.isEmpty() }

        for (area in animation.areas) {
            area.update()
            for (entry in area.orderOfLineSegments) {
                for (index in prevIndex..<entry.key) { // Adds the part of the border until the index
                    val node = animation.getNodeByID(area.nodeIDs[index])
                    if (node == null) {
                        println("Node not found what the fuck")
                    } else {
                        area.drawCoords.add(node.screenPosition)
                    }
                }
                prevIndex = entry.key
                for (lineSegment in entry.value) {
                    val line = animation.getLineByID(lineSegment.lineID)!!
                    val indexFirst = line.nodeIDs.map {it.value}.indexOf(lineSegment.segment.first.value)
                    val indexSecond = line.nodeIDs.map {it.value}.indexOf(lineSegment.segment.second.value)
                    if (indexFirst != -1 && indexSecond != -1) {
                        if (indexSecond > indexFirst) {
                            for (index in indexFirst * AnimationScreen.LINES_PER_NODE..indexSecond * AnimationScreen.LINES_PER_NODE) {
                                area.drawCoords.add(Coordinate(line.xInterpolator.interpolator.interpolateAt(index.toDouble()).toFloat(), line.yInterpolator.interpolator.interpolateAt(index.toDouble()).toFloat()))
                            }
                        } else {
                            for (index in indexSecond * AnimationScreen.LINES_PER_NODE..indexFirst * AnimationScreen.LINES_PER_NODE) {
                                area.drawCoords.add(Coordinate(line.xInterpolator.interpolator.interpolateAt(index.toDouble()).toFloat(), line.yInterpolator.interpolator.interpolateAt(index.toDouble()).toFloat()))
                            }
                        }
                    }
                }
            }
            for (index in prevIndex..<area.nodeIDs.size) {
                area.drawCoords.add(animation.getNodeByID(area.nodeIDs[index])!!.screenPosition)
            }
        }
    }

    fun draw(batcher: SpriteBatch, shapeRenderer: ShapeRenderer, colorLayer: FrameBuffer, animationMode: Boolean) {
        // Draw area polygons
        colorLayer.begin()
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color.CLEAR
        shapeRenderer.rect(0F, 0f, DISPLAY_WIDTH.toFloat(), DISPLAY_HEIGHT.toFloat()) // Clear the color layer

        for (area in animation.areas) { // Draw areas to the color layer
            area.draw(shapeRenderer)
        }

        shapeRenderer.end()
        colorLayer.end()

        // Draw the color layer to the screen TODO replace color layer
        batcher.begin()
        val textureRegion: TextureRegion = TextureRegion(colorLayer.getColorBufferTexture())
        textureRegion.flip(false, true)
        batcher.setColor(1f, 1f, 1f, 0.2f) // Draw the color layers with transparency
        batcher.draw(textureRegion, 0f, 0f, DISPLAY_WIDTH.toFloat(), DISPLAY_HEIGHT.toFloat())
        batcher.end()

        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for (line in animation.lines) {
            line.draw(shapeRenderer)
            if (animationMode) {
                for (id in line.nodeIDs) {
                    val node = animation.getNodeByID(id)!!
                    node.drawAsLineNode(shapeRenderer)
                }
            }
        }
        shapeRenderer.end()

        //Draw the debug circles
        if (animationMode) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            for (area in animation.areas) {
                for (id in area.nodeIDs) {
                    val node = animation.getNodeByID(id)!!
                    node.drawAsAreaNode(shapeRenderer)
                }
            }
            shapeRenderer.end()
        }
    }
}
