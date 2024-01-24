package com.badlogicgames.superjumper.models

import com.badlogicgames.superjumper.Screen
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.png.PngDirectory
import java.io.File
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos

interface Object
{
    var position: Coordinate
    var screenPosition: Coordinate

    val movementFrames: MutableList<GroupedMovement<Coordinate>>
    var death: Int?

    fun clicked(x: Float, y: Float): Boolean
    {
        return (x - screenPosition.x).absoluteValue <= 10 && (y - screenPosition.y).absoluteValue <= 10
    }
    fun goToTime(time: Int, zoom: Float, cx: Float, cy: Float): Boolean {
        val (length, index, subindex, start, end) = findTime(time)
        val (mode, motion) = getMotion(length, index, subindex)
        val (first, last) = motion

        val xmotion = arrayOf(first.x, last.x)
        val ymotion = arrayOf(first.y, last.y)

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
            index++;
        }

        return arrayOf(1, index - 1, movementFrames[movementFrames.size - 1].frames.keys.size - 1, -1, -1)
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

        var found = false; //Used to overwrite duplicate times

        for (movement in movementFrames) {
            for (t in movement.keys) {
                if (time == t) {
                    movement.frames[t] = Coordinate(x, y)
                    println("Overwrote, new motions: $movementFrames")
                    found = true; //no return yet, if it finds another duplicate time, overwrite
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
                return
            }
            println("Not possible to add new motion during already defined time")
            return
        }

        var found = false; //Used to overwrite duplicate times

        for (movement in movementFrames) {
            for (t in movement.keys) {
                if (time == t) {
                    movement.frames[t] = Coordinate(x, y)
                    println("Overwrote, new motions: $movementFrames")
                    found = true; //no return yet, if it finds another duplicate time, overwrite
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
            zoom = zfirst;
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
            index++;
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

        var found = false; //Used to overwrite duplicate times

        for (movement in movementFrames) {
            for (t in movement.keys) {
                if (time == t) {
                    movement.frames[t] = Coordinate(x, y) to zoom
                    println("Overwrote, new motions: $movementFrames")
                    found = true; //no return yet, if it finds another duplicate time, overwrite
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
    val frames: MutableMap<Int, T> = mutableMapOf()
) : MutableMap<Int, T> by frames

data class Unit(
    val image: String,
    override val movementFrames: MutableList<GroupedMovement<Coordinate>> = mutableListOf(),
    override var death: Int? = null,
    override var position: Coordinate,
    override var screenPosition: Coordinate
) : Object

data class Node
    @JvmOverloads
constructor(
    override val movementFrames: MutableList<GroupedMovement<Coordinate>> = mutableListOf(),
    override var death: Int? = null,
    override var position: Coordinate,
    override var screenPosition: Coordinate
) : Object

data class Line(
    val nodes: MutableList<Node> = mutableListOf()
)

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
    val area: Line = Line(),
    val units: MutableList<Unit> = mutableListOf(),
    private var camera: Camera? = Camera(),
    val lines: MutableList<Line> = mutableListOf()
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

    fun getLineOfNode(node: Object): Int {
        var id = 0;
        for (line in lines) {
            if (line.nodes.contains(node)) {
                return id
            }
            id++;
        }
        return -1
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