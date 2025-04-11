package com.badlogicgames.waranimationmaker.interpolator;

public class StepInterpolator<I extends Number, O> extends Interpolator<I, O> {
    public StepInterpolator(I[] x, O[] y) {
        super(x, y);
    }

    @Override
    public O interpolateAt(I xi) {
        int i = 0;
        if (xi.doubleValue() < getX()[0].doubleValue()) {
            return getY()[0];
        }

        if (xi.doubleValue() > getX()[getX().length - 1].doubleValue()) {
            return getY()[getX().length - 1];
        }

        for (I definedTime : getX()) {
            if (definedTime.doubleValue() >= xi.doubleValue()) {
                return getY()[i];
            }
            i++;
        }
        throw new IllegalStateException("Step interpolation failed somehow");
    }
}
