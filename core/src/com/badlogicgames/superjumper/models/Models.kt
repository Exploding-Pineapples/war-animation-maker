package com.badlogicgames.superjumper.models

import com.badlogicgames.superjumper.Screen
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.png.PngDirectory
import java.io.File
import java.util.*
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos

interface Object
{
    var position: Coordinate
    var screenPosition: Coordinate

    val movementFrames: MutableList<GroupedMovement>
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
            position.x = xmotion[0] + distancex * ((time - start) / deltatime)
            position.y = ymotion[0] + distancey * ((time - start) / deltatime)
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
            return
        }

        for (movement in movementFrames) {
            for (t in movement.keys) {
                if (time == t) {
                    movement.frames[t] = Coordinate(x, y)
                    return
                }
                if (t > time) {
                    movement[time] = Coordinate(x, y)

                    val clone = movement.frames.toMap()
                    movement.frames.clear()

                    clone.keys.sorted().forEach {
                        movement[it] = clone[it]!!
                    }
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

data class GroupedMovement(
    val frames: MutableMap<Int, Coordinate> = mutableMapOf()
) : MutableMap<Int, Coordinate> by frames

data class Unit(
    val image: String,
    override val movementFrames: MutableList<GroupedMovement> = mutableListOf(),
    override var death: Int? = null,
    override var position: Coordinate,
    override var screenPosition: Coordinate
) : Object

data class Node(
    override val movementFrames: MutableList<GroupedMovement> = mutableListOf(),
    override var death: Int? = null,
    override var position: Coordinate,
    override var screenPosition: Coordinate
) : Object

data class Line(
    val nodes: MutableList<Node> = mutableListOf()
)

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
    val lines: MutableList<Line> = mutableListOf()
)
{
    private var cachedImageDimensions: Pair<Int, Int>? = null

    fun getImageDimensions(): Pair<Int, Int> {
        if (cachedImageDimensions == null) {
            cachedImageDimensions = File(path)
                .getImageDimensions()
        }

        return cachedImageDimensions!!
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