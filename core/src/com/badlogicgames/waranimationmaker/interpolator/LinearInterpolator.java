package com.badlogicgames.waranimationmaker.interpolator;

public class LinearInterpolator<I extends Number> extends Interpolator<I, Double> {

    public LinearInterpolator(I[] x, Double[] y) {
        super(x, y);
    }

    @Override
    public Double interpolateAt(I xi) {
        I[] x = getX();
        Double[] y = getY();
        if ((Double) xi <= (Double) x[0]) {
            return y[0];
        }
        if ((Double) xi >= (Double) x[x.length - 1]) {
            return y[y.length - 1];
        }

        int i = 0;
        while ((Double) xi > (Double) x[i+1]) {
            i++;
        }

        I x0 = x[i];
        I x1 = x[i+1];
        double y0 = y[i];
        double y1 = y[i+1];

        return y0 + (y1 - y0) * ((Double) xi - (Double) x0) / ((Double) x1 - (Double) x0);
    }
}
