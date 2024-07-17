package com.badlogicgames.waranimationmaker

import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Skin

class DropdownElement<T>(skin: Skin, val inputOptions: MutableList<(T) -> Unit> = mutableListOf()) {
    val selectBox: SelectBox<T> = SelectBox(skin)
}