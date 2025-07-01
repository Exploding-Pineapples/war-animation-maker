package com.badlogicgames.waranimationmaker.interpolator

import kotlin.math.max
import kotlin.math.min

// this is a java number which isn't rly the saME AS KOTLIN number, uu should converr this class  to kotlin first of all 
class PCHIPInterpolator<I : Number>(x: Array<I>, y: Array<Double>) : Interpolator<I, Double>(x, y) {
    protected val slopes: DoubleArray

    init {
        this.slopes = computeSlopes(x, y)
    }

    // Function to compute the PCHIP slopes
    fun computeSlopes(x: Array<I>, y: Array<Double>): DoubleArray { // ChatGPT wrote this
        val n = x.size

        if (n < 2) {
            return doubleArrayOf()
        }

        val h = DoubleArray(n - 1)
        val delta = DoubleArray(n - 1)
        val slopes = DoubleArray(n)

        // Calculate h and delta
        for (i in 0 until n - 1) {
            h[i] = x[i + 1] as Double - x[i] as Double
            delta[i] = (y[i + 1] - y[i]) / h[i]
        }

        // End points: 0 slope
        slopes[0] = 0.0
        slopes[n - 1] = 0.0

        // Internal points
        for (i in 1 until n - 1) {
            if (delta[i - 1] * delta[i] > 0) {
                val w1 = 2 * h[i] + h[i - 1]
                val w2 = h[i] + 2 * h[i - 1]
                slopes[i] = (w1 + w2) / (w1 / delta[i - 1] + w2 / delta[i])
            } else {
                slopes[i] = 0.0
            }
        }

        return slopes
    }

    // Method to perform PCHIP interpolation at a given xi
    override fun interpolateAt(xi: I): Double { // ChatGPT wrote this
        val n = x.size

        if (n < 2) {
            return y[0]!!
        }

        if ((xi as Double) < (x[0] as Double)) {
            return y[0]!!
        }
        if (xi as Double > x[n - 1] as Double) {
            return y[n - 1]!!
        }

        // Find the interval [x_k, x_{k+1}] where xi lies
        var k = x.binarySearch(xi)
        if (k < 0) {
            k = -k - 2
        }
        k = max(0.0, min(k.toDouble(), (n - 2).toDouble())).toInt()

        // Calculate the cubic polynomial coefficients
        val h = x[k + 1] as Double - x[k] as Double
        val t = (xi as Double - x[k] as Double) / h
        val t2 = t * t
        val t3 = t2 * t

        val h00 = 2 * t3 - 3 * t2 + 1
        val h10 = t3 - 2 * t2 + t
        val h01 = -2 * t3 + 3 * t2
        val h11 = t3 - t2

        // Interpolated value
        return h00 * y[k]!! + h10 * h * slopes[k] + h01 * y[k + 1]!! + h11 * h * slopes[k + 1]
    }
}
