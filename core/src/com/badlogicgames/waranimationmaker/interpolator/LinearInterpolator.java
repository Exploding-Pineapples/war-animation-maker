package com.badlogicgames.waranimationmaker.interpolator;

public class LinearInterpolator extends Interpolator {

    public LinearInterpolator(double[] x, double[] y) {
        super(x, y);
    }

    @Override
    public double interpolateAt(double xi) {
        if (xi <= x[0]) {
            return y[0];
        }
        if (xi >= x[x.length - 1]) {
            return y[y.length - 1];
        }

        int i = 0;
        while (xi > x[i+1]) {
            i++;
        }

        double x0 = x[i];
        double x1 = x[i+1];
        double y0 = y[i];
        double y1 = y[i+1];

        return y0 + (y1 - y0) * (xi - x0) / (x1 - x0);
    }
}
