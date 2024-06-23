package com.badlogicgames.superjumper.models

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogicgames.superjumper.AreaColor
import com.badlogicgames.superjumper.Screen
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.png.PngDirectory
import earcut4j.Earcut
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import java.io.File
import java.util.*
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos

interface Object
{
    var position: Coordinate
    var screenPosition: Coordinate

    val movementFrames: MutableList<GroupedMovement<Coordinate>>
    var death: Int?

    var alpha: Float

    fun clicked(x: Float, y: Float): Boolean
    {
        return (x - screenPosition.x).absoluteValue <= 10 && (y - screenPosition.y).absoluteValue <= 10
    }
    fun goToTime(time: Int, zoom: Float, cx: Float, cy: Float): Boolean { //can only be called after at least one key frame has been added
        val (length, index, subindex, start, end) = findTime(time)
        val (mode, motion) = getMotion(length, index, subindex)
        val (first, last) = motion

        val xmotion = arrayOf(first.x, last.x)
        val ymotion = arrayOf(first.y, last.y)

        if (death != null) { //checks for death time
            alpha = 0.0f.coerceAtLeast((1.0f - (time - death!!) / 100f))
        }

        //alpha = Math.min(alpha, movementFrames[0].frames.entries.first().key);

        if (mode == "single") {
            position.x = xmotion[0]
            position.y = ymotion[0]
            if (start == -1) {
                screenPosition.x = position.x * zoom - cx * (zoom - 1) + (Screen.DISPLAY_WIDTH / 2 - cx)
                screenPosition.y = position.y * zoom - cy * (zoom - 1) + (Screen.DISPLAY_HEIGHT / 2 - cy)
                return false
            }
        }

        val distancex = xmotion[1] - xmotion[0]
        val distancey = ymotion[1] - ymotion[0]
        val deltatime = end - start
        if (mode == "double") {
            position.x =
                (xmotion[0] - 0.5 * distancex * cos((PI / deltatime) * (time - start)) + 0.5 * distancex).toFloat()
            position.y =
                (ymotion[0] - 0.5 * distancey * cos((PI / deltatime) * (time - start)) + 0.5 * distancey).toFloat()
        }
        if (mode == "speedup") {
            position.x =
                (xmotion[0] + distancex * cos((PI / (2 * deltatime)) * ((time - start) - 2 * deltatime)) + distancex).toFloat()
            position.y =
                (ymotion[0] + distancey * cos((PI / (2 * deltatime)) * ((time - start) - 2 * deltatime)) + distancey).toFloat()
        }
        if (mode == "slowdown") {
            position.x =
                (xmotion[0] + distancex * cos((PI / (2 * deltatime)) * ((time - start)) - PI / 2)).toFloat()
            position.y =
                (ymotion[0] + distancey * cos((PI / (2 * deltatime)) * ((time - start)) - PI / 2)).toFloat()
        }
        if (mode == "linear") {
            position.x = xmotion[0] + distancex * ((time - start) / deltatime.toFloat())
            position.y = ymotion[0] + distancey * ((time - start) / deltatime.toFloat())
        }
        screenPosition.x = position.x * zoom - cx * (zoom - 1) + (Screen.DISPLAY_WIDTH / 2 - cx)
        screenPosition.y = position.y * zoom - cy * (zoom - 1) + (Screen.DISPLAY_HEIGHT / 2 - cy)
        return true
    }

    fun findTime(time: Int): Array<Int> {
        var index = 0
        if (time < movementFrames[0].frames.keys.toList()[0]) {
            return arrayOf(1, 0, 0, -1, -1)
        }

        for (frame in movementFrames) {
            val times = frame.frames.keys.toList()
            for (subindex in 1 until times.size) {
                if ((time >= times[subindex - 1]) && (time < times[subindex])) {
                    return arrayOf(times.size, index, subindex, times[subindex - 1], times[subindex])
                }
            }
            index++
        }

        return arrayOf(1, index - 1, movementFrames[movementFrames.size - 1].frames.keys.size - 1, -1, -1)
    }

    fun removeFrame(time: Int): Boolean {
        if ((movementFrames.size > 1)||(movementFrames.first().size > 1))
        for (frame in movementFrames) {
            for (t in frame.keys) {
                frame.remove(t)
                return true
            }
        }
        return false
    }

    fun getMotion(length: Int, index: Int, subindex: Int): Pair<String, Pair<Coordinate, Coordinate>> {
        val motion = movementFrames[index]
        val frames = motion.frames.entries.toList()
        if (length == 1) {
            return "single" to (frames[subindex].value to frames[subindex].value)
        }
        if (length == 2) {
            return "double" to (frames[subindex-1].value to frames[subindex].value)
        }
        if (length > 2) {
            if (subindex == 1) {
                return "speedup" to (frames[0].value to frames[subindex].value)
            }
            if (subindex == length - 1) {
                return "slowdown" to (frames[subindex - 1].value to frames[subindex].value)
            }
            return "linear" to (frames[subindex - 1].value to frames[subindex].value)
        }

        throw IllegalStateException("what the fuck did you do")
    }

    fun newSetPoint(time: Int, x: Float, y: Float) {
        // for (Class variableName : listOfItems) {}

        if (movementFrames.isEmpty()) {
            movementFrames += GroupedMovement()
        }

        val lastGroup = movementFrames.last()

        if (time > (lastGroup.keys.lastOrNull() ?: -1)) {
            lastGroup[time] = Coordinate(x, y)
            println("Appended, new motions: $movementFrames")
            return
        }

        var found = false //Used to overwrite duplicate times

        for (movement in movementFrames) {
            for (t in movement.keys) {
                if (time == t) {
                    movement.frames[t] = Coordinate(x, y)
                    println("Overwrote, new motions: $movementFrames")
                    found = true //no return yet, if it finds another duplicate time, overwrite
                }
                if (t > time) {
                    if (found) { //if t > time and the time was found, all duplicate times have been overwritten already so return
                        return
                    }
                    movement[time] = Coordinate(x, y)

                    val clone = movement.frames.toMap()
                    movement.frames.clear()

                    clone.keys.sorted().forEach {
                        movement[it] = clone[it]!!
                    }
                    println("Inserted, new motions: $movementFrames")
                    return
                }
            }
        }
    }
    fun newSetPoint(time: Int, x: Float, y: Float, new: Boolean) {  //new = whether to create a new motion or not
        if (movementFrames.isEmpty()) {
            movementFrames += GroupedMovement()
        }

        val lastGroup = movementFrames.last()

        if (new) {
            if (time > lastGroup.keys.last()) {
                movementFrames += GroupedMovement(
                    mutableMapOf(
                        lastGroup.keys.last() to Coordinate(
                            lastGroup.frames.entries.last().value.x,
                            lastGroup.frames.entries.last().value.y
                        ),
                        time to Coordinate(x, y)
                    )
                )
                movementFrames += GroupedMovement(
                    mutableMapOf(
                        time to Coordinate(x, y)
                    )
                )
                println("Added new motion: $movementFrames")
                return
            }
            println("Not possible to add new motion during already defined time")
            return
        }

        var found = false //Used to overwrite duplicate times

        for (movement in movementFrames) {
            for (t in movement.keys) {
                if (time == t) {
                    movement.frames[t] = Coordinate(x, y)
                    println("Overwrote, new motions: $movementFrames")
                    found = true //no return yet, if it finds another duplicate time, overwrite
                }
                if (t > time) {
                    if (found) { //if t > time and the time was found, all duplicate times have been overwritten already so return
                        return
                    }
                    movement[time] = Coordinate(x, y)

                    val clone = movement.frames.toMap()
                    movement.frames.clear()

                    clone.keys.sorted().forEach {
                        movement[it] = clone[it]!!
                    }
                    println("Inserted, new motions: $movementFrames")
                    return
                }
            }
        }
    }
}

interface ObjectWithZoom
{
    var position: Coordinate
    var zoom: Float //zoom for camera only

    val movementFrames: MutableList<GroupedMovement<Pair<Coordinate, Float>>>
    fun goToTime(time: Int): Boolean {
        val (length, index, subindex, start, end) = findTime(time)
        val (mode, motion) = getMotion(length, index, subindex)
        val (coordinates, zooms) = motion

        val xmotion = arrayOf(coordinates.first.x, coordinates.second.x)
        val ymotion = arrayOf(coordinates.first.y, coordinates.second.y)
        val (zfirst, zsecond) = zooms


        if (mode == "single") {
            position.x = xmotion[0]
            position.y = ymotion[0]
            zoom = zfirst
            if (start == -1) {
                return false
            }
        }

        val distancex = xmotion[1] - xmotion[0]
        val distancey = ymotion[1] - ymotion[0]
        val deltatime = end - start
        val deltaz = zsecond - zfirst

        if (mode == "double") {
            position.x = (xmotion[0] - 0.5 * distancex * cos((PI / deltatime) * (time - start)) + 0.5 * distancex).toFloat()
            position.y = (ymotion[0] - 0.5 * distancey * cos((PI / deltatime) * (time - start)) + 0.5 * distancey).toFloat()
            zoom = (zfirst - 0.5 * deltaz * cos((PI / deltatime) * (time - start)) + 0.5 * deltaz).toFloat()
        }
        if (mode == "speedup") {
            position.x = (xmotion[0] + distancex * cos((PI / (2 * deltatime)) * ((time - start) - 2 * deltatime)) + distancex).toFloat()
            position.y = (ymotion[0] + distancey * cos((PI / (2 * deltatime)) * ((time - start) - 2 * deltatime)) + distancey).toFloat()
            zoom = (zfirst + deltaz * cos((PI / (2 * deltatime)) * ((time - start) - 2 * deltatime)) + deltaz).toFloat()
        }
        if (mode == "slowdown") {
            position.x = (xmotion[0] + distancex * cos((PI / (2 * deltatime)) * ((time - start)) - PI / 2)).toFloat()
            position.y = (ymotion[0] + distancey * cos((PI / (2 * deltatime)) * ((time - start)) - PI / 2)).toFloat()
            zoom = (zfirst + deltaz * cos((PI / (2 * deltatime)) * ((time - start)) - PI / 2)).toFloat()
        }
        if (mode == "linear") {
            position.x = xmotion[0] + distancex * ((time - start) / deltatime.toFloat())
            position.y = ymotion[0] + distancey * ((time - start) / deltatime.toFloat())
            zoom = zfirst + deltaz * ((time - start) / deltatime.toFloat())
        }
        return true
    }

    fun findTime(time: Int): Array<Int> {
        var index = 0
        if (time < movementFrames[0].frames.keys.toList()[0]) {
            return arrayOf(1, 0, 0, -1, -1)
        }

        for (frame in movementFrames) {
            val times = frame.frames.keys.toList()
            for (subindex in 1 until times.size) {
                if ((time >= times[subindex - 1]) && (time < times[subindex])) {
                    return arrayOf(times.size, index, subindex, times[subindex - 1], times[subindex])
                }
            }
            index++
        }
        return arrayOf(1, index - 1, movementFrames[movementFrames.size - 1].frames.keys.size - 1, -1, -1)
    }

    fun getMotion(length: Int, index: Int, subindex: Int): Pair<String, Pair<Pair<Coordinate, Coordinate>, Pair<Float, Float>>> {
        val motion = movementFrames[index]
        val frames = motion.frames.entries.toList()
        val curframe = frames[subindex].value
        if (length == 1) {
            return "single" to ((curframe.first to curframe.first) to (curframe.second to curframe.second)) //if only one frame defined, make a dummy motion with no movement (beginning and end are the same thing)
        }

        val prevframe = frames[subindex - 1].value
        if (length == 2) { //if two frames or more are defined, return their motion
            return "double" to ((prevframe.first to curframe.first) to (prevframe.second to curframe.second)) //2 frames only means return a speed up and slow down
        }
        if (length > 2) {
            if (subindex == 1) {
                return "speedup" to ((prevframe.first to curframe.first) to (prevframe.second to curframe.second)) //if more than 2 frames, first motion is speed up
            }
            if (subindex == length - 1) {
                return "slowdown" to ((prevframe.first to curframe.first) to (prevframe.second to curframe.second)) //if more than 2 frmes, last motion is slow down
            }
            return "linear" to ((prevframe.first to curframe.first) to (prevframe.second to curframe.second)) //if more than 2 frames, all middle frames are linear
        }

        throw IllegalStateException("what the fuck did you do")
    }

    fun newSetPoint(time: Int, x: Float, y: Float, zoom: Float) {
        // for (Class variableName : listOfItems) {}

        if (movementFrames.isEmpty()) {
            movementFrames += GroupedMovement()
        }

        val lastGroup = movementFrames.last()

        if (time > (lastGroup.keys.lastOrNull() ?: -1)) {
            lastGroup[time] = Coordinate(x, y) to zoom
            println("Appended, new motions: $movementFrames")
            return
        }

        var found = false //Used to overwrite duplicate times

        for (movement in movementFrames) {
            for (t in movement.keys) {
                if (time == t) {
                    movement.frames[t] = Coordinate(x, y) to zoom
                    println("Overwrote, new motions: $movementFrames")
                    found = true //no return yet, if it finds another duplicate time, overwrite
                }
                if (t > time) {
                    if (found) { //if t > time and the time was found, all duplicate times have been overwritten already so return
                        return
                    }
                    movement[time] = Coordinate(x, y) to zoom

                    val clone = movement.frames.toMap()
                    movement.frames.clear()

                    clone.keys.sorted().forEach {
                        movement[it] = clone[it]!!
                    }
                    println("Inserted, new motions: $movementFrames")
                    return
                }
            }
        }
    }
}

data class Coordinate(
    var x: Float,
    var y: Float
)

data class GroupedMovement<T>(
    val frames: TreeMap<Int, T> = TreeMap()
) : MutableMap<Int, T> by frames
{
    constructor(frames: MutableMap<Int, T>) : this()
    {
        this.frames.putAll(frames)
    }
}

data class Unit(
    val image: String,
    override val movementFrames: MutableList<GroupedMovement<Coordinate>> = mutableListOf(),
    override var death: Int? = null,
    override var position: Coordinate,
    override var screenPosition: Coordinate,
    override var alpha: Float = 0.0f
) : Object

data class Node
    @JvmOverloads
constructor(
    override val movementFrames: MutableList<GroupedMovement<Coordinate>> = mutableListOf(),
    override var death: Int? = null,
    override var position: Coordinate,
    override var screenPosition: Coordinate,
    override var alpha: Float = 0.0f,
) : Object

data class Area(
    val nodes: MutableList<Node> = mutableListOf(),
    val color: AreaColor = AreaColor.RED,
    var lineIDs: List<Pair<Int, Int>> = mutableListOf(),
    var drawPoly: MutableList<FloatArray> = mutableListOf()
) {
    fun calculatePolygon(lines: List<Pair<Line, Int>>) {
        val border1D = DoubleArray(nodes.size * 2)
        var poly = DoubleArray(0)

        var n = 0
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
    val nodes: MutableList<Node> = mutableListOf(),
    var interpolatedX: Array<Float> = arrayOf(),
    var interpolatedY: Array<Float> = arrayOf(),
) {
    fun getDrawNodes(time: Int): List<Node> {
        val out = mutableListOf<Node>()
        for (n in nodes) {
            if (time >= n.movementFrames.first().keys.first()) {
                out.add(n)
            }
        }
        return out
    }

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
            if (!(node.death != null && time > node.death!!)) {
                xValues[nodeIndex] = node.screenPosition.x.toDouble()
                yValues[nodeIndex] = node.screenPosition.y.toDouble()
            }
        }

        if (drawNodes.size > Screen.MIN_LINE_SIZE) {
            interpolatedX = Array(num + 1) { 0.0f }
            interpolatedY = Array(num + 1) { 0.0f }

            val xInterpolator = Animation.interpolator.interpolate(evalAt, xValues)
            val yInterpolator = Animation.interpolator.interpolate(evalAt, yValues)

            if (drawNodes.size > Screen.MIN_LINE_SIZE) {
                i = 0
                var eval: Double
                while (i < num) {
                    eval = (drawNodes.size.toFloat() - 1.00) * i / num
                    interpolatedX[i] = xInterpolator.value(eval).toFloat()
                    interpolatedY[i] = yInterpolator.value(eval).toFloat()
                    i++
                }
            }

            interpolatedX[num] = xInterpolator.value((drawNodes.size.toFloat() - 1.00)).toFloat()
            interpolatedY[num] = yInterpolator.value((drawNodes.size.toFloat() - 1.00)).toFloat()
            return true
        }
        return false
    }
}

data class Camera(
    override var position: Coordinate = Coordinate(x = 960.0f, y = 540.0f),
    override var zoom: Float = 1.0f,
    override val movementFrames: MutableList<GroupedMovement<Pair<Coordinate, Float>>> = mutableListOf()
) : ObjectWithZoom

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
        animation.units += unit
    }

    fun loop()
    {}

    fun delete(unit: Unit)
    {
        animation.units -= unit
    }
}

data class Animation(
    val path: String,
    val name: String = "My Animation",
    val units: MutableList<Unit> = mutableListOf(),
    private var camera: Camera? = Camera(),
    val lines: MutableList<Line> = mutableListOf(),
    val areas: MutableList<Area> = mutableListOf()
)
{
    private var cachedImageDimensions: Pair<Int, Int>? = null

    fun camera(): Camera
    {
        if (camera == null)
        {
            camera = Camera(
                movementFrames = mutableListOf(
                    GroupedMovement(
                        frames = mutableMapOf(
                            0 to (Coordinate(x = 0.0f, y = 0.0f) to 1.0f)
                        )
                    )
                )
            )
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

    fun getListOfObject(obj: Object): List<Object> {
        var id = 0
        for (line in lines) {
            if (line.nodes.contains(obj)) {
                return line.nodes
            }
            id++
        }
        for (area in areas) {
            if (area.nodes.contains(obj)) {
                return area.nodes
            }
            id++
        }
        for (unit in units) {
            if (unit == obj) {
                return units
            }
        }
        return emptyList()
    }

    fun getAreaOfNode(node: Object): Area {
        for (a in areas) {
            if (a.nodes.contains(node)) {
                return a
            }
        }
        return Area()
    }

    fun getListOfNode(node: Object): List<Node> {
        var id = 0
        for (line in lines) {
            if (line.nodes.contains(node)) {
                return line.nodes
            }
            id++
        }
        for (area in areas) {
            if (area.nodes.contains(node)) {
                return area.nodes
            }
            id++
        }

        return emptyList()
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

        val interpolator = SplineInterpolator()
        fun getInterpolator(evalAt: DoubleArray, values: DoubleArray): PolynomialSplineFunction {
            return interpolator.interpolate(evalAt, values)
        }

        //Update all nodes and interpolate line
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