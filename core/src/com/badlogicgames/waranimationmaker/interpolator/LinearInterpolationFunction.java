package com.badlogicgames.waranimationmaker.interpolator;

public class LinearInterpolationFunction<I extends Number> extends InterpolationFunction<I, Double> {

    public LinearInterpolationFunction(I[] x, Double[] y) {
        super(x, y);
    }

    @Override
    public Double evaluate(I at) {
        I[] x = getI();
        Double[] y = getO();
        if (at.doubleValue() <= x[0].doubleValue()) {
            return y[0];
        }
        if (at.doubleValue() >= x[x.length - 1].doubleValue()) {
            return y[y.length - 1];
        }

        int i = 0;
        while (at.doubleValue() > x[i+1].doubleValue()) {
            i++;
        }

        I x0 = x[i];
        I x1 = x[i+1];
        double y0 = y[i];
        double y1 = y[i+1];

        return y0 + (y1 - y0) * (at.doubleValue() - x0.doubleValue()) / (x1.doubleValue() - x0.doubleValue());
    }
}
