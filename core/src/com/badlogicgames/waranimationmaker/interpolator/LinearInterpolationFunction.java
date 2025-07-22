package com.badlogicgames.waranimationmaker.interpolator;

public class LinearInterpolationFunction<I extends Number> extends InterpolationFunction<I, Double> {

    public LinearInterpolationFunction(I[] x, Double[] y) {
        super(x, y);
    }

    @Override
    public Double evaluate(I at) {
        I[] x = getI();
        Double[] y = getO();
        if ((Double) at <= (Double) x[0]) {
            return y[0];
        }
        if ((Double) at >= (Double) x[x.length - 1]) {
            return y[y.length - 1];
        }

        int i = 0;
        while ((Double) at > (Double) x[i+1]) {
            i++;
        }

        I x0 = x[i];
        I x1 = x[i+1];
        double y0 = y[i];
        double y1 = y[i+1];

        return y0 + (y1 - y0) * ((Double) at - (Double) x0) / ((Double) x1 - (Double) x0);
    }
}
