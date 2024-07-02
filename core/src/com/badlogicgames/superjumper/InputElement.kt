package com.badlogicgames.superjumper

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextField

class InputElement<T> private constructor(builder: Builder<T>) {
    val table: Table = builder.table
    val textField: TextField = builder.textField
    val input = builder.input
    val requiredSelectedTypes = builder.requiredSelectedTypes
    var displayed: Boolean = false

    fun update(selected: Any?, table: Table, animationMode: Boolean) {
        if (animationMode) {
            if (selected == null) {
                if (displayed) {
                    this.table.remove()
                    displayed = false
                }
            } else {
                if (requiredSelectedTypes.isEmpty()) {
                    if (!displayed) {
                        display(table)
                    }
                } else {
                    for (type in requiredSelectedTypes) {
                        if (type.isAssignableFrom(selected.javaClass)) {
                            display(table)
                            break
                        }
                    }
                }
            }
        } else {
            if (displayed) {
                this.table.remove()
                displayed = false
            }
        }
    }

    fun display(table: Table) {
        if (!displayed) {
            textField.setText(input.invoke())
            table.add(this.table)
            table.row()
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

        class Builder<T>(skin: Skin, var output: (T) -> Unit, var input: () -> String, private val clazz: Class<T>, var name: String) {
            var table = Table()
            var textField = TextField(input.invoke(), skin)
            var nameLabel = Label(name, skin)
            var requiredSelectedTypes = mutableListOf<Class<*>>()
            var converter: ((String) -> T)? = null

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

                        output.invoke(existing(textField.text) as T)
                        return true
                    }
                })
                return InputElement(this)
            }
        }
    }
}