package com.badlogicgames.waranimationmaker

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogicgames.waranimationmaker.models.NodeCollection


class InputElement<T> private constructor(builder: Builder<T>) {
    val table: Table = builder.table
    val textField: TextField = builder.textField
    val input = builder.input
    val requiredSelectedTypes = builder.requiredSelectedTypes
    val requiredSelectedNodeCollectionTypes = builder.requiredSelectedNodeCollectionTypes
    var displayed: Boolean = false

    fun shouldDisplay(selected: Any?, selectedNodeCollection: NodeCollection?, animationMode: Boolean): Boolean {
        if (!animationMode) {
            return false
        }
        if (selected == null) {
            return false
        }
        if (requiredSelectedTypes.isNotEmpty()) {
            var found = false
            for (type in requiredSelectedTypes) {
                if (type.isAssignableFrom(selected.javaClass)) {
                    found = true
                    break
                }
            }
            if (!found) return false
        }
        if (requiredSelectedNodeCollectionTypes.isNotEmpty()) {
            var found = false
            for (type in requiredSelectedNodeCollectionTypes) {
                if (selectedNodeCollection != null) {
                    if (type.isAssignableFrom(selectedNodeCollection.javaClass)) {
                        found = true
                        break
                    }
                }
            }
            if (!found) return false
        }
        return true
    }

    fun update(selected: Any?, selectedNodeCollection: NodeCollection?, table: VerticalGroup, animationMode: Boolean) {
        val shouldDisplay = shouldDisplay(selected, selectedNodeCollection, animationMode)
        if (shouldDisplay) {
            textField.setText(input.invoke())
            if (!displayed) {
                display(table)
                displayed = true
            }
        } else {
            if (displayed) {
                this.table.remove()
                // Remove cell from table
                table.layout()
                displayed = false
            }
        }
    }

    fun display(table: VerticalGroup) {
        if (!displayed) {
            table.addActor(this.table)
            displayed = true
        }
    }

    companion object {

        // String, Integer, Double, Float
        private val converters = mutableMapOf<Class<*>, (String) -> Any>(
            String::class.java to {
                it
            },
            Integer::class.java to {
                it.toInt()
            },
            Float::class.java to {
                it.toFloat()
            }
        )

        class Builder<T>(skin: Skin, var output: (T?) -> Unit, var input: () -> String?, private val clazz: Class<T>, var name: String) {
            val requiredSelectedNodeCollectionTypes = mutableListOf<Class<*>>()
            var table = Table()
            var textField = TextField(input.invoke(), skin)
            var nameLabel = Label(name, skin)
            var requiredSelectedTypes = mutableListOf<Class<*>>()
            var converter: ((String) -> T)? = null

            fun requiredSelectedNodeCollectionTypes(vararg classes: Class<*>): Builder<T> {
                requiredSelectedNodeCollectionTypes.addAll(classes)
                return this
            }

            fun requiredSelectedTypes(vararg classes: Class<*>): Builder<T> {
                requiredSelectedTypes.addAll(classes)
                return this
            }

            fun converter(converter: (String) -> T): Builder<T> {
                this.converter = converter
                return this
            }

            fun build(): InputElement<T> {
                table.add(nameLabel)
                table.add(textField).pad(10.0f)
                table.row()
                textField.addListener(object : InputListener() {
                    override fun keyTyped(event: InputEvent, character: Char): Boolean {
                        val existing = converters[clazz]
                            ?: converter
                            ?: throw IllegalArgumentException("No converter for $clazz")
                        if (textField.text.equals("")) {
                            output.invoke(null)
                        } else {
                            output.invoke(existing(textField.text) as T)
                        }
                        return true
                    }
                })
                return InputElement(this)
            }
        }
    }
}