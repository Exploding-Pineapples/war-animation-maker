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
            if (setPoints.remove(time.toDouble()) != null) { // Remove was successful or not
                updateInterpolator()
                return true
            } else {
                return false
            }
        } else {
            return false
        }
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
    var xInterpolator: InterpolatedFloat
    var yInterpolator: InterpolatedFloat
    val initTime: Int

    fun goToTime(time: Int): Boolean { // Can only be called after at least one key frame has been added
        if (xInterpolator == null) {
            xInterpolator = InterpolatedFloat(position.x, initTime)
            yInterpolator = InterpolatedFloat(position.y, initTime)
        }
        position.x = xInterpolator.update(time)
        position.y = yInterpolator.update(time)

        return shouldDraw(time)
    }

    fun shouldDraw(time: Int): Boolean {
        return true
    }

    fun removeFrame(time: Int): Boolean {
        return xInterpolator.removeFrame(time) && yInterpolator.removeFrame(time) // Both should be paired
    }

    fun newSetPoint(time: Int, x: Float, y: Float) {
        xInterpolator.newSetPoint(time, x)
        yInterpolator.newSetPoint(time, y)
    }

    // When you add a time coordinate pair to an object which hasn't had a defined movement for a long time, it will interpolate a motion the whole way, which can be undesirable
    // Ex. last defined position was at time 0, you want it to move to another position at 800
    // But you only want it to move starting from time 600
    // The below function is used hold the object at the last position until the desired time
    fun holdPositionUntil(time: Int) {  // Create a new movement that keeps the object at its last defined position until the current time
        xInterpolator.holdValueUntil(time)
        yInterpolator.holdValueUntil(time)
    }
}

interface ObjectWithZoom {
    var zoom: Float //zoom for camera only
    var zoomInterpolator: InterpolatedFloat
}

data class Camera(
    override var position: Coordinate = Coordinate(x = 960.0f, y = 540.0f),
    override var zoom: Float = 1.0f,
    override val initTime: Int
) : ScreenObject(), ObjectWithZoom, ObjectWithScreenPosition {
    override var xInterpolator: InterpolatedFloat = InterpolatedFloat(position.x, initTime)
    override var yInterpolator: InterpolatedFloat = InterpolatedFloat(position.y, initTime)
    override var zoomInterpolator: InterpolatedFloat = InterpolatedFloat(zoom, initTime)

    override fun goToTime(time: Int): Boolean {
        super.goToTime(time, zoom, position.x, position.y) // Call ScreenObject's goToTime to set screen position
        if (zoomInterpolator == null) {
            zoomInterpolator = InterpolatedFloat(zoom, initTime)
        }
        zoom = zoomInterpolator.update(time)
        return true
    }

    override fun holdPositionUntil(time: Int) {  // Create a new movement that keeps the object at its last defined position until the current time
        super.holdPositionUntil(time)
        zoomInterpolator.holdValueUntil(time)
    }

    override fun removeFrame(time: Int): Boolean {
        val zoomResult = zoomInterpolator.removeFrame(time)
        val positionResult = super.removeFrame(time)
        return zoomResult || positionResult // If either a zoom or position frame is removed it is a success
    }

    fun newSetPoint(time: Int, x: Float, y: Float, zoom: Float) {
        super.newSetPoint(time, x, y)
        zoomInterpolator.newSetPoint(time, zoom)
    }
}

interface ObjectWithScreenPosition {
    var screenPosition: Coordinate
}

fun projectToScreen(position: Coordinate, zoom: Float, cx: Float, cy: Float): Coordinate {
    return Coordinate(position.x * zoom - cx * (zoom - 1) + (DISPLAY_WIDTH / 2 - cx), position.y * zoom - cy * (zoom - 1) + (DISPLAY_HEIGHT / 2 - cy))
}

abstract class ScreenObject : Object, ObjectWithScreenPosition {
    var death: Int? = null
    var alpha: Float = 1f
    @Transient override var screenPosition: Coordinate = Coordinate(0f, 0f)

    open fun clicked(x: Float, y: Float): Boolean
    {
        return (x - screenPosition.x).absoluteValue <= 10 && (y - screenPosition.y).absoluteValue <= 10
    }

    fun goToTime(time: Int, zoom: Float, cx: Float, cy: Float): Boolean {
        super.goToTime(time)
        updateScreenPosition(zoom, cx, cy)
        alpha = (1f - (xInterpolator.setPoints.keys.first() - time) / 100).coerceIn(0.0, 1.0).toFloat()
        if (death != null) {
            if (time > death!! - 100) {
                alpha = ((death!! - time) / 100f).coerceIn(0f, 1f)
            }
        }

        return shouldDraw(time)
    }

    fun updateScreenPosition(zoom: Float, cx: Float, cy: Float) {
        if (screenPosition == null) { // Is null when animation is first opened because screenPosition is @Transient
            screenPosition = Coordinate(0f, 0f)
        }

        screenPosition.x = position.x * zoom - cx * (zoom - 1) + (DISPLAY_WIDTH / 2 - cx)
        screenPosition.y = position.y * zoom - cy * (zoom - 1) + (DISPLAY_HEIGHT / 2 - cy)
    }

    // Draw only if selected
    fun drawAsSelected(shapeRenderer: ShapeRenderer, animationMode: Boolean, currentZoom: Float, currentCX: Float, currentCY: Float) {
        if (animationMode) {
            shapeRenderer.color = Color.SKY
            for (time in xInterpolator.setPoints.keys.first().toInt()..xInterpolator.setPoints.keys.last().toInt() step 4) { // Draws entire path of the selected object over time
                val position = projectToScreen(Coordinate(xInterpolator.interpolator.interpolateAt(time.toDouble()).toFloat(), yInterpolator.interpolator.interpolateAt(time.toDouble()).toFloat()), currentZoom, currentCX, currentCY)
                shapeRenderer.circle(position.x, position.y, 2f)
            }
            shapeRenderer.color = Color.PURPLE
            for (time in xInterpolator.setPoints.keys) { // Draws all set points of the selected object
                val position = projectToScreen(Coordinate(xInterpolator.interpolator.interpolateAt(time.toDouble()).toFloat(), yInterpolator.interpolator.interpolateAt(time.toDouble()).toFloat()), currentZoom, currentCX, currentCY)
                shapeRenderer.circle(position.x, position.y, 4f)
            }
            shapeRenderer.color = Color.ORANGE
            shapeRenderer.rect(screenPosition.x - 6.0f, screenPosition.y - 6.0f, 12f, 12f) // Draws an orange square to symbolize being selected
        }
    }

    override fun shouldDraw(time: Int): Boolean {
        if (time < xInterpolator.setPoints.keys.first()) {
            return false
        }
        if (death != null) {
            if (time > death!!) {
                return false
            }
        }
        return true
    }

    override fun toString(): String {
        val output = StringBuilder()
        output.append("Movements: " + xInterpolator.setPoints.keys + "\n")
        output.append("       xs: " + xInterpolator.setPoints.values + "\n")
        output.append("       ys: " + yInterpolator.setPoints.values + "\n")
        return output.toString()
    }
}

data class Unit(
    override var position: Coordinate,
    override val initTime: Int,
    val image: String,
) : ScreenObject() {
    override var xInterpolator: InterpolatedFloat = InterpolatedFloat(position.x, initTime)
    override var yInterpolator: InterpolatedFloat = InterpolatedFloat(position.y, initTime)
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

    override fun clicked(x: Float, y: Float): Boolean {
        return ((x in (screenPosition.x - width / 2)..(screenPosition.x + width / 2)) && (y in (screenPosition.y - height / 2)..(screenPosition.y + height / 2)))
    }

    fun texture(): Texture
    {
        if (texture == null)
        {
            texture = Assets.loadTexture(image)
        }

        return texture!!
    }

    fun draw(batcher: SpriteBatch, sizefactor: Float, font: BitmapFont) {
        // Draw only for the correct country
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

        font.color = Color(255.0f, 63.75f, 0.0f, alpha)
        font.data.setScale((0.3 * sizefactor * sizePresetFactor).toFloat())

        font.draw(batcher, size, screenPosition.x - width / 2, screenPosition.y + height / 2)
        if (name != null) { font.draw(batcher, name, screenPosition.x - width / 2, screenPosition.y) }
        font.draw(batcher, type, screenPosition.x - width / 2, screenPosition.y - height / 2)
    }
}

data class Node(
    override var position: Coordinate,
    override val initTime: Int,
) : ScreenObject(), Object  {
    var color: Color = Color.GREEN
    override var xInterpolator = InterpolatedFloat(position.x, initTime)
    override var yInterpolator = InterpolatedFloat(position.y, initTime)

    init {
        screenPosition = Coordinate(0f, 0f)
    }

    fun update(time: Int, camera: OrthographicCamera) { // Goes to time, and if animation mode is active, draws colored circle
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
    }

    fun draw(shapeRenderer: ShapeRenderer, animationMode: Boolean) {
        if (animationMode) {
            shapeRenderer.color = color
            shapeRenderer.circle(screenPosition.x, screenPosition.y, 7.0f)
        }
    }
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

data class Area (
    override val nodes: MutableList<Node>
) : NodeCollection() {
    var color: AreaColor = AreaColor.RED
    var lineIDAndOrder: List<Pair<Int, Int>> = mutableListOf() // Stores the lineIDs which correspond to lines and an integer which represents at what index in nodes should the line's points be inserted
    override var alpha: Float = 0.2f
    @Transient var drawPoly: MutableList<FloatArray> = mutableListOf()

    fun calculatePolygon(time: Int, animation: Animation) {
        // Converts lineIDs into Line objects from the lineIDAndOrder list
        val convertedLineIDs: MutableList<Pair<Line, Int>> = ArrayList()

        if (lineIDAndOrder == null) {
            lineIDAndOrder = mutableListOf()
        }

        for ((first, second) in lineIDAndOrder) {
            val line = animation.getLineByID(first)

            if (line != null) {
                convertedLineIDs.add(Pair(line, second))
            } else {
                println("Line with ID $first not found")
            }
        }

        val border1D = DoubleArray(nodes.size * 2)
        var poly = DoubleArray(0)

        var n = 0
        updateDrawNodes(time)
        updateNonDrawNodes(time)
        while (n < drawNodes.size) {
            val node = nodes[n]
            border1D[2 * n] = node.screenPosition.x.toDouble()
            border1D[2 * n + 1] = node.screenPosition.y.toDouble()
            n++
        }

        var lastBorderIndex = 0
        for (lineIntPair in convertedLineIDs) {
            //flattens interpolatedX and interpolatedY points into 1D array
            val line: Line = lineIntPair.first
            val linePoly = DoubleArray(line.interpolatedX.size * 2)
            for (i in line.interpolatedX.indices) {
                linePoly[i * 2] = line.interpolatedX[i].toDouble()
                linePoly[i * 2 + 1] = line.interpolatedY[i].toDouble()
            }

            poly += border1D.slice(lastBorderIndex until lineIntPair.second * 2)
            lastBorderIndex = lineIntPair.second * 2
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

    fun update(time: Int, zoom: Float, cx: Float, cy: Float, animation: Animation) {
        for (node in nodes) {
            node.goToTime(time, zoom, cx, cy)
        }
        calculatePolygon(time, animation)
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
) : NodeCollection() {
    @Transient var interpolatedX: Array<Float> = arrayOf()
    @Transient var interpolatedY: Array<Float> = arrayOf()
    var lineThickness: Float = 5.0f
    var color: AreaColor = AreaColor.RED
    override var alpha: Float = 1.0f

    fun update(linesPerNode: Int, time: Int) : Boolean {
        lineThickness = 5.0f
        color = AreaColor.RED
        //reset values to nothing by default
        interpolatedX = arrayOf()
        interpolatedY = arrayOf()

        updateDrawNodes(time)
        updateNonDrawNodes(time)
        val numLines = drawNodes.size * linesPerNode

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
            interpolatedX = Array(numLines + 1) { 0.0f }
            interpolatedY = Array(numLines + 1) { 0.0f }

            val xInterpolator = Interpolator(evalAt, xValues)
            val yInterpolator = Interpolator(evalAt, yValues)

            i = 0
            var eval: Double
            while (i < numLines) {
                eval = (drawNodes.size - 1.00) * i / numLines
                interpolatedX[i] = xInterpolator.interpolateAt(eval).toFloat()
                interpolatedY[i] = yInterpolator.interpolateAt(eval).toFloat()
                i++
            }

            interpolatedX[numLines] = xInterpolator.interpolateAt((drawNodes.size.toFloat() - 1.00)).toFloat()
            interpolatedY[numLines] = yInterpolator.interpolateAt((drawNodes.size.toFloat() - 1.00)).toFloat()
            return true
        }
        return false
    }

    fun draw(shapeRenderer: ShapeRenderer) {
        if (drawNodes.size >= AnimationScreen.MIN_LINE_SIZE) {
            shapeRenderer.color = color.color
            for (i in 0 until AnimationScreen.LINES_PER_NODE * drawNodes.size) {
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