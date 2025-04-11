package com.badlogicgames.waranimationmaker.interpolator

abstract class Interpolator<I : Number, O>(x: Array<I>, y: Array<O>) {
    val x: Array<I>
    val y: Array<O>

    init {
        require(x.size == y.size) { "The lengths of x and y must be the same." }
        this.x = x
        this.y = y
    }

    abstract fun interpolateAt(xi: I): O
}
