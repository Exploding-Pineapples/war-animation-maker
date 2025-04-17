package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.AbstractTypeSerializable
import com.badlogicgames.waranimationmaker.InputElement
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH
import com.badlogicgames.waranimationmaker.interpolator.InterpolatedBoolean
import com.badlogicgames.waranimationmaker.interpolator.InterpolatedFloat
import com.badlogicgames.waranimationmaker.interpolator.InterpolatedValue
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
    fun show(verticalGroup: VerticalGroup, area: Area) {
        text = "Area: "
        show(verticalGroup, area as HasInputs)
    }
    fun show(verticalGroup: VerticalGroup, line: Line) {
        text = "Line: "
        show(verticalGroup, line as HasInputs)
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
    var zoomInterpolator: InterpolatedFloat
}

interface ObjectWithScreenPosition {
    var screenPosition: Coordinate
}

interface ObjectWithDeath {
    var death: InterpolatedBoolean
}

interface ObjectClickable {
    fun clicked(x: Float, y: Float) : Boolean
}

fun projectToScreen(position: Coordinate, zoom: Float, cx: Float, cy: Float): Coordinate {
    return Coordinate(position.x * zoom - cx * (zoom - 1) + (DISPLAY_WIDTH / 2 - cx), position.y * zoom - cy * (zoom - 1) + (DISPLAY_HEIGHT / 2 - cy))
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

abstract class NodeCollectionID : ID
class LineID(override val value: Int = -1) : NodeCollectionID() {
    override fun getAbstractType() = LineID::class.java
}
class AreaID(override val value: Int = -1) : NodeCollectionID() {
    override fun getAbstractType() = AreaID::class.java
}

class NodeID(override val value: Int = -1) : ID {
    override fun getAbstractType() = NodeID::class.java
}

data class Animation @JvmOverloads constructor(
    var path: String,
    var name: String = "My Animation",
    var countries: List<String> = mutableListOf(),
    val units: MutableList<Unit> = mutableListOf(),
    private var camera: Camera? = null,
    val nodes: MutableList<Node> = mutableListOf(),
    val lines: MutableList<Line> = mutableListOf(),
    val areas: MutableList<Area> = mutableListOf(),
    var unitId: Int = 0,
    var nodeCollectionId: Int = 0,
    var nodeId: Int = 0,
    val linesPerNode: Int = 12
)
{
    private var cachedImageDimensions: Pair<Int, Int>? = null

    fun load(skin: Skin) {
        unitHandler = UnitHandler(this)
        nodeHandler = NodeHandler(this)
        buildInputs(skin)
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
            cachedImageDimensions = File(path)
                .getImageDimensions()
        }

        return cachedImageDimensions!!
    }

    fun deleteObject(obj: Object): Boolean {
        if (obj.javaClass == Node::class.java) {
            if (nodeHandler.remove(obj as Node)) {
                return true
            }
        }
        if (unitHandler.remove(obj)) {
            return true
        }
        return false
    }

    fun emptyLine(): Line {
        return addLine(Line(LineID(nodeCollectionId)))
    }

    fun addLine(line: Line): Line {
        lines.add(line)
        line.buildInputs()
        nodeCollectionId++
        return line
    }

    fun addNewLine(vararg edges: Edge): Line {
        val newLine = Line(LineID(nodeCollectionId))
        newLine.edges.addAll(edges)
        return addLine(newLine)
    }

    fun addArea(area: Area): Area {
        areas.add(area)
        area.buildInputs()
        nodeCollectionId++
        return area
    }

    fun addNewArea(nodes: List<NodeID>): Area {
        val newArea = Area(AreaID(nodeCollectionId))
        return addArea(newArea)
    }

    fun getNodeCollectionByID(id: NodeCollectionID): NodeCollection? {
        if (id.javaClass == LineID::class.java) {
            return getLineByID(id as LineID) as NodeCollection
        }
        if (id.javaClass == AreaID::class.java) {
            return getAreaByID(id as AreaID) as NodeCollection
        }
        return null
    }

    fun getAreaByID(id: AreaID): Area? = areas.firstOrNull { it.id.value == id.value }

    fun getLineByID(id: LineID): Line? = lines.firstOrNull { it.id.value == id.value }

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

    fun <T : ObjectClickable> selectObjectWithType(x: Float, y: Float, type: Class<out ObjectClickable>): ArrayList<T> {
        val screenObjects = ArrayList<T>()

        if (type.isAssignableFrom(Unit::class.java)) {
            val selectedUnit = unitHandler.clicked(x, y)
            if (selectedUnit != null) {
                screenObjects.add(selectedUnit as T)
            }
        }

        if (type.isAssignableFrom(Node::class.java)) {
            screenObjects.addAll(nodes.filter { it.clicked(x, y) }.map {it as T})
        }

        if (type.isAssignableFrom(Edge::class.java)) {
            for (node in nodes) {
                for (edge in node.edges) {
                    if (edge.clicked(x, y)) {
                        screenObjects.add(edge as T)
                    }
                }
            }
        }

        return screenObjects
    }

    fun selectDraw(x: Float, y: Float, type: Class<out ScreenObject> = ScreenObject::class.java, time: Int): List<ScreenObject> {
        val output = ArrayList<ScreenObject>()
        val lowPriority = ArrayList<ScreenObject>()

        if (type::class.java.isAssignableFrom(Unit::class.java)) {
            for (unit in units) {
                if (unit.clicked(x, y)) {
                    if (unit.shouldDraw(time)) {
                        output.add(unit)
                    } else {
                        lowPriority.add(unit)
                    }
                }
            }
        }

        if (type::class.java.isAssignableFrom(Node::class.java)) {
            for (node in nodes) {
                if (node.clicked(x, y)) {
                    if (node.shouldDraw(time)) {
                        output.add(node)
                    } else {
                        lowPriority.add(node)
                    }
                }
            }
        }

        output.addAll(lowPriority)

        return output
    }

    fun getParents(id: ID) : List<NodeCollection> {
        val output: MutableList<NodeCollection> = mutableListOf()
        if (id::class.java == NodeID::class.java) {
            for (area in areas) {
                if (area.edges.firstOrNull { it.contains(id as NodeID) } != null) {
                    output.add(area)
                }
            }
            for (line in lines) {
                if (line.edges.firstOrNull { it.contains(id as NodeID) } != null) {
                    output.add(line)
                }
            }
        }
        return output
    }

    fun <T : NodeCollection> getParentsOfType(id: ID, type: Class<out NodeCollection>) : List<T> {
        val output: MutableList<T> = mutableListOf()
        if (id::class.java == NodeID::class.java) {
            if (type.isAssignableFrom(Area::class.java)) {
                for (area in areas) {
                    for (edge in area.edges) {
                        if (id == edge.segment.first) {
                            output.add(area as T)
                            break
                        }
                    }
                    if (id == area.edges.last().segment.second) {
                        output.add(area as T)
                    }
                }
            }
            if (type.isAssignableFrom(Line::class.java)) {
                for (line in lines) {
                    for (edge in line.edges) {
                        if (id == edge.segment.first) {
                            output.add(line as T)
                            break
                        }
                    }
                    if (id == line.edges.last().segment.second) {
                        output.add(line as T)
                    }
                }
            }
        }

        println("Selected Node Collections " + output)

        return output
    }

    fun buildInputs(skin: Skin) {
        nodeHandler.buildInputs()
        unitHandler.buildInputs()

        for (line in lines) {
            line.buildInputs()
        }
        for (area in areas) {
            area.buildInputs()
        }
    }

    fun update(time: Int, camera: OrthographicCamera, verticalGroup: VerticalGroup, animationMode: Boolean) {
        nodeHandler.update(time, camera)
        unitHandler.update(time, camera.zoom, camera.position.x, camera.position.y)
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