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
import com.badlogicgames.waranimationmaker.interpolator.PCHIPInterpolator.Interpolator
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.png.PngDirectory
import earcut4j.Earcut
import java.io.File
import java.util.*
import kotlin.math.absoluteValue

class InterpolatedFloat(initValue: Float, initTime: Int) {
    @Transient var interpolator = Interpolator(doubleArrayOf(initTime.toDouble()), doubleArrayOf(initValue.toDouble()))
    val setPoints: SortedMap<Double, Double> = TreeMap()
    @Transient var value: Float = initValue

    init {
        println("Created new Interpolated Value")
        setPoints[initTime.toDouble()] = initValue.toDouble()
        updateInterpolator()
    }

    fun updateInterpolator() {
        print("updating interpolator")
        println(setPoints)
        interpolator = Interpolator(doubleArrayOf(setPoints.keys.first() - 200) + setPoints.keys.toDoubleArray() + doubleArrayOf(setPoints.keys.last() + 200),
            doubleArrayOf(setPoints.values.first()) + setPoints.values.toDoubleArray() + doubleArrayOf(setPoints.values.last()))
        // This gives a flat slope at the end to try to keep the object in position
    }

    fun update(time: Int): Float { // Updates value based on time and returns it
        if (setPoints.isEmpty()) {
            throw IllegalArgumentException("Movement frames can not be empty when goToTime is called")
        }
        if (interpolator == null) { // Is null when animation is first opened because interpolator is @Transient
            updateInterpolator()
        }

        value = interpolator.interpolateAt(time.toDouble()).toFloat()

        return value
    }

    fun removeFrame(time: Int): Boolean {
        if (setPoints.size > 1) {
            setPoints.remove(time.toDouble())
            updateInterpolator()
        } else {
            println("Cannot delete frame from object with less than 1 keyframe")
            return false
        }

        return true
    }

    fun newSetPoint(time: Int, value: Float) {
        val time = time.toDouble()
        val value = value.toDouble()
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
        val time = time.toDouble()
        var prevTime: Double? = null
        var prevValue: Double? = null

        val frameTimes = setPoints.keys.toList()

        for (i in frameTimes.indices) {
            val definedTime = frameTimes[i]

            if (definedTime == time) { // If the time is already defined, don't do anything
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

data class Coordinate(
    var x: Float,
    var y: Float
)

interface Object {
    var position: Coordinate
    var xPosition: InterpolatedFloat
    var yPosition: InterpolatedFloat
    val initTime: Int

    fun goToTime(time: Int): Boolean { // Can only be called after at least one key frame has been added
        position.x = xPosition.update(time)
        position.y = yPosition.update(time)

        return shouldDraw(time)
    }

    fun shouldDraw(time: Int): Boolean {
        return true
    }

    fun removeFrame(time: Int): Boolean {
        return xPosition.removeFrame(time) && yPosition.removeFrame(time)

    }

    fun newSetPoint(time: Int, x: Float, y: Float) {
        xPosition.newSetPoint(time, x)
        yPosition.newSetPoint(time, y)
    }

    // When you add a time coordinate pair to an object which hasn't had a defined movement for a long time, it will interpolate a motion the whole way, which can be undesirable
    // Ex. last defined position was at time 0, you want it to move to another position at 800
    // But you only want it to move starting from time 600
    // The below function is used hold the object at the last position until the desired time
    fun holdPositionUntil(time: Int) {  // Create a new movement that keeps the object at its last defined position until the current time
        xPosition.holdValueUntil(time)
        yPosition.holdValueUntil(time)
    }
}

interface ObjectWithZoom : Object {
    var zoom: Float //zoom for camera only
    var zoomInterpolator: InterpolatedFloat

    override fun goToTime(time: Int): Boolean { //can only be called after at least one key frame has been added
        if (xPosition == null) {
            xPosition = InterpolatedFloat(position.x, initTime)
            yPosition = InterpolatedFloat(position.y, initTime)
            zoomInterpolator = InterpolatedFloat(zoom, initTime)
        }
        super.goToTime(time)
        zoom = zoomInterpolator.update(time)
        return true
    }

    // When you add a time coordinate pair to an object which hasn't had a defined movement for a long time, it will interpolate a motion the whole way, which can be undesirable
    // Ex. last defined position was at time 0, you want it to move to another position at 800
    // But you only want it to move starting from time 600
    // The below function is used hold the object at the last position until the desired time
    override fun holdPositionUntil(time: Int) {  // Create a new movement that keeps the object at its last defined position until the current time
        xPosition.holdValueUntil(time)
        yPosition.holdValueUntil(time)
        zoomInterpolator.holdValueUntil(time)
    }

    fun newSetPoint(time: Int, x: Float, y: Float, zoom: Float) {
        xPosition.newSetPoint(time, x)
        yPosition.newSetPoint(time, y)
        zoomInterpolator.newSetPoint(time, zoom)
    }
}

data class Camera(
    override var position: Coordinate = Coordinate(x = 960.0f, y = 540.0f),
    override var zoom: Float = 1.0f,
    override val initTime: Int
) : ObjectWithZoom {
    override var xPosition: InterpolatedFloat = InterpolatedFloat(position.x, initTime)
    override var yPosition: InterpolatedFloat = InterpolatedFloat(position.y, initTime)
    override var zoomInterpolator: InterpolatedFloat = InterpolatedFloat(zoom, initTime)
}

interface ObjectWithScreenPosition {
    var screenPosition: Coordinate
}

abstract class ScreenObject : Object {
    var death: Int? = null
    var alpha: Float = 1.0f
    var screenPosition: Coordinate = Coordinate(0f, 0f)

    fun clicked(x: Float, y: Float): Boolean
    {
        return (x - screenPosition.x).absoluteValue <= 10 && (y - screenPosition.y).absoluteValue <= 10
    }

    fun goToTime(time: Int, zoom: Float, cx: Float, cy: Float): Boolean {
        super.goToTime(time)
        updateScreenPosition(zoom, cx, cy)

        return true
    }

    fun updateScreenPosition(zoom: Float, cx: Float, cy: Float) {
        if (screenPosition == null) { // Is null when animation is first opened because screenPosition is @Transient
            screenPosition = Coordinate(0f, 0f)
        }

        screenPosition.x = position.x * zoom - cx * (zoom - 1) + (DISPLAY_WIDTH / 2 - cx)
        screenPosition.y = position.y * zoom - cy * (zoom - 1) + (DISPLAY_HEIGHT / 2 - cy)
    }

    override fun shouldDraw(time: Int): Boolean {
        if (time < xPosition.setPoints.keys.first()) {
            return false
        }
        if (death != null) {
            if (time > death!! - 100) { //TODO Make it so that the number subtracted matches how long the death fade out is
                return false
            }
        }
        return true
    }
}

data class Unit(
    override var position: Coordinate,
    override val initTime: Int,
    val image: String,
) : ScreenObject(), ObjectWithScreenPosition {
    override var xPosition: InterpolatedFloat = InterpolatedFloat(position.x, initTime)
    override var yPosition: InterpolatedFloat = InterpolatedFloat(position.y, initTime)
    var name: String? = null
    var type: String = "infantry"
    var size: String = "XX"

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

    fun draw(batcher: SpriteBatch, sizefactor: Float, font: BitmapFont) {
        //draw only for the correct country
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

data class Node(
    override var position: Coordinate,
    override val initTime: Int,
) : Object, ScreenObject() {
    var color: Color = Color.GREEN
    override var xPosition = InterpolatedFloat(position.x, initTime)
    override var yPosition = InterpolatedFloat(position.y, initTime)

    init {
        screenPosition = Coordinate(0f, 0f)
    }

    fun update(shapeRenderer: ShapeRenderer, time: Int, camera: OrthographicCamera, animationMode: Boolean) { // Goes to time, and if animation mode is active, draws colored circle
        color = if (goToTime(time, camera.zoom, camera.position.x, camera.position.y)) {
            Color.GREEN
        } else {
            Color.YELLOW
        }
        if (death != null) {
            if (time > death!!) {
                color = Color.RED
            }
        }
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
            if (time >= node.xPosition.setPoints.keys.first()) {
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
            if (time <= node.xPosition.setPoints.keys.first()) {
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

    fun interpolate(num: Int, time: Int) : Boolean {
        //reset values to nothing by default
        interpolatedX = arrayOf()
        interpolatedY = arrayOf()

        val drawNodes = getDrawNodes(time)
        val xValues = DoubleArray(drawNodes.size)
        val yValues = DoubleArray(drawNodes.size)
        val evalAt = DoubleArray(drawNodes.size)

        var i = 0
        while (i < drawNodes.size) {
            evalAt[i] = i.toDouble() //numbers from 0 - drawNodes.size() are used as interpolation points
            i += 1
        }

        var node: Node
        for (nodeIndex in drawNodes.indices) {
            node = drawNodes[nodeIndex]
            xValues[nodeIndex] = node.screenPosition.x.toDouble()
            yValues[nodeIndex] = node.screenPosition.y.toDouble()
        }

        if (drawNodes.size >= AnimationScreen.MIN_LINE_SIZE) {
            interpolatedX = Array(num + 1) { 0.0f }
            interpolatedY = Array(num + 1) { 0.0f }

            val xInterpolator = Interpolator(evalAt, xValues)
            val yInterpolator = Interpolator(evalAt, yValues)

            i = 0
            var eval: Double
            while (i < num) {
                eval = (drawNodes.size.toFloat() - 1.00) * i / num
                interpolatedX[i] = xInterpolator.interpolateAt(eval).toFloat()
                interpolatedY[i] = yInterpolator.interpolateAt(eval).toFloat()
                i++
            }

            interpolatedX[num] = xInterpolator.interpolateAt((drawNodes.size.toFloat() - 1.00)).toFloat()
            interpolatedY[num] = yInterpolator.interpolateAt((drawNodes.size.toFloat() - 1.00)).toFloat()
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
            camera = Camera(Coordinate(960f, 540f),1.0f, 0)
        }

        return camera!!
    }

    fun getDrawUnits(time: Int): List<Unit> {
        val out = mutableListOf<Unit>()
        for (unit in units) {
            if (time >= unit.xPosition.setPoints.keys.first()) {
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
            if (time <= unit.xPosition.setPoints.keys.first()) {
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

    companion object {
//        fun getInterpolator(evalAt: DoubleArray, values: DoubleArray): PolynomialSplineFunction {
//            return interpolator.interpolate(evalAt, values)
//        }
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