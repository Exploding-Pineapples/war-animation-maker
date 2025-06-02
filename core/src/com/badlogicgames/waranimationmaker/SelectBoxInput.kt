package com.badlogicgames.waranimationmaker

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Array

class SelectBoxInput<T: Any> (skin: Skin?, output: (T?) -> Unit, val input: () -> T?, clazz: Class<T>, name: String, val choices: Array<T>, converter: ((String) -> T)? = null) : InputElement<T>(skin, output, clazz, name, converter) {
    @Transient override var inputElement: Actor? = null

    override fun show(verticalGroup: VerticalGroup, inSkin: Skin) {
        if (!displayed) {
            val skin: Skin = inSkin
            table = Table()
            val nameLabel = Label(name, skin)
            val selectBox = SelectBox<T>(skin)
            selectBox.setItems(choices)
            selectBox.selected = input.invoke()
            inputElement = selectBox
            selectBox.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    println("selectbox changed: " + selectBox.selected)
                    output.invoke(selectBox.selection.lastSelected)
                }
            })

            table!!.add(nameLabel)
            table!!.add(selectBox).pad(10.0f)
            table!!.row()

            verticalGroup.addActor(table)
            displayed = true
        }
    }
}