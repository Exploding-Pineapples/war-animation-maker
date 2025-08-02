package com.badlogicgames.waranimationmaker.interpolator

// this is a java number which isn't rly the saME AS KOTLIN number, uu should converr this class  to kotlin first of all 
class PCHIPInterpolationFunction<I : Number>(i: Array<I>, o: DoubleArray) : InterpolationFunction<I, Double>(i, o.toTypedArray()) {
    protected var slopes: DoubleArray = computeSlopes(i, o)
    private var iDoubles: DoubleArray = i.map { it.toDouble() }.toDoubleArray()

    override fun init() {
        this.slopes = computeSlopes(i, o.toDoubleArray())
        iDoubles = i.map { it.toDouble() }.toDoubleArray()
    }

    // Function to compute the PCHIP slopes
    fun computeSlopes(x: Array<I>, y: DoubleArray): DoubleArray { // ChatGPT wrote this
        val n = x.size

        if (n < 2) {
            return doubleArrayOf()
        }

        val h = DoubleArray(n - 1)
        val delta = DoubleArray(n - 1)
        val slopes = DoubleArray(n)

        // Calculate h and delta
        for (i in 0 until n - 1) {
            h[i] = x[i + 1].toDouble() - x[i].toDouble()
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
    override fun evaluate(at: I): Double { // ChatGPT wrote this
        val n = i.size
        val i = iDoubles
        val at1 = at.toDouble()

        if (n < 2) {
            return o[0]
        }

        if ((at1) < (i[0])) {
            return o[0]
        }
        if (at1 > i[n - 1]) {
            return o[n - 1]
        }

        // Find the interval [x_k, x_{k+1}] where xi lies
        var k = i.binarySearch(at1)
        if (k < 0) {
            k = -k - 2
        }
        k = when {
            k < 0 -> -k - 2
            k >= n - 1 -> n - 2
            else -> k
        }

        // Calculate the cubic polynomial coefficients
        val h = i[k + 1] - i[k]
        val t = (at1 - i[k]) / h
        val t2 = t * t
        val t3 = t2 * t

        val h00 = 2 * t3 - 3 * t2 + 1
        val h10 = t3 - 2 * t2 + t
        val h01 = -2 * t3 + 3 * t2
        val h11 = t3 - t2

        // Interpolated value
        return h00 * o[k] + h10 * h * slopes[k] + h01 * o[k + 1] + h11 * h * slopes[k + 1]
    }
}
