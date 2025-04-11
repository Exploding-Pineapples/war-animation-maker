package com.badlogicgames.waranimationmaker

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.*

abstract class InputElement<T> (val skin: Skin?, var output: (T?) -> Unit, @Transient val clazz: Class<T>, var name: String, var converter: ((String) -> T)? = null) {
    @Transient var table: Table? = null
    abstract var inputElement: Actor?
    var displayed: Boolean = false

    fun hide(verticalGroup: VerticalGroup) {
        if (displayed) {
            verticalGroup.layout()
            table!!.remove()
            table = null
            inputElement = null
            displayed = false
        }
    }

    abstract fun show(verticalGroup: VerticalGroup, inSkin: Skin)

    companion object {
        // String, Integer, Double, Float
        val converters = mutableMapOf<Class<*>, (String) -> Any>(
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