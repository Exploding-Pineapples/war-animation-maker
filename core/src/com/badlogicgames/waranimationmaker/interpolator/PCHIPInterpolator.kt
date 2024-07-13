package com.badlogicgames.waranimationmaker.interpolator

import java.util.*
import kotlin.math.max
import kotlin.math.min


class PCHIPInterpolator {
    class IntToFloatInterpolator(var x: List<Int>, var y: List<Float>) {
        public var slopes: Array<Float>

        init {
            require(x.size == y.size) { "The lengths of x and y must be the same." }
            this.slopes = computeSlopes()
        }

        fun update(x: List<Int>, y: List<Float>) {
            require(x.size == y.size) { "The lengths of x and y must be the same." }
            this.x = x
            this.y = y
        }

        // Method to perform PCHIP interpolation at a given xi
        fun interpolateAt(xi: Int): Float {
            this.slopes = computeSlopes()

            if (x.size < 2) { // If there is only 1 element, there is no interpolation
                return y[0]
            }

            val n = x.size

            // Find the interval [x_k, x_{k+1}] where xi lies
            var k: Int = Arrays.binarySearch(x.toIntArray(), xi)
            if (k < 0) {
                k = -k - 2
            }
            k = max(0.0, min(k.toDouble(), (n - 2).toDouble())).toInt()

            // Calculate the cubic polynomial coefficients, everything is done in double to avoid generic types
            val h = x[k + 1] - x[k]
            val t = xi - x[k] / h
            val t2 = t * t
            val t3 = t2 * t

            val h00 = 2 * t3 - 3 * t + 1
            val h10 = t3 - 2 * t2 + t
            val h01 = -2 * t3 + 3 * t2
            val h11 = t3 - t2

            // Interpolated value
            val result = h00 * y[k] + h10 * h * slopes[k] + h01 * y[k + 1] + h11 * h * slopes[k + 1]
            println(result)
            return (result)
        }

        private fun computeSlopes() : Array<Float> {
            val n = x.size

            if (n < 2) {
                return arrayOf(0f, 0f)
            }

            val h = DoubleArray(n - 1)
            val delta = DoubleArray(n - 1)
            val slopes = Array(n){0.0f}

            // Calculate h and delta
            for (i in 0 until n - 1) {
                h[i] = x[i + 1].toDouble() - x[i]
                delta[i] = (y[i + 1] - y[i]) / h[i]
            }

            // End points: using one-sided differences
            slopes[0] = delta[0].toFloat()
            slopes[n - 1] = delta[n - 2].toFloat()

            // Internal points
            for (i in 1 until n - 1) {
                if (delta[i - 1] * delta[i] > 0) {
                    val w1 = 2 * h[i] + h[i - 1]
                    val w2 = h[i] + 2 * h[i - 1]
                    slopes[i] = ((w1 + w2) / (w1 / delta[i - 1] + w2 / delta[i])).toFloat()
                } else {
                    slopes[i] = 0.0f
                }
            }

            return slopes
        }
    }

    class FloatToFloatInterpolator(var x: List<Float>, var y: List<Float>) {
        public var slopes: Array<Float>

        init {
            require(x.size == y.size) { "The lengths of x and y must be the same." }
            this.slopes = computeSlopes()
        }

        fun update(x: List<Float>, y: List<Float>) {
            require(x.size == y.size) { "The lengths of x and y must be the same." }
            this.x = x
            this.y = y
            this.slopes = computeSlopes()
        }

        // Method to perform PCHIP interpolation at a given xi
        fun interpolateAt(xi: Float): Float {
            if (x.size < 2) { // If there is only 1 element, there is no interpolation
                return y[0]
            }

            val n = x.size

            // Find the interval [x_k, x_{k+1}] where xi lies
            var k: Int = Arrays.binarySearch(x.toFloatArray(), xi)
            if (k < 0) {
                k = -k - 2
            }
            k = max(0.0, min(k.toDouble(), (n - 2).toDouble())).toInt()

            // Calculate the cubic polynomial coefficients, everything is done in double to avoid generic types
            val h = x[k + 1] - x[k]
            val t = xi - x[k] / h
            val t2 = t * t
            val t3 = t2 * t

            val h00 = 2 * t3 - 3 * t + 1
            val h10 = t3 - 2 * t2 + t
            val h01 = -2 * t3 + 3 * t2
            val h11 = t3 - t2

            // Interpolated value
            val result = h00 * y[k] + h10 * h * slopes[k] + h01 * y[k + 1] + h11 * h * slopes[k + 1]
            println(result)
            return (result)
        }

        private fun computeSlopes() : Array<Float> {
            val n = x.size

            if (n < 2) {
                return arrayOf(0f, 0f)
            }

            val h = DoubleArray(n - 1)
            val delta = DoubleArray(n - 1)
            val slopes = Array(n){0.0f}

            // Calculate h and delta
            for (i in 0 until n - 1) {
                h[i] = x[i + 1].toDouble() - x[i]
                delta[i] = (y[i + 1] - y[i]) / h[i]
            }

            // End points: using one-sided differences
            slopes[0] = delta[0].toFloat()
            slopes[n - 1] = delta[n - 2].toFloat()

            // Internal points
            for (i in 1 until n - 1) {
                if (delta[i - 1] * delta[i] > 0) {
                    val w1 = 2 * h[i] + h[i - 1]
                    val w2 = h[i] + 2 * h[i - 1]
                    slopes[i] = ((w1 + w2) / (w1 / delta[i - 1] + w2 / delta[i])).toFloat()
                } else {
                    slopes[i] = 0.0f
                }
            }

            return slopes
        }
    }
}