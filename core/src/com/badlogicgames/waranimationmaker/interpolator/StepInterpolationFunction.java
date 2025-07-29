package com.badlogicgames.waranimationmaker.interpolator;

public class StepInterpolationFunction<I extends Number, O> extends InterpolationFunction<I, O> {
    public StepInterpolationFunction(I[] x, O[] y) {
        super(x, y);
    }

    @Override
    public O evaluate(I at) {
        int i = 0;
        if (at.doubleValue() <= getI()[0].doubleValue()) {
            return getO()[0];
        }

        if (at.doubleValue() >= getI()[getI().length - 1].doubleValue()) {
            return getO()[getI().length - 1];
        }

        for (I definedTime : getI()) {
            if (definedTime.doubleValue() > at.doubleValue()) {
                return getO()[i - 1];
            }
            i++;
        }
        throw new IllegalStateException("Step interpolation failed somehow");
    }

    @Override
    public void init() {

    }
}
