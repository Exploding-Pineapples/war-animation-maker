package com.badlogicgames.waranimationmaker.interpolator

inline fun <I : Number, O, reified NO, reified NI : Number> InterpolationFunction<I, O>.map(
    noinline mapIn: (I) -> NI,
    noinline mapOut: (O) -> NO,
    noinline mapNewToOriginalIn: (NI) -> I
): InterpolationFunction<NI, NO> = MapperInterpolationFunction(
    this,
    mapOut,
    mapNewToOriginalIn,
    o.map(mapOut).toTypedArray(),
    i.map(mapIn).toTypedArray()
)

class MapperInterpolationFunction<I : Number, O, NewOutput, NewInput : Number>(
    private val original: InterpolationFunction<I, O>,
    private val mapToOut: (O) -> NewOutput,
    private val mapBackToOriginalIn: (NewInput) -> I,
    convertedArrayOut: Array<NewOutput>,
    convertedArrayIn: Array<NewInput>
) : InterpolationFunction<NewInput, NewOutput>(convertedArrayIn, convertedArrayOut) {

    override fun evaluate(at: NewInput) = mapToOut(
        original.evaluate(
            mapBackToOriginalIn(at)
        )
    )

    override fun init() {
        TODO("Not yet implemented")
    }
}