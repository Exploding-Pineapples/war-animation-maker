package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.*
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH
import com.badlogicgames.waranimationmaker.interpolator.InterpolatedBoolean
import com.badlogicgames.waranimationmaker.interpolator.LinearInterpolatedFloat
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolatedFloat
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.png.PngDirectory
import java.io.File

data class Coordinate(
    var x: Float,
    var y: Float
)

interface HasInputs {
    var inputElements: MutableList<InputElement<*>>

    fun buildInputs() {
        inputElements = mutableListOf()
    }

    fun updateInputs() {
        if (inputElements == null) {
            inputElements = mutableListOf()
        }
    }

    fun showInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor)

    fun hideInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor) {
        uiVisitor.hide(verticalGroup, this)
    }
}

class UIVisitor(val skin: Skin) {
    var text: String = ""
    val labels: MutableList<Label> = mutableListOf()

    fun show(verticalGroup: VerticalGroup, hasInputs: HasInputs) {
        hasInputs.updateInputs()
        val label = Label(text, skin)
        labels.add(label)
        verticalGroup.addActor(label)
        for (inputElement in hasInputs.inputElements) {
            inputElement.show(verticalGroup, skin)
        }
    }
    fun show(verticalGroup: VerticalGroup, camera: Camera) {
        text = "Camera: "
        show(verticalGroup, camera as HasInputs)
    }
    fun show(verticalGroup: VerticalGroup, unit: Unit) {
        text = "Unit: "
        show(verticalGroup, unit as HasInputs)
    }
    fun show(verticalGroup: VerticalGroup, node: Node) {
        text = "Node: "
        show(verticalGroup, node as HasInputs)
    }
    fun show(verticalGroup: VerticalGroup, edgeCollection: EdgeCollection) {
        text = "Edge Collection ${edgeCollection.id.value}: "
        show(verticalGroup, edgeCollection as HasInputs)
    }
    fun show(verticalGroup: VerticalGroup, arrow: Arrow) {
        text = "Arrow: "
        show(verticalGroup, arrow as HasInputs)
    }

    fun hide(verticalGroup: VerticalGroup, hasInputs: HasInputs) {
        hasInputs.updateInputs()
        for (inputElement in hasInputs.inputElements) {
            inputElement.hide(verticalGroup)
        }
        for (label in labels) {
            verticalGroup.removeActor(label)
        }
        labels.clear()
    }
}

interface ObjectWithZoom {
    var zoom: Float //zoom for camera only
    var zoomInterpolator: PCHIPInterpolatedFloat
}

interface ObjectWithScreenPosition {
    var screenPosition: Coordinate
}

interface ObjectWithDeath {
    var death: InterpolatedBoolean
}

interface ObjectWithAlpha {
    val alpha: LinearInterpolatedFloat
}

abstract class ScreenObjectWithAlpha: ScreenObject(), ObjectWithAlpha {
    override var alpha: LinearInterpolatedFloat = LinearInterpolatedFloat(1f, 0)

    override fun goToTime(time: Int, zoom: Float, cx: Float, cy: Float): Boolean {
        alpha.update(time)
        return super.goToTime(time, zoom, cx, cy)
    }

    override fun buildInputs() {
        super.buildInputs()

        inputElements.add(TextInput(null, { input ->
            if (input != null) {
                alpha.value = input
            }
        }, label@{
            return@label alpha.value.toString()
        }, Float::class.java, "Set alpha set point"))
    }
}

interface ObjectClickable {
    fun clicked(x: Float, y: Float) : Boolean
}

fun projectToScreen(position: Coordinate, zoom: Float, cx: Float, cy: Float): Coordinate {
    return Coordinate(
        position.x * zoom - cx * (zoom - 1) + (DISPLAY_WIDTH / 2 - cx),
        position.y * zoom - cy * (zoom - 1) + (DISPLAY_HEIGHT / 2 - cy)
    )
}

interface ID : Comparable<ID>, AbstractTypeSerializable {
    val value: Int

    override fun compareTo(other: ID): Int {
        return value - other.value
    }
}

class UnitID(override val value: Int = -1) : ID {
    override fun getAbstractType() = UnitID::class.java
}

class EdgeCollectionID(override val value: Int = -1) : ID {
    override fun getAbstractType() = EdgeCollectionID::class.java
}

class NodeID(override val value: Int = -1) : ID {
    override fun getAbstractType() = NodeID::class.java
}

data class Animation @JvmOverloads constructor(
    var mapPath: String,
    var name: String = "My Animation",
    var countries: List<String> = mutableListOf(),
    val units: MutableList<Unit> = mutableListOf(),
    private var camera: Camera? = null,
    val nodes: MutableList<Node> = mutableListOf(),
    val edgeCollections: MutableList<EdgeCollection> = mutableListOf(),
    val arrows: MutableList<Arrow> = mutableListOf(),
    var unitId: Int = 0,
    private var edgeCollectionId: Int = 0,
    var nodeId: Int = 0,
    val linesPerNode: Int = 12
)
{
    private var cachedImageDimensions: Pair<Int, Int>? = null

    fun getEdgeCollectionId(): Int {
        edgeCollectionId++
        return edgeCollectionId
    }

    fun getEdgeCollectionId(noIncrement: Boolean): Int {
        if (!noIncrement) {
            edgeCollectionId++
        }
        return edgeCollectionId
    }

    fun load() {
        unitHandler = UnitHandler(this)
        nodeHandler = NodeHandler(this)
        buildInputs()
    }

    fun camera(): Camera
    {
        if (camera == null)
        {
            camera = Camera(Coordinate(960f, 540f),1.0f, 0)
        }

        return camera!!
    }

    fun getImageDimensions(): Pair<Int, Int> {
        if (cachedImageDimensions == null) {
            cachedImageDimensions = File(mapPath)
                .getImageDimensions()
        }

        return cachedImageDimensions!!
    }

    fun deleteObject(obj: Object): Boolean {
        if (obj.javaClass == Node::class.java) {
            return nodeHandler.remove(obj as Node)
        }
        if (unitHandler.remove(obj)) {
            return true
        }
        return false
    }

    fun getEdgeCollectionByID(id: EdgeCollectionID): EdgeCollection? {
        return edgeCollections.find { it.id.value == id.value }
    }

    fun getNodeByID(id: NodeID): Node? = nodes.firstOrNull { it.id.value == id.value }

    fun createNodeAtPosition(time: Int, x: Float, y: Float): Node {
        val node = Node(Coordinate(x, y), time, NodeID(nodeId))
        node.buildInputs()
        nodes.add(node)
        nodeId++
        return node
    }

    fun selectObject(x: Float, y: Float): ArrayList<ScreenObject> {
        return selectObjectWithType(x, y, ScreenObject::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : ObjectClickable> selectObjectWithType(x: Float, y: Float, type: Class<out ObjectClickable>): ArrayList<T> {
        val objects = ArrayList<T>()

        if (type.isAssignableFrom(Unit::class.java)) {
            val selectedUnit = unitHandler.clicked(x, y)
            if (selectedUnit != null) {
                objects.add(selectedUnit as T)
            }
        }

        if (type.isAssignableFrom(Node::class.java)) {
            objects.addAll(nodes.filter { it.clicked(x, y) }.map {it as T})
        }

        if (type.isAssignableFrom(Edge::class.java)) {
            for (node in nodes) {
                for (edge in node.edges) {
                    if (edge.clicked(x, y)) {
                        objects.add(edge as T)
                    }
                }
            }
        }

        if (type.isAssignableFrom(Arrow::class.java)) {
            for (arrow in arrows) {
                if (arrow.clicked(x, y)) {
                    objects.add(arrow as T);
                }
            }
        }

        return objects
    }

    fun getParents(id: ID) : List<EdgeCollection> {
        return if (id::class.java == NodeID::class.java) {
            edgeCollections.filter {
                edgeCollection -> (edgeCollection.edges.find { it.contains(id as NodeID) } != null)
            }
        } else {
            mutableListOf()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : EdgeCollectionStrategy<EdgeCollectionContext>> getParentsOfType(id: ID, type: Class<out EdgeCollectionContext>) : List<T> {
        val output: MutableList<T> = mutableListOf()
        if (id::class.java == NodeID::class.java) {
            for (edgeCollection in edgeCollections) {
                if (edgeCollection.edgeCollectionStrategy.javaClass == type) {
                    for (edge in edgeCollection.edges) {
                        if (edge.contains(id as NodeID)) {
                            output.add(edgeCollection as T)
                        }
                    }
                }
            }
        }

        println("Selected Node Collections $output")

        return output
    }

    fun buildInputs() {
        nodeHandler.buildInputs()
        unitHandler.buildInputs()
        arrows.forEach { it.buildInputs() }
        edgeCollections.forEach { it.buildInputs() }
    }

    fun update(time: Int, orthographicCamera: OrthographicCamera) {
        nodeHandler.update(time, orthographicCamera)
        unitHandler.update(time, orthographicCamera)
        arrows.forEach { it.goToTime(time, orthographicCamera.zoom, orthographicCamera.position.x, orthographicCamera.position.y) }
    }

    fun draw(game: WarAnimationMaker, colorLayer: FrameBuffer, animationMode: Boolean, zoomFactor: Float, time: Int, camera: OrthographicCamera) {
        nodeHandler.draw(game.batcher, game.shapeRenderer, colorLayer, animationMode)
        unitHandler.draw(game, game.shapeRenderer, zoomFactor)
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        arrows.forEach { it.draw(game.shapeRenderer, camera, time) }
        game.shapeRenderer.end()
    }

    fun newArrow(x: Float, y: Float, time: Int): Arrow {
        val new = Arrow(x, y, time)
        new.buildInputs()
        arrows.add(new)
        return new
    }

    @Transient var unitHandler = UnitHandler(this)
    @Transient var nodeHandler = NodeHandler(this)
}

fun File.getImageDimensions(): Pair<Int, Int> {
    val metadata = ImageMetadataReader.readMetadata(this)

    val pngDirectory = metadata
        .getDirectoriesOfType(PngDirectory::class.java)
        .firstOrNull { it.name == "PNG-IHDR" }
        ?: throw IllegalStateException(
            "Image read is not of PNG format"
        )

    return Pair(
        pngDirectory.getInt(1), // image width
        pngDirectory.getInt(2) // image height
    )
}