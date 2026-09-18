package com.example.matchering.dsp

import com.example.matchering.model.AudioData
import com.example.matchering.model.MatcheringConfig
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Port of the core Matchering 2.0 DSP algorithm to Kotlin.
 *
 * Implements:
 * 1. Mid/Side matrixing
 * 2. Reference peak normalization
 * 3. Loudest pieces segmentation & RMS level matching
 * 4. Log-frequency smoothed spectral matching EQ (FIR synthesis & convolution)
 * 5. Iterative RMS level correction
 * 6. Hyrax lookahead brickwall peak limiter
 */
object MatcheringEngine {

    interface ProgressListener {
        fun onProgress(stage: String, percent: Float)
    }

    fun process(
        target: AudioData,
        reference: AudioData,
        config: MatcheringConfig,
        listener: ProgressListener? = null
    ): AudioData {
        listener?.onProgress("Analyzing target & reference...", 0.05f)

        val sampleRate = target.sampleRate
        val n = target.sampleCount

        // 1. Normalize Reference
        listener?.onProgress("Normalizing reference...", 0.12f)
        val thresholdAmp = 10.0.pow(config.limiterThresholdDb / 20.0).toFloat()
        val refPeak = reference.peak.coerceAtLeast(1e-6f)
        val refNormCoeff = if (refPeak > thresholdAmp) thresholdAmp / refPeak else 1.0f

        val refLeft = FloatArray(reference.sampleCount) { reference.left[it] * refNormCoeff }
        val refRight = FloatArray(reference.sampleCount) { reference.right[it] * refNormCoeff }

        // 2. Mid/Side decomposition
        listener?.onProgress("Mid/Side matrixing...", 0.20f)
        val targetMid = FloatArray(n) { (target.left[it] + target.right[it]) * 0.5f }
        val targetSide = FloatArray(n) { (target.left[it] - target.right[it]) * 0.5f }

        val refMid = FloatArray(reference.sampleCount) { (refLeft[it] + refRight[it]) * 0.5f }
        val refSide = FloatArray(reference.sampleCount) { (refLeft[it] - refRight[it]) * 0.5f }

        // 3. Analyze Levels (Loudest Pieces)
        listener?.onProgress("Extracting loudest pieces & matching levels...", 0.30f)
        val pieceSize = min(sampleRate * 8, max(sampleRate * 2, n / 12)).coerceAtLeast(1024)
        val targetMatchRms = computeLoudestPiecesRms(targetMid, pieceSize)
        val refMatchRms = computeLoudestPiecesRms(refMid, pieceSize)

        val rmsCoeff = if (targetMatchRms > 1e-6f) {
            (refMatchRms / targetMatchRms).coerceIn(0.1f, 10.0f)
        } else {
            1.0f
        }

        // Apply initial RMS gain to Target
        for (i in 0 until n) {
            targetMid[i] *= rmsCoeff
            targetSide[i] *= rmsCoeff
        }

        // 4. Frequency Matching (Matching EQ)
        listener?.onProgress("Calculating matching FIR curves...", 0.45f)
        val fftSize = 2048
        val midFir = calculateMatchingFir(
            targetSamples = targetMid,
            referenceSamples = refMid,
            fftSize = fftSize,
            strength = config.matchingStrength
        )
        val sideFir = calculateMatchingFir(
            targetSamples = targetSide,
            referenceSamples = refSide,
            fftSize = fftSize,
            strength = config.matchingStrength * config.stereoMatchingStrength
        )

        listener?.onProgress("Convolving audio with matching filters...", 0.60f)
        val convolvedMid = convolveFir(targetMid, midFir)
        val convolvedSide = convolveFir(targetSide, sideFir)

        // Convert back to L/R
        val resultLeft = FloatArray(n) { convolvedMid[it] + convolvedSide[it] }
        val resultRight = FloatArray(n) { convolvedMid[it] - convolvedSide[it] }

        // 5. RMS Correction Steps
        listener?.onProgress("Iterative RMS correction...", 0.75f)
        var currentMid = convolvedMid
        for (step in 1..config.rmsCorrectionSteps) {
            val clippedMid = FloatArray(n) { currentMid[it].coerceIn(-1.0f, 1.0f) }
            val currentMatchRms = computeLoudestPiecesRms(clippedMid, pieceSize)
            if (currentMatchRms > 1e-6f) {
                val stepCoeff = (refMatchRms / currentMatchRms).coerceIn(0.5f, 2.0f)
                val blendCoeff = 1.0f + (stepCoeff - 1.0f) * 0.75f
                for (i in 0 until n) {
                    currentMid[i] *= blendCoeff
                    resultLeft[i] *= blendCoeff
                    resultRight[i] *= blendCoeff
                }
            }
        }

        // 6. Hyrax Brickwall Peak Limiter
        if (config.applyLimiter) {
            listener?.onProgress("Applying Hyrax lookahead brickwall limiter...", 0.88f)
            applyHyraxLimiter(resultLeft, resultRight, sampleRate, thresholdAmp)
        } else if (config.normalizeWithoutLimiter) {
            listener?.onProgress("Normalizing output...", 0.88f)
            var maxPeak = 0f
            for (i in 0 until n) {
                val l = abs(resultLeft[i])
                val r = abs(resultRight[i])
                if (l > maxPeak) maxPeak = l
                if (r > maxPeak) maxPeak = r
            }
            if (maxPeak > 1e-6f) {
                val norm = thresholdAmp / maxPeak
                for (i in 0 until n) {
                    resultLeft[i] *= norm
                    resultRight[i] *= norm
                }
            }
        }

        listener?.onProgress("Finalizing audio...", 0.98f)
        return AudioData(
            title = "${target.title} (Matchering Master)",
            sampleRate = sampleRate,
            left = resultLeft,
            right = resultRight
        )
    }

    private fun computeLoudestPiecesRms(samples: FloatArray, pieceSize: Int): Float {
        val count = samples.size
        if (count == 0) return 0f
        val numPieces = max(1, count / pieceSize)
        val rmsList = FloatArray(numPieces)
        var sumRms = 0.0

        for (p in 0 until numPieces) {
            val start = p * pieceSize
            val end = min(count, start + pieceSize)
            var sumSq = 0.0
            for (i in start until end) {
                val s = samples[i].toDouble()
                sumSq += s * s
            }
            val r = sqrt((sumSq / (end - start)).coerceAtLeast(1e-12)).toFloat()
            rmsList[p] = r
            sumRms += r
        }

        val avgRms = (sumRms / numPieces).toFloat()
        var loudestSumSq = 0.0
        var loudestCount = 0

        for (r in rmsList) {
            if (r >= avgRms) {
                loudestSumSq += r.toDouble() * r.toDouble()
                loudestCount++
            }
        }

        return if (loudestCount > 0) {
            sqrt(loudestSumSq / loudestCount).toFloat()
        } else {
            avgRms
        }
    }

    private fun calculateMatchingFir(
        targetSamples: FloatArray,
        referenceSamples: FloatArray,
        fftSize: Int,
        strength: Float
    ): FloatArray {
        val window = FFT.hannWindow(fftSize)
        val half = fftSize / 2 + 1

        val targetMag = computeAverageMagnitude(targetSamples, fftSize, window)
        val refMag = computeAverageMagnitude(referenceSamples, fftSize, window)

        // Ratio of Reference to Target
        val rawCurve = FloatArray(half)
        for (i in 0 until half) {
            val t = targetMag[i].coerceAtLeast(1e-5f)
            val r = refMag[i].coerceAtLeast(1e-5f)
            // Limit extreme EQ boosts or cuts to safe mastering range (-12 dB to +12 dB)
            val ratio = (r / t).coerceIn(0.25f, 4.0f)
            // Blend with 1.0 according to strength
            rawCurve[i] = 1.0f + (ratio - 1.0f) * strength
        }

        // Smooth curve in frequency domain (octave-like smoothing)
        val smoothedCurve = smoothFrequencyCurve(rawCurve)

        // Convert smoothed magnitude to linear-phase FIR filter
        val firReal = FloatArray(fftSize)
        val firImag = FloatArray(fftSize)

        for (i in 0 until half) {
            firReal[i] = smoothedCurve[i]
        }
        // Mirror for negative frequencies
        for (i in 1 until fftSize / 2) {
            firReal[fftSize - i] = smoothedCurve[i]
        }

        // IFFT
        FFT.ifft(firReal, firImag)

        // Shift center to make causal linear-phase filter, and apply Hann window
        val fir = FloatArray(fftSize)
        val halfSize = fftSize / 2
        for (i in 0 until fftSize) {
            val shiftedIdx = (i + halfSize) % fftSize
            fir[i] = firReal[shiftedIdx] * window[i]
        }

        // Normalize FIR sum so DC gain is preserved
        var firSum = 0f
        for (v in fir) firSum += v
        if (abs(firSum) > 1e-6f) {
            val norm = smoothedCurve[0].coerceAtLeast(0.5f) / firSum
            for (i in fir.indices) fir[i] *= norm
        }

        return fir
    }

    private fun computeAverageMagnitude(samples: FloatArray, fftSize: Int, window: FloatArray): FloatArray {
        val half = fftSize / 2 + 1
        val avgMag = FloatArray(half)
        val n = samples.size
        if (n < fftSize) {
            avgMag.fill(1.0f)
            return avgMag
        }

        val step = max(fftSize, n / 16)
        var count = 0
        val buffer = FloatArray(fftSize)

        for (offset in 0 until (n - fftSize) step step) {
            System.arraycopy(samples, offset, buffer, 0, fftSize)
            val mag = FFT.magnitudeSpectrum(buffer, window)
            for (i in 0 until half) {
                avgMag[i] += mag[i]
            }
            count++
            if (count >= 16) break
        }

        if (count > 0) {
            for (i in 0 until half) {
                avgMag[i] /= count
            }
        }
        return avgMag
    }

    /**
     * Exponential log-frequency moving average smoothing.
     */
    private fun smoothFrequencyCurve(curve: FloatArray): FloatArray {
        val n = curve.size
        val result = FloatArray(n)
        for (i in 0 until n) {
            // Adaptive window size: narrow at low frequencies, wide at high frequencies
            val windowRadius = max(1, (i * 0.15f).toInt())
            val start = max(0, i - windowRadius)
            val end = min(n - 1, i + windowRadius)
            var sum = 0f
            var weightSum = 0f
            for (j in start..end) {
                val weight = 1.0f - abs(j - i).toFloat() / (windowRadius + 1)
                sum += curve[j] * weight
                weightSum += weight
            }
            result[i] = sum / weightSum
        }
        return result
    }

    /**
     * Efficient time-domain convolution for linear phase FIR.
     * Truncates FIR to 256 taps for optimal speed with preservation of spectral curve.
     */
    private fun convolveFir(input: FloatArray, fir: FloatArray): FloatArray {
        val output = FloatArray(input.size)
        // Use central 256 taps of the FIR for fast, low-latency processing
        val targetTaps = min(256, fir.size)
        val startFir = (fir.size - targetTaps) / 2
        val taps = FloatArray(targetTaps) { fir[startFir + it] }
        val center = targetTaps / 2

        val n = input.size
        for (i in 0 until n) {
            var acc = 0.0
            val minK = max(0, center - i)
            val maxK = min(targetTaps, n - i + center)
            for (k in minK until maxK) {
                val inputIdx = i + k - center
                acc += input[inputIdx] * taps[k]
            }
            output[i] = acc.toFloat()
        }
        return output
    }

    /**
     * Hyrax Lookahead Brickwall Peak Limiter.
     * Smooths gain reduction across lookahead window with attack, hold, and release stages.
     */
    private fun applyHyraxLimiter(
        left: FloatArray,
        right: FloatArray,
        sampleRate: Int,
        ceiling: Float
    ) {
        val n = left.size
        val lookaheadMs = 2.0f
        val lookaheadSamples = (lookaheadMs * sampleRate / 1000f).toInt().coerceAtLeast(16)
        val releaseMs = 50.0f
        val releaseCoeff = exp(-1.0 / (releaseMs * sampleRate / 1000f)).toFloat()

        // 1. Detect required gain envelope
        val targetGain = FloatArray(n)
        for (i in 0 until n) {
            val peak = max(abs(left[i]), abs(right[i]))
            targetGain[i] = if (peak > ceiling) ceiling / peak else 1.0f
        }

        // 2. Lookahead minimum filter (attack envelope)
        val attackGain = FloatArray(n)
        var minInWindow = 1.0f
        for (i in 0 until n) {
            val lookEnd = min(n, i + lookaheadSamples)
            var currentMin = 1.0f
            for (j in i until lookEnd) {
                if (targetGain[j] < currentMin) currentMin = targetGain[j]
            }
            attackGain[i] = currentMin
        }

        // 3. Smooth release envelope
        var currentGain = 1.0f
        for (i in 0 until n) {
            val target = attackGain[i]
            if (target < currentGain) {
                // Instant lookahead attack
                currentGain = target
            } else {
                // Exponential release recovery
                currentGain = target + (currentGain - target) * releaseCoeff
            }
            left[i] = (left[i] * currentGain).coerceIn(-ceiling, ceiling)
            right[i] = (right[i] * currentGain).coerceIn(-ceiling, ceiling)
        }
    }
}
