package com.badlogicgames.waranimationmaker

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.*


class InputElement<T> (val skin: Skin?, var output: (T?) -> Unit, val input: () -> String?, val clazz: Class<T>, var name: String, var converter: ((String) -> T)? = null) {
    @Transient var table: Table? = null
    @Transient var textField: TextField? = null
    var displayed: Boolean = false

    fun hide(verticalGroup: VerticalGroup) {
        if (displayed) {
            verticalGroup.layout()
            table!!.remove()
            table = null
            textField = null
            displayed = false
        }
    }

    fun show(verticalGroup: VerticalGroup, inSkin: Skin?) {
        val skin: Skin? = inSkin?: this.skin
        if (!displayed) {
            table = Table()
            val nameLabel = Label(name, skin)
            textField = TextField(input.invoke(), skin)
            textField!!.setText(input.invoke())
            textField!!.addListener(object : InputListener() {
                override fun keyTyped(event: InputEvent, character: Char): Boolean {
                    val existing = converters[clazz]
                        ?: converter
                        ?: throw IllegalArgumentException("No converter for $clazz")
                    if (textField!!.text.equals("")) {
                        output.invoke(null)
                    } else {
                        output.invoke(existing(textField!!.text) as T)
                    }
                    return true
                }
            })

            table!!.add(nameLabel)
            table!!.add(textField).pad(10.0f)
            table!!.row()

            verticalGroup.addActor(table)
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
            Int::class.java to {
                it.toInt()
            },
            Float::class.java to {
                it.toFloat()
            }
        )
    }
}