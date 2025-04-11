package com.badlogicgames.waranimationmaker.interpolator;

import java.util.Arrays;

public class PCHIPInterpolator extends Interpolator {
    protected final double[] slopes;

    public PCHIPInterpolator(double[] x, double[] y) {
        super(x, y);
        this.slopes = computeSlopes(x, y);
    }

    // Function to compute the PCHIP slopes
    public double[] computeSlopes(double[] x, double[] y) {
        int n = x.length;

        if (n < 2) {
            return new double[]{};
        }

        double[] h = new double[n - 1];
        double[] delta = new double[n - 1];
        double[] slopes = new double[n];

        // Calculate h and delta
        for (int i = 0; i < n - 1; i++) {
            h[i] = x[i + 1] - x[i];
            delta[i] = (y[i + 1] - y[i]) / h[i];
        }

        // End points: 0 slope
        slopes[0] = 0;
        slopes[n - 1] = 0;

        // Internal points
        for (int i = 1; i < n - 1; i++) {
            if (delta[i - 1] * delta[i] > 0) {
                double w1 = 2 * h[i] + h[i - 1];
                double w2 = h[i] + 2 * h[i - 1];
                slopes[i] = (w1 + w2) / (w1 / delta[i - 1] + w2 / delta[i]);
            } else {
                slopes[i] = 0;
            }
        }

        return slopes;
    }

    // Method to perform PCHIP interpolation at a given xi
    public double interpolateAt(double xi) {
        int n = x.length;

        if (n < 2) {
            return y[0];
        }

        if (xi < x[0]) {
            return y[0];
        }
        if (xi > x[n - 1]) {
            return y[n - 1];
        }

        // Find the interval [x_k, x_{k+1}] where xi lies
        int k = Arrays.binarySearch(x, xi);
        if (k < 0) {
            k = -k - 2;
        }
        k = Math.max(0, Math.min(k, n - 2));

        // Calculate the cubic polynomial coefficients
        double h = x[k + 1] - x[k];
        double t = (xi - x[k]) / h;
        double t2 = t * t;
        double t3 = t2 * t;

        double h00 = 2 * t3 - 3 * t2 + 1;
        double h10 = t3 - 2 * t2 + t;
        double h01 = -2 * t3 + 3 * t2;
        double h11 = t3 - t2;

        // Interpolated value
        return h00 * y[k] + h10 * h * slopes[k] + h01 * y[k + 1] + h11 * h * slopes[k + 1];
    }
}
