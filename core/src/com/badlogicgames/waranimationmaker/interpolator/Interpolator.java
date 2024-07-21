package com.badlogicgames.waranimationmaker.interpolator;

public abstract class Interpolator {
    final double[] x;
    final double[] y;

    public Interpolator(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("The lengths of x and y must be the same.");
        }
        this.x = x;
        this.y = y;
    }
    abstract double interpolateAt(double xi);
}
