package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogicgames.waranimationmaker.WarAnimationMaker


class UnitHandler(
    private val animation: Animation
)
{
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

    fun newUnit(position: Coordinate, initTime: Int, image: String): Unit {
        return add(Unit(UnitID(animation.unitId), position, initTime, image))
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

    fun getDrawUnits(time: Int): List<Unit> {
        val out = mutableListOf<Unit>()
        for (unit in animation.units) {
            if (time >= unit.xInterpolator.setPoints.keys.first()) {
                if (!unit.death.value) {
                    out.add(unit)
                }
            }
        }
        return out
    }

    fun getNonDrawUnits(time: Int): List<Unit> {
        val out = mutableListOf<Unit>()
        for (unit in animation.units) {
            if (time <= unit.xInterpolator.setPoints.keys.first()) {
                out.add(unit)
            } else {
                if (unit.death.value) {
                    out.add(unit)
                }
            }
        }
        return out
    }
}
