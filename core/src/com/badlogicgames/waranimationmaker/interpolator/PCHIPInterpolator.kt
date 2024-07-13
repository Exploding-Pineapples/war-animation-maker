package com.badlogicgames.waranimationmaker.interpolator;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

public class PCHIPInterpolator {

    public static class Interpolator {
        private final double[] x;
        private final double[] y;
        private final double[] slopes;

        public Interpolator(double[] x, double[] y) {
            if (x.length != y.length) {
                throw new IllegalArgumentException("The lengths of x and y must be the same.");
            }
            this.x = x;
            this.y = y;
            this.slopes = computeSlopes(x, y);
        }

        // Function to compute the PCHIP slopes
        private static double[] computeSlopes(double[] x, double[] y) {
            int n = x.length;
            double[] h = new double[n - 1];
            double[] delta = new double[n - 1];
            double[] slopes = new double[n];

            // Calculate h and delta
            for (int i = 0; i < n - 1; i++) {
                h[i] = x[i + 1] - x[i];
                delta[i] = (y[i + 1] - y[i]) / h[i];
            }

            // End points: using one-sided differences
            slopes[0] = delta[0];
            slopes[n - 1] = delta[n - 2];

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

        // Method to create an interpolation function
        public DoubleUnaryOperator interpolate() {
            return this::interpolateAt;
        }

        // Method to perform PCHIP interpolation at a given xi
        public double interpolateAt(double xi) {
            int n = x.length;

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

    public static void main(String[] args) {
        double[] x = {0, 1, 2, 3, 4};
        double[] y = {0, 1, 0, 1, 0};

        Interpolator interpolator = new Interpolator(x, y);
        DoubleUnaryOperator interpolationFunction = interpolator.interpolate();

        double xi = 2.5;
        double yi = interpolationFunction.applyAsDouble(xi);

        System.out.println("Interpolated value at x = " + xi + " is y = " + yi);
    }
}
