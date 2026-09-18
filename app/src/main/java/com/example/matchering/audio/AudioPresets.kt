package com.example.matchering.audio

import com.example.matchering.model.AudioData
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

object AudioPresets {

    fun generateTargetRawMix(): AudioData {
        val sampleRate = 44100
        val durationSec = 7.0f
        val numSamples = (durationSec * sampleRate).toInt()
        val left = FloatArray(numSamples)
        val right = FloatArray(numSamples)

        val bpm = 120.0
        val beatSamples = (sampleRate * 60.0 / bpm).toInt()
        val barSamples = beatSamples * 4

        val random = Random(42)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val barPos = (i % barSamples).toDouble() / barSamples
            val beatPos = (i % beatSamples).toDouble() / beatSamples

            // 1. Kick drum on beat 0 and beat 2
            val beatIndex = (i / beatSamples) % 4
            var kick = 0.0
            if (beatIndex == 0 || beatIndex == 2) {
                val kickT = beatPos * (60.0 / bpm)
                if (kickT < 0.25) {
                    val kickFreq = 120.0 * exp(-kickT * 18.0) + 45.0
                    kick = sin(2.0 * PI * kickFreq * kickT) * exp(-kickT * 12.0) * 0.45
                }
            }

            // 2. Snare drum on beat 1 and beat 3
            var snare = 0.0
            if (beatIndex == 1 || beatIndex == 3) {
                val snareT = beatPos * (60.0 / bpm)
                if (snareT < 0.22) {
                    val tone = sin(2.0 * PI * 185.0 * snareT) * exp(-snareT * 20.0) * 0.25
                    val noise = (random.nextDouble() * 2.0 - 1.0) * exp(-snareT * 15.0) * 0.2
                    snare = tone + noise
                }
            }

            // 3. Hi-hat on every 1/8 note
            val eighthPos = ((i * 2) % beatSamples).toDouble() / beatSamples
            val hatT = eighthPos * (30.0 / bpm)
            var hat = 0.0
            if (hatT < 0.06) {
                hat = (random.nextDouble() * 2.0 - 1.0) * exp(-hatT * 60.0) * 0.08
            }

            // 4. Bassline (80 Hz with minor pentatonic notes)
            val bassPitch = when ((i / (beatSamples / 2)) % 8) {
                0, 1 -> 55.0 // A1
                2, 3 -> 65.4 // C2
                4, 5 -> 73.4 // D2
                else -> 49.0 // G1
            }
            val bass = sin(2.0 * PI * bassPitch * t) * 0.22 +
                    sin(2.0 * PI * bassPitch * 2.0 * t) * 0.08

            // 5. Synth pad / chord progression (unmastered: subdued high frequencies)
            val chordRoot = when ((i / barSamples) % 2) {
                0 -> 220.0 // A3
                else -> 261.63 // C4
            }
            val synthL = (sin(2.0 * PI * chordRoot * t) +
                    sin(2.0 * PI * chordRoot * 1.25 * t) +
                    sin(2.0 * PI * chordRoot * 1.5 * t)) * 0.07
            val synthR = (sin(2.0 * PI * chordRoot * 1.003 * t) +
                    sin(2.0 * PI * chordRoot * 1.252 * t) +
                    sin(2.0 * PI * chordRoot * 1.503 * t)) * 0.07

            // Raw mix: unmastered, quiet headroom (around -6 dBFS peak), narrower stereo
            val monoSum = kick + snare + bass
            left[i] = (monoSum + synthL * 0.85 + hat * 0.6).toFloat() * 0.55f
            right[i] = (monoSum + synthR * 0.85 + hat * 0.6).toFloat() * 0.55f
        }

        return AudioData(
            title = "Target: Raw Unmastered Demo Mix",
            sampleRate = sampleRate,
            left = left,
            right = right
        )
    }

    fun generateReferenceMaster(): AudioData {
        val sampleRate = 44100
        val durationSec = 7.0f
        val numSamples = (durationSec * sampleRate).toInt()
        val left = FloatArray(numSamples)
        val right = FloatArray(numSamples)

        val bpm = 120.0
        val beatSamples = (sampleRate * 60.0 / bpm).toInt()
        val barSamples = beatSamples * 4
        val random = Random(99)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val barPos = (i % barSamples).toDouble() / barSamples
            val beatPos = (i % beatSamples).toDouble() / beatSamples

            // 1. Deep, tight mastered punchy kick with sub-bass weight
            val beatIndex = (i / beatSamples) % 4
            var kick = 0.0
            if (beatIndex == 0 || beatIndex == 2) {
                val kickT = beatPos * (60.0 / bpm)
                if (kickT < 0.28) {
                    val kickFreq = 140.0 * exp(-kickT * 22.0) + 50.0
                    val sub = sin(2.0 * PI * 42.0 * kickT) * exp(-kickT * 6.0) * 0.35
                    kick = (sin(2.0 * PI * kickFreq * kickT) * exp(-kickT * 14.0) * 0.55) + sub
                }
            }

            // 2. Snare with crisp bright snap and stereo room reverb
            var snareL = 0.0
            var snareR = 0.0
            if (beatIndex == 1 || beatIndex == 3) {
                val snareT = beatPos * (60.0 / bpm)
                if (snareT < 0.26) {
                    val tone = sin(2.0 * PI * 200.0 * snareT) * exp(-snareT * 18.0) * 0.35
                    val noiseL = (random.nextDouble() * 2.0 - 1.0) * exp(-snareT * 12.0) * 0.25
                    val noiseR = (random.nextDouble() * 2.0 - 1.0) * exp(-snareT * 12.0) * 0.25
                    snareL = tone + noiseL
                    snareR = tone + noiseR
                }
            }

            // 3. Sizzling bright wide stereo hi-hats
            val eighthPos = ((i * 2) % beatSamples).toDouble() / beatSamples
            val hatT = eighthPos * (30.0 / bpm)
            var hatL = 0.0
            var hatR = 0.0
            if (hatT < 0.08) {
                val nL = (random.nextDouble() * 2.0 - 1.0) * exp(-hatT * 45.0) * 0.15
                val nR = (random.nextDouble() * 2.0 - 1.0) * exp(-hatT * 45.0) * 0.15
                hatL = nL * 1.2
                hatR = nR * 0.8
            }

            // 4. Punchy saturated modern sub-bass
            val bassPitch = when ((i / (beatSamples / 2)) % 8) {
                0, 1 -> 55.0
                2, 3 -> 65.4
                4, 5 -> 73.4
                else -> 49.0
            }
            val bass = sin(2.0 * PI * bassPitch * t) * 0.38 +
                    sin(2.0 * PI * bassPitch * 2.0 * t) * 0.20 +
                    sin(2.0 * PI * bassPitch * 3.0 * t) * 0.08

            // 5. Wide, shimmering stereo synth chords with air and sheen (12 kHz+)
            val chordRoot = when ((i / barSamples) % 2) {
                0 -> 220.0
                else -> 261.63
            }
            val synthToneL = (sin(2.0 * PI * chordRoot * t) +
                    sin(2.0 * PI * chordRoot * 1.5 * t) +
                    sin(2.0 * PI * chordRoot * 2.0 * t)) * 0.16
            val synthToneR = (sin(2.0 * PI * chordRoot * 1.008 * t) +
                    sin(2.0 * PI * chordRoot * 1.508 * t) +
                    sin(2.0 * PI * chordRoot * 2.008 * t)) * 0.16

            val airShimmer = (random.nextDouble() * 2.0 - 1.0) * 0.015

            // Commercial Master: loud, punchy, bright, stereo wide, high energy (-0.1 dBFS ceiling)
            val l = (kick + snareL + bass + synthToneL * 1.2 + hatL + airShimmer) * 0.95
            val r = (kick + snareR + bass + synthToneR * 1.2 + hatR - airShimmer) * 0.95

            // Soft saturator / mastering ceiling
            left[i] = kotlin.math.tanh(l).toFloat() * 0.98f
            right[i] = kotlin.math.tanh(r).toFloat() * 0.98f
        }

        return AudioData(
            title = "Reference: Commercial Master Track",
            sampleRate = sampleRate,
            left = left,
            right = right
        )
    }
}
