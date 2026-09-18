package com.example.matchering.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Fast Fourier Transform (FFT) and Inverse Fast Fourier Transform (IFFT)
 * using Cooley-Tukey Radix-2 algorithm.
 */
object FFT {

    /**
     * In-place Radix-2 FFT.
     * @param real Real parts, length must be power of 2.
     * @param imag Imaginary parts, length must be power of 2.
     */
    fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        require(n == imag.size) { "Real and imag arrays must have same size" }
        require((n and (n - 1)) == 0) { "Size must be power of 2" }

        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR

                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        // Cooley-Tukey decimation in time
        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val angle = -2.0 * PI / len
            val wStepR = cos(angle).toFloat()
            val wStepI = sin(angle).toFloat()

            var i = 0
            while (i < n) {
                var wR = 1.0f
                var wI = 0.0f
                for (k in 0 until halfLen) {
                    val posA = i + k
                    val posB = i + k + halfLen

                    val uR = real[posA]
                    val uI = imag[posA]

                    val vR = real[posB] * wR - imag[posB] * wI
                    val vI = real[posB] * wI + imag[posB] * wR

                    real[posA] = uR + vR
                    imag[posA] = uI + vI
                    real[posB] = uR - vR
                    imag[posB] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += len
            }
            len = len shl 1
        }
    }

    /**
     * In-place Radix-2 Inverse FFT.
     */
    fun ifft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        // Conjugate imaginary
        for (i in 0 until n) {
            imag[i] = -imag[i]
        }
        fft(real, imag)
        val scale = 1.0f / n
        for (i in 0 until n) {
            real[i] = real[i] * scale
            imag[i] = -imag[i] * scale
        }
    }

    /**
     * Computes magnitude spectrum for power-of-2 input window.
     */
    fun magnitudeSpectrum(samples: FloatArray, window: FloatArray? = null): FloatArray {
        val n = samples.size
        val real = FloatArray(n)
        val imag = FloatArray(n)

        for (i in 0 until n) {
            real[i] = if (window != null) samples[i] * window[i] else samples[i]
        }
        fft(real, imag)

        val half = n / 2 + 1
        val mag = FloatArray(half)
        for (i in 0 until half) {
            mag[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }
        return mag
    }

    /**
     * Creates a standard Hann window.
     */
    fun hannWindow(size: Int): FloatArray {
        val window = FloatArray(size)
        val inv = 2.0 * PI / (size - 1)
        for (i in 0 until size) {
            window[i] = (0.5 * (1.0 - cos(i * inv))).toFloat()
        }
        return window
    }
}
