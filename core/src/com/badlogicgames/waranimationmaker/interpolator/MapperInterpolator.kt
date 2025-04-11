package com.badlogicgames.waranimationmaker.interpolator

inline fun <I : Number, O, reified NO, reified NI : Number> Interpolator<I, O>.map(
    noinline mapIn: (I) -> NI,
    noinline mapOut: (O) -> NO,
    noinline mapNewToOriginalIn: (NI) -> I
): Interpolator<NI, NO> = MapperInterpolator(
    this,
    mapOut,
    mapNewToOriginalIn,
    y.map(mapOut).toTypedArray(),
    x.map(mapIn).toTypedArray()
)

class MapperInterpolator<I : Number, O, NewOutput, NewInput : Number>(
    private val original: Interpolator<I, O>,
    private val mapToOut: (O) -> NewOutput,
    private val mapBackToOriginalIn: (NewInput) -> I,
    convertedArrayOut: Array<NewOutput>,
    convertedArrayIn: Array<NewInput>
) : Interpolator<NewInput, NewOutput>(convertedArrayIn, convertedArrayOut) {

    override fun interpolateAt(xi: NewInput) = mapToOut(
        original.interpolateAt(
            mapBackToOriginalIn(xi)
        )
    )
}