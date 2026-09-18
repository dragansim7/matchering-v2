package com.example.matchering.model

import com.example.matchering.dsp.FFT
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * In-memory representation of stereo 32-bit float audio buffer with metadata and metrics.
 */
data class AudioData(
    val title: String,
    val sampleRate: Int,
    val left: FloatArray,
    val right: FloatArray
) {
    val sampleCount: Int = left.size
    val durationSec: Float = if (sampleRate > 0) sampleCount.toFloat() / sampleRate else 0f

    val peak: Float by lazy { calculatePeak() }
    val peakDbfs: Float by lazy { toDbfs(peak) }
    val rms: Float by lazy { calculateRms() }
    val rmsDbfs: Float by lazy { toDbfs(rms) }
    val stereoCorrelation: Float by lazy { calculateStereoCorrelation() }

    // Waveform visualization points (e.g. 120 points for smooth canvas rendering)
    val waveformPoints: FloatArray by lazy { generateWaveformPoints(120) }

    // Average magnitude spectrum for frequency display (64 frequency bands)
    val spectrumBands: FloatArray by lazy { calculateSpectrumBands(64) }

    private fun calculatePeak(): Float {
        var maxVal = 0f
        for (i in 0 until sampleCount) {
            val l = kotlin.math.abs(left[i])
            val r = kotlin.math.abs(right[i])
            if (l > maxVal) maxVal = l
            if (r > maxVal) maxVal = r
        }
        return maxVal
    }

    private fun calculateRms(): Float {
        if (sampleCount == 0) return 0f
        var sumSquares = 0.0
        for (i in 0 until sampleCount) {
            val l = left[i].toDouble()
            val r = right[i].toDouble()
            sumSquares += l * l + r * r
        }
        return sqrt((sumSquares / (sampleCount * 2)).coerceAtLeast(1e-12)).toFloat()
    }

    private fun calculateStereoCorrelation(): Float {
        if (sampleCount == 0) return 1f
        var sumLR = 0.0
        var sumL2 = 0.0
        var sumR2 = 0.0
        for (i in 0 until sampleCount) {
            val l = left[i].toDouble()
            val r = right[i].toDouble()
            sumLR += l * r
            sumL2 += l * l
            sumR2 += r * r
        }
        val denom = sqrt(sumL2 * sumR2)
        return if (denom > 1e-9) {
            (sumLR / denom).toFloat().coerceIn(-1f, 1f)
        } else {
            1f
        }
    }

    private fun generateWaveformPoints(targetPoints: Int): FloatArray {
        if (sampleCount == 0) return FloatArray(targetPoints)
        val points = FloatArray(targetPoints)
        val chunkSize = max(1, sampleCount / targetPoints)

        for (p in 0 until targetPoints) {
            val start = p * chunkSize
            val end = min(sampleCount, start + chunkSize)
            var maxAmp = 0f
            for (i in start until end) {
                val amp = (kotlin.math.abs(left[i]) + kotlin.math.abs(right[i])) * 0.5f
                if (amp > maxAmp) maxAmp = amp
            }
            points[p] = maxAmp.coerceIn(0.02f, 1.0f)
        }
        return points
    }

    private fun calculateSpectrumBands(bandCount: Int): FloatArray {
        val bands = FloatArray(bandCount)
        if (sampleCount < 1024) return bands

        val fftSize = 2048
        val window = FFT.hannWindow(fftSize)
        val step = max(fftSize, sampleCount / 8)
        var count = 0

        val temp = FloatArray(fftSize)
        for (offset in 0 until (sampleCount - fftSize) step step) {
            for (i in 0 until fftSize) {
                temp[i] = (left[offset + i] + right[offset + i]) * 0.5f
            }
            val mag = FFT.magnitudeSpectrum(temp, window)

            // Map linear FFT bins (0..1024) into log-spaced bands (0..bandCount-1)
            val halfBins = mag.size
            for (b in 0 until bandCount) {
                val fractionLow = (b.toDouble() / bandCount).pow(2.5)
                val fractionHigh = ((b + 1).toDouble() / bandCount).pow(2.5)
                val binStart = (fractionLow * (halfBins - 1)).toInt().coerceIn(1, halfBins - 1)
                val binEnd = (fractionHigh * (halfBins - 1)).toInt().coerceIn(binStart + 1, halfBins)

                var sum = 0f
                for (k in binStart until binEnd) {
                    sum += mag[k]
                }
                val avg = sum / (binEnd - binStart)
                bands[b] += avg
            }
            count++
            if (count >= 12) break
        }

        if (count > 0) {
            for (b in 0 until bandCount) {
                bands[b] = bands[b] / count
            }
        }
        return bands
    }

    companion object {
        fun toDbfs(amplitude: Float): Float {
            if (amplitude <= 1e-6f) return -96f
            return (20.0 * log10(amplitude.toDouble())).toFloat().coerceIn(-96f, 6f)
        }
    }
}

/**
 * Configuration options for Matchering matching and mastering process.
 */
data class MatcheringConfig(
    val matchingStrength: Float = 1.0f, // 0.0 to 1.0 (wet/dry EQ)
    val stereoMatchingStrength: Float = 0.85f, // Side channel matching intensity
    val limiterThresholdDb: Float = -0.1f, // Limiter ceiling
    val rmsCorrectionSteps: Int = 3, // Iterative loudness convergence steps
    val applyLimiter: Boolean = true, // Whether to apply Hyrax lookahead limiter
    val normalizeWithoutLimiter: Boolean = false
)
