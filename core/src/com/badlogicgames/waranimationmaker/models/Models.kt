package com.badlogicgames.waranimationmaker.models

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
    fun show(verticalGroup: VerticalGroup, nodeCollection: NodeCollection) {
        text = "Edge Collection ${nodeCollection.id.value}: "
        show(verticalGroup, nodeCollection as HasInputs)
    }
    fun show(verticalGroup: VerticalGroup, arrow: Arrow) {
        text = "Arrow: "
        show(verticalGroup, arrow as HasInputs)
    }
    fun show(verticalGroup: VerticalGroup, mapLabel: MapLabel) {
        text = "Map Label: "
        show(verticalGroup, mapLabel as HasInputs)
    }
    fun show(verticalGroup: VerticalGroup, image: Image) {
        text = "Image: "
        show(verticalGroup, image as HasInputs)
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

interface HasID {
    val id: ID
}

interface HasZoom {
    var zoom: Float
    var zoomInterpolator: PCHIPInterpolatedFloat
}

interface HasScreenPosition {
    var screenPosition: Coordinate
}

interface HasDeath {
    var death: InterpolatedBoolean
}

interface HasAlpha : HasInputs {
    val alpha: LinearInterpolatedFloat

    override fun buildInputs() {
        inputElements.add(TextInput(null, { input ->
            if (input != null) {
                alpha.value = input
            }
        }, label@{
            return@label alpha.value.toString()
        }, Float::class.java, "Set alpha set point"))
    }
}

interface HasColor : HasInputs {
    var color: AreaColor

    override fun buildInputs() {
        inputElements.add(TextInput(null, { input ->
            if (input != null) {
                for (color in AreaColor.entries) {
                    if (input == color.name) {
                        this.color = color
                    }
                }
            }
        }, label@{
            return@label color.name
        }, String::class.java, "Set color"))
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

class NodeCollectionID(override val value: Int = -1) : ID {
    override fun getAbstractType() = NodeCollectionID::class.java
}

class NodeID(override val value: Int = -1) : ID {
    override fun getAbstractType() = NodeID::class.java
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