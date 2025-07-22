package com.badlogicgames.waranimationmaker.interpolator

abstract class InterpolationFunction<I : Number, O>(i: Array<I>, o: Array<O>) {
    var i: Array<I>
    var o: Array<O>

    init {
        require(i.size == o.size) { "The lengths of x and y must be the same." }
        this.i = i
        this.o = o
    }

    abstract fun evaluate(at: I): O
}
