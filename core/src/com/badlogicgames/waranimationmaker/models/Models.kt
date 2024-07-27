package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogicgames.waranimationmaker.AbstractTypeSerializable
import com.badlogicgames.waranimationmaker.InputElement
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH
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

    fun showInputs(verticalGroup: VerticalGroup) {
        updateInputs()
        for (inputElement in inputElements) {
            inputElement.display(verticalGroup)
        }
    }

    fun hideInputs(verticalGroup: VerticalGroup) {
        updateInputs()
        for (inputElement in inputElements) {
            inputElement.hide(verticalGroup)
        }
    }
}

interface ObjectWithZoom {
    var zoom: Float //zoom for camera only
    var zoomInterpolator: InterpolatedFloat
}

interface ObjectWithScreenPosition {
    var screenPosition: Coordinate
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
class LineID(override val value: Int = -1) : ID {
    override fun getAbstractType() = LineID::class.java
}
class AreaID(override val value: Int = -1) : ID {
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
        if (nodeHandler.remove(obj)) {
            return true
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
        nodeCollectionId++
        return line
    }

    fun addNewLine(nodes: List<NodeID>): Line {
        val newLine = Line(LineID(nodeCollectionId))
        newLine.nodeIDs.addAll(nodes)
        return addLine(newLine)
    }

    fun addArea(area: Area): Area {
        areas.add(area)
        nodeCollectionId++
        return area
    }

    fun addNewArea(nodes: List<NodeID>): Area {
        val newArea = Area(AreaID(nodeCollectionId))
        newArea.nodeIDs.addAll(nodes)
        return addArea(newArea)
    }

    fun getAreaByID(id: AreaID): Area? = areas.firstOrNull { it.id.value == id.value }

    fun getLineByID(id: LineID): Line? = lines.find { it.id.value == id.value }

    fun getNodeByID(id: NodeID): Node? = nodes.firstOrNull { it.id.value == id.value }

    fun createNodeAtPosition(time: Int, x: Float, y: Float): Node {
        val node = Node(Coordinate(x, y), time, NodeID(nodeId))
        nodes.add(node)
        nodeId++
        return node
    }

    fun selectObject(x: Float, y: Float): ArrayList<ScreenObject> {
        return selectObjectWithType(x, y, ScreenObject::class.java)
    }

    fun selectObjectWithType(x: Float, y: Float, type: Class<out ScreenObject>): ArrayList<ScreenObject> {
        val screenObjects = ArrayList<ScreenObject>()


        if (type.isAssignableFrom(Unit::class.java)) {
            for (unit in units) {
                if (unit.clicked(x, y)) {
                    screenObjects.add(unit)
                }
            }
        }

        if (type.isAssignableFrom(Node::class.java)) {
            for (node in nodes) {
                if (node.clicked(x, y)) {
                    screenObjects.add(node)

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
                if (id.value in area.nodeIDs.map { it.value }) {
                    output.add(area)
                }
            }
            for (line in lines) {
                if (id.value in line.nodeIDs.map { it.value }) {
                    output.add(line)
                }
            }
        }
        return output
    }

    fun getParentsOfType(id: ID, type: Class<out NodeCollection>) : List<NodeCollection> {
        val output: MutableList<NodeCollection> = mutableListOf()
        if (id::class.java == NodeID::class.java) {
            if (type.isAssignableFrom(Area::class.java)) {
                for (area in areas) {
                    if (id.value in area.nodeIDs.map { it.value }) {
                        output.add(area)
                    }
                }
            }
            if (type.isAssignableFrom(Line::class.java)) {
                for (line in lines) {
                    if (id.value in line.nodeIDs.map { it.value }) {
                        output.add(line)
                    }
                }
            }
        }

        println("Selected Node Collections " + output)

        return output
    }

    fun buildInputs(skin: Skin) {
        nodeHandler.buildInputs(skin)
        unitHandler.buildInputs(skin)
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