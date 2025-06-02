package com.badlogicgames.waranimationmaker

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.*

class TextInput<T> (skin: Skin?, output: (T?) -> Unit, val input: () -> String?, clazz: Class<T>, name: String, converter: ((String) -> T)? = null) : InputElement<T>(skin, output, clazz, name, converter) {
    @Transient override var inputElement: Actor? = null

    override fun show(verticalGroup: VerticalGroup, inSkin: Skin) {
        if (!displayed) {
            val skin: Skin = inSkin
            table = Table()
            val nameLabel = Label(name, skin)
            val textField = TextField(input.invoke(), skin)
            inputElement = textField
            textField.setText(input.invoke())
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

            table!!.add(nameLabel)
            table!!.add(textField).pad(10.0f)
            table!!.row()

            verticalGroup.addActor(table)
            displayed = true
        }
    }
}