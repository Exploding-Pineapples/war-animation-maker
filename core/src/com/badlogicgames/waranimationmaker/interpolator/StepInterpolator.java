package com.badlogicgames.waranimationmaker.interpolator;

import java.util.Arrays;

public class StepInterpolator extends Interpolator {
    public StepInterpolator(double[] x, double[] y) {
        super(x, y);
    }

    @Override
    double interpolateAt(double xi) {
        return y[Arrays.binarySearch(x, xi)];
    }
}
