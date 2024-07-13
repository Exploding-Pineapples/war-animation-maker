package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogicgames.waranimationmaker.AnimationScreen
import com.badlogicgames.waranimationmaker.AreaColor
import com.badlogicgames.waranimationmaker.Assets
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_HEIGHT
import com.badlogicgames.waranimationmaker.WarAnimationMaker.DISPLAY_WIDTH
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolator
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolator.IntToFloatInterpolator
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolator.FloatToFloatInterpolator
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.png.PngDirectory
import earcut4j.Earcut
import java.io.File
import java.util.*
import kotlin.math.absoluteValue

// Any values which needed to be interpolated over time are of this type
class InterpolatedFloat(initValue: Float, initTime: Int) {
    var interpolator = IntToFloatInterpolator(listOf(initTime), listOf(initValue))
    val setPoints: SortedMap<Int, Float> = TreeMap()
    var value: Float = initValue

    init {
        println("Created new Interpolated Value")
        setPoints[initTime] = initValue
    }

    fun updateInterpolator() {
        print("updating interpolator")
        println(setPoints)
        interpolator.update(setPoints.keys.toList(), setPoints.values.toList())
    }

    fun update(time: Int): Float { // Updates value based on time and returns it
        if (setPoints.isEmpty()) {
            throw IllegalArgumentException("Movement frames can not be empty when goToTime is called")
        }
        value = interpolator.interpolateAt(time)

        println(value)

        return value
    }

    fun removeFrame(time: Int): Boolean {
        var definedTime = 0

        val iterator = setPoints.keys.iterator()
        val removeKeys: MutableList<Int> = mutableListOf() // avoid concurrent modification of keys

        while ((definedTime <= time)) {
            if (!iterator.hasNext()) { //the time is not defined further, so stop
                break
            }

            definedTime = iterator.next()
            if (definedTime == time) {
                if (setPoints.size > 1) {
                    removeKeys.add(definedTime)
                    println("removed single coordinate")
                } else {
                    return false
                }
            }
        }

        for (key in removeKeys) {
            if (setPoints.keys.size > 1) { // if somehow all the times in movement frames were the same, this would prevent them all being removed
                setPoints.keys.remove(key)
            }
        }
        updateInterpolator()

        return true
    }

    fun newSetPoint(time: Int, value: Float) {
        if (time > (setPoints.keys.last())) { // Adds time and value to the end
            setPoints[time] = value
            updateInterpolator()
            println("Appended, new motions: $setPoints")
            return
        }

        for (definedTime in setPoints.keys) {
            if (time == definedTime) {
                setPoints[definedTime] = value
                updateInterpolator()
                println("Overwrote, new motions: $setPoints")
                return
            }
            if (definedTime > time) {
                setPoints[time] = value
                updateInterpolator()

                println("Inserted, new motions: $setPoints")
                return
            }
        }
    }

    // When you add a time coordinate pair to an object which hasn't had a defined movement for a long time, it will interpolate a motion the whole way, which can be undesirable
    // Ex. last defined position was at time 0, you want it to move to another position at 800
    // But you only want it to move starting from time 600
    // The below function is used hold the object at the last position until the desired time
    fun holdValueUntil(time: Int) {
        var prevTime: Int? = null
        var prevValue: Float? = null

        val frameTimes = setPoints.keys.toList()

        for (i in frameTimes.indices) {
            val definedTime = frameTimes[i]

            if (definedTime?.toInt() == time) { // If the time is already defined, don't do anything
                return
            }

            if ((definedTime > time) && (prevTime != null)) { // If the input time is not defined but is in the defined period, modify the movement to stay at the position just before the input time until the input time
                setPoints[time] = prevValue!!
                updateInterpolator()

                println("Added hold frame: $setPoints")
                return
            }

            prevTime = definedTime
            prevValue = setPoints[prevTime]
        }
        // If the input time was not in the defined period, add a movement to the end
        setPoints[time] = setPoints.entries.last().value
        updateInterpolator()
    }
}
// Needed for StaticObject to have a plain Coordinate as its position while other Objects have a InterpolatedCoordinate

data class Coordinate(var x: Float, var y: Float) {
    var value = Pair(x, y)
}
// Contains 2 InterpolatedValues which represent an interpolated Coordinate
class InterpolatedCoordinate(
    firstCoordinate: Coordinate,
    firstTime: Int
) {
    var x: InterpolatedFloat = InterpolatedFloat(firstCoordinate.x, firstTime)
    var y: InterpolatedFloat = InterpolatedFloat(firstCoordinate.y, firstTime)

    var value = Pair(x.value, y.value)
    fun update(time: Int): Coordinate {
        val x = x.update(time).toFloat()
        val y = y.update(time).toFloat()
        return Coordinate(x, y)
    }
    fun removeFrame(time: Int): Boolean {
        return x.removeFrame(time) && y.removeFrame(time)
    }
    fun newSetPoint(time: Int, coordinate: Coordinate) {
        x.newSetPoint(time, coordinate.x)
        y.newSetPoint(time, coordinate.y)
    }
    fun holdPositionUntil(time: Int) {
        x.holdValueUntil(time)
        y.holdValueUntil(time)
    }
}
// The base Object, is extended by the camera, all nodes, all units, and all other features with a specific position. Might not have a screen position. Requires a position to instantiate
open class Object (@Transient open val position: InterpolatedCoordinate) {
}

interface ObjectWithDeath {
    var death: Int?
}

interface ObjectWithAlpha {
    var alpha: Float
}

interface ObjectWithScreenPosition {
    val screenPosition: Coordinate

    fun clicked(x: Float, y: Float): Boolean {
        return (x - screenPosition.x).absoluteValue <= 10 && (y - screenPosition.y).absoluteValue <= 10
    }
}

// Object which moves, includes all nodes, all units, and the camera.
abstract class InterpolatedObject(@Transient override val position: InterpolatedCoordinate) : Object(position) {
    open fun update(time: Int) {
        position.update(time)
    }
}

interface ObjectWithZoom {
    val zoom: InterpolatedFloat
}

data class Camera(
    override var position: InterpolatedCoordinate = InterpolatedCoordinate(Coordinate(960f, 540f), 0),
    override val zoom: InterpolatedFloat = InterpolatedFloat(1.0f, 0)
) : InterpolatedObject(position), ObjectWithZoom {
    override fun update(time: Int) {
        super.update(time)
        zoom.update(time)
    }
}

interface InterpolatedScreenObject : ObjectWithScreenPosition {
    val position: InterpolatedCoordinate

    fun update(time: Int, camera: OrthographicCamera) {
        position.update(time)
        updateScreenPosition(camera)
    }

    fun updateScreenPosition(camera: OrthographicCamera) {
        val screenX = position.x.value * camera.zoom - camera.position.x * (camera.zoom - 1) + (DISPLAY_WIDTH / 2 - camera.position.x)
        val screenY = position.y.value * camera.zoom - camera.position.y * (camera.zoom - 1) + (DISPLAY_HEIGHT / 2 - camera.position.y)

        screenPosition.x = screenX
        screenPosition.y = screenY
    }
}

data class Unit(
    override val position: InterpolatedCoordinate,
    val image: String,
    var name: String? = null,
    var type: String = "infantry",
    var size: String = "XX",
    override var alpha: Float = 1.0f,
    override var death: Int? = null
) : InterpolatedObject(position), InterpolatedScreenObject, ObjectWithAlpha, ObjectWithDeath
{
    override var screenPosition: Coordinate = Coordinate(Float.NaN, Float.NaN)
    companion object {
        val sizePresets = mapOf(
            "XX" to 1.0f,
            "X" to 0.75f,
            "III" to 0.5f,
        )
    }
    @Transient
    private var texture: Texture? = null
    private var width: Float = AnimationScreen.DEFAULT_UNIT_WIDTH.toFloat()
    private var height: Float = AnimationScreen.DEFAULT_UNIT_HEIGHT.toFloat()

    fun texture(): Texture
    {
        if (texture == null)
        {
            texture = Assets.loadTexture(image)
        }

        return texture!!
    }
    override fun update(time: Int) {
        super<InterpolatedObject>.update(time)
    }

    fun draw(batcher: SpriteBatch, sizefactor: Float, font: BitmapFont) {
        var sizePresetFactor = 1.0f
        if (size in sizePresets) {
            sizePresetFactor = sizePresets[size]!!
        }
        width = AnimationScreen.DEFAULT_UNIT_WIDTH * sizefactor * sizePresetFactor
        height = AnimationScreen.DEFAULT_UNIT_HEIGHT * sizefactor * sizePresetFactor

        batcher.setColor(1f, 1f, 1f, alpha)
        batcher.draw(
            texture(),
            screenPosition.x - width / 2,
            screenPosition.y - height / 2,
            width,
            height
        )
        font.data.setScale((0.3 * sizefactor * sizePresetFactor).toFloat())
        font.color = Color(255.0f, 63.75f, 0.0f, alpha)

        font.draw(batcher, size, screenPosition.x - width / 2, screenPosition.y + height / 2)
        if (name != null) { font.draw(batcher, name, screenPosition.x - width / 2, screenPosition.y) }
        font.draw(batcher, type, screenPosition.x - width / 2, screenPosition.y - height / 2)
    }
}

data class Node
@JvmOverloads
constructor(
    override val position: InterpolatedCoordinate,
    var color: Color = Color.GREEN,
    override var death: Int? = null,
) : InterpolatedObject(position), InterpolatedScreenObject, ObjectWithDeath {
    override var screenPosition: Coordinate = Coordinate(Float.NaN, Float.NaN)

    fun update(shapeRenderer: ShapeRenderer, time: Int, camera: OrthographicCamera, animationMode: Boolean) { // Goes to time, and if animation mode is active, draws colored circle
        position.update(time)
        // Add back the color update code

        if (death != null) {
            if (time > death!!) {
                color = Color.RED
            }
        }
        
        updateScreenPosition(camera)
        
        if (animationMode) {
            shapeRenderer.color = color
            shapeRenderer.circle(screenPosition.x, screenPosition.y, 7.0f)
        }
    }
}

interface NodeCollection {
    val nodes: MutableList<Node>
    var alpha: Float

    fun getDrawNodes(time: Int): List<Node> {
        val out = mutableListOf<Node>()
        for (node in nodes) {
            if (time >= node.position.x.setPoints.keys.first()) {
                if (node.death != null) {
                    if (time <= node.death!!) {
                        out.add(node)
                    }
                } else {
                    out.add(node)
                }
            }
        }
        return out
    }

    fun getNonDrawNodes(time: Int): List<Node> {
        val out = mutableListOf<Node>()
        for (node in nodes) {
            if (time <= node.position.x.setPoints.keys.first()) {
                out.add(node)
            } else {
                if (node.death != null) {
                    if (time >= node.death!!) {
                        out.add(node)
                    }
                }
            }
        }
        return out
    }
}

data class Area (
    override val nodes: MutableList<Node> = mutableListOf(),
    var color: AreaColor = AreaColor.RED,
    var lineIDs: List<Pair<Int, Int>> = mutableListOf(),
    @Transient var drawPoly: MutableList<FloatArray> = mutableListOf(),
    override var alpha: Float
) : NodeCollection {
    fun calculatePolygon(lines: List<Pair<Line, Int>>, time: Int) {
        val border1D = DoubleArray(nodes.size * 2)
        var poly = DoubleArray(0)

        var n = 0
        val nodes = getDrawNodes(time)
        while (n < nodes.size) {
            val node = nodes[n]
            border1D[2 * n] = node.screenPosition.x.toDouble()
            border1D[2 * n + 1] = node.screenPosition.y.toDouble()
            n++
        }

        var lastBorderIndex = 0
        for (l in lines) {
            //flattens interpolatedX and interpolatedY points into 1D array
            val line: Line = l.first
            val linePoly = DoubleArray(line.interpolatedX.size * 2)
            for (i in line.interpolatedX.indices) {
                linePoly[i * 2] = line.interpolatedX[i].toDouble()
                linePoly[i * 2 + 1] = line.interpolatedY[i].toDouble()
            }

            poly += border1D.slice(lastBorderIndex until l.second * 2)
            lastBorderIndex = l.second * 2
            poly += linePoly
        }
        poly += border1D.slice(lastBorderIndex until border1D.size)

        val earcut = Earcut.earcut(poly) //turns polygon into series of triangles represented by polygon vertex indexes

        drawPoly = mutableListOf()

        var j = 0
        while (j < earcut.size) {
            drawPoly.add(
                floatArrayOf(
                    poly[earcut[j] * 2].toFloat(),
                    poly[earcut[j] * 2 + 1].toFloat(),
                    poly[earcut[j + 1] * 2].toFloat(),
                    poly[earcut[j + 1] * 2 + 1].toFloat(),
                    poly[earcut[j + 2] * 2].toFloat(),
                    poly[earcut[j + 2] * 2 + 1].toFloat()
                )
            ) //3 pairs of floats represent a triangle
            j += 3
        }
    }
    fun draw(shapeRenderer: ShapeRenderer) {
        shapeRenderer.color = color.color
        for (triangle in drawPoly) {
            shapeRenderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5])
        }
    }
}

data class Line(
    val id: Int,
    override val nodes: MutableList<Node> = mutableListOf(),
    @Transient var interpolatedX: Array<Float> = arrayOf(),
    @Transient var interpolatedY: Array<Float> = arrayOf(),
    var lineThickness: Float = 5.0f,
    var color: AreaColor = AreaColor.RED,
    override var alpha: Float
) : NodeCollection {

    private fun interpolate(num: Int, time: Int) : Boolean {
        //reset values to nothing by default
        interpolatedX = arrayOf()
        interpolatedY = arrayOf()

        val drawNodes = getDrawNodes(time)
        val xValues = FloatArray(drawNodes.size)
        val yValues = FloatArray(drawNodes.size)
        val evalAt = FloatArray(drawNodes.size)

        var i = 0
        while (i < drawNodes.size) {
            evalAt[i] = i.toFloat() //numbers from 0 - drawNodes.size() are used as interpolation points
            i += 1
        }

        var node: Node
        for (nodeIndex in drawNodes.indices) {
            node = drawNodes[nodeIndex]
            xValues[nodeIndex] = node.screenPosition.x
            yValues[nodeIndex] = node.screenPosition.y
        }

        if (drawNodes.size >= AnimationScreen.MIN_LINE_SIZE) {
            interpolatedX = Array(num + 1) { 0.0f }
            interpolatedY = Array(num + 1) { 0.0f }

            val xInterpolator = FloatToFloatInterpolator(evalAt.toList(), xValues.toList())
            val yInterpolator = FloatToFloatInterpolator(evalAt.toList(), yValues.toList())

            i = 0
            var eval: Double
            while (i < num) {
                eval = (drawNodes.size.toFloat() - 1.00) * i / num
                interpolatedX[i] = xInterpolator.interpolateAt(eval.toFloat()).toFloat()
                interpolatedY[i] = yInterpolator.interpolateAt(eval.toFloat()).toFloat()
                i++
            }

            interpolatedX[num] = xInterpolator.interpolateAt((drawNodes.size.toFloat() - 1.00f)).toFloat()
            interpolatedY[num] = yInterpolator.interpolateAt((drawNodes.size.toFloat() - 1.00f)).toFloat()
            return true
        }
        return false
    }

    fun update(shapeRenderer: ShapeRenderer, linesPerNode: Int, time: Int) {
        shapeRenderer.color = color.color
        if (interpolate(linesPerNode * nodes.size, time)) {
            for (i in 0 until AnimationScreen.LINES_PER_NODE * getDrawNodes(time).size) {
                shapeRenderer.rectLine(
                    interpolatedX[i],
                    interpolatedY[i],
                    interpolatedX[i + 1],
                    interpolatedY[i + 1],
                    lineThickness
                )
            }
        }
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
        unit.texture()
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
            camera = Camera()
        }

        return camera!!
    }

    fun getDrawUnits(time: Int): List<Unit> {
        val out = mutableListOf<Unit>()
        for (unit in units) {
            if (time >= unit.position.x.setPoints.keys.first()) {
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
            if (time <= unit.position.x.setPoints.keys.first()) {
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
        return units.remove(obj)
    }

    fun getLineByID(id: Int): Line? {
        for (l in lines) {
            if (l.id == id) {
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