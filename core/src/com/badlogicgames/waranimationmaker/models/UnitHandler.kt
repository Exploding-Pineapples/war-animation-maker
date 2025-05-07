package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogicgames.waranimationmaker.WarAnimationMaker


class UnitHandler(val animation: Animation)
{
    fun init() {
        animation.units.forEach { it.alpha.update(animation.initTime) }
    }

    fun buildInputs() {
        for (unit in animation.units) {
            unit.buildInputs()
        }
    }

    fun clicked(x: Float, y: Float) = animation.units
        .firstOrNull {
            it.clicked(x, y)
        }

    fun add(unit: Unit) : Unit
    {
        unit.typeTexture()
        unit.buildInputs()
        animation.units.add(unit)
        animation.unitId++
        return unit
    }

    fun newUnit(x: Float, y: Float, time: Int) : Unit {
         return add(Unit(Coordinate(x, y), time))
    }

    fun newUnit(position: Coordinate, initTime: Int, image: String): Unit {
        return add(Unit(position, initTime, image))
    }

    fun update(time: Int, camera: OrthographicCamera, paused: Boolean) {
        animation.units.forEach { it.goToTime(time, camera.zoom, camera.position.x, camera.position.y, paused) }
    }

    fun draw(game: WarAnimationMaker, shapeRenderer: ShapeRenderer, zoomFactor: Float) {
        for (unit in animation.units) {
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
            unit.draw(game.batcher, shapeRenderer, zoomFactor, game.bitmapFont, game.fontShader, game.layout)
        }
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    fun remove(unit: Object): Boolean
    {
        return animation.units.remove(unit)
    }

}
