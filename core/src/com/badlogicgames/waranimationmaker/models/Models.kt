package com.badlogicgames.waranimationmaker.models

import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.png.PngDirectory
import java.io.File

data class Coordinate(
    var x: Float,
    var y: Float
)

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

abstract class NodeCollection {
    abstract val nodes: MutableList<Node>
    @Transient var drawNodes: MutableList<Node> = mutableListOf()
    @Transient var nonDrawNodes: MutableList<Node> = mutableListOf()
    abstract var alpha: Float

    fun updateDrawNodes(time: Int){
        val out = mutableListOf<Node>()
        for (node in nodes) {
            if (node.shouldDraw(time)) {
                out.add(node)
            }
        }
        drawNodes = out
    }

    fun updateNonDrawNodes(time: Int) {
        val out = mutableListOf<Node>()
        for (node in nodes) {
            if (!node.shouldDraw(time)) {
                out.add(node)
            }
        }
        nonDrawNodes = out
    }
}

class UnitHandler(
    private val animation: Animation
)
{
    fun clicked(x: Float, y: Float) = animation.units
        .firstOrNull {
            it.clicked(x, y)
        }

    fun add(unit: Unit)
    {
        unit.typeTexture()
        animation.units += unit
    }

    fun loop()
    {}

    fun delete(unit: Unit)
    {
        animation.units -= unit
    }
}

data class Animation @JvmOverloads constructor(
    var path: String,
    var name: String = "My Animation",
    var countries: List<String> = mutableListOf(),
    val units: MutableList<Unit> = mutableListOf(),
    private var camera: Camera? = null,
    val lines: MutableList<Line> = mutableListOf(),
    val areas: MutableList<Area> = mutableListOf(),
    var lineID: Int = 0
)
{
    private var cachedImageDimensions: Pair<Int, Int>? = null

    fun camera(): Camera
    {
        if (camera == null)
        {
            camera = Camera(Coordinate(960f, 540f),1.0f, 0)
        }

        return camera!!
    }

    fun getDrawUnits(time: Int): List<Unit> {
        val out = mutableListOf<Unit>()
        for (unit in units) {
            if (time >= unit.xInterpolator.setPoints.keys.first()) {
                if (unit.death != null) {
                    if (time <= unit.death!!) {
                        out.add(unit)
                    }
                } else {
                    out.add(unit)
                }
            }
        }
        return out
    }

    fun getNonDrawUnits(time: Int): List<Unit> {
        val out = mutableListOf<Unit>()
        for (unit in units) {
            if (time <= unit.xInterpolator.setPoints.keys.first()) {
                out.add(unit)
            } else {
                if (unit.death != null) {
                    if (time >= unit.death!!) {
                        out.add(unit)
                    }
                }
            }
        }
        return out
    }

    fun getImageDimensions(): Pair<Int, Int> {
        if (cachedImageDimensions == null) {
            cachedImageDimensions = File(path)
                .getImageDimensions()
        }

        return cachedImageDimensions!!
    }

    fun deleteObject(obj: Object): Boolean {
        var id = 0
        for (line in lines) {
            if (line.nodes.remove(obj)) {
                return true
            }
            id++
        }
        for (area in areas) {
            if (area.nodes.remove(obj)) {
                return true
            }
            id++
        }
        if (units.remove(obj)) {
            return true
        }
        return false
    }

    fun getLineByID(ID: Int): Line? {
        for (l in lines) {
            if (l.id == ID) {
                return l
            }
        }
        return null
    }

    @Transient
    val unitHandler = UnitHandler(this)
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