# Matchering 2.0 for Android

> Matching + Mastering = ❤️

**Matchering 2.0** is an open-source audio matching and mastering application rewritten for Android with Kotlin and Jetpack Compose.

It follows a simple yet revolutionary idea: you feed **TWO** audio tracks into Matchering:
- **TARGET**: The track you want to master (e.g. unmastered mix).
- **REFERENCE**: A commercial reference track whose tonal balance, RMS loudness, and dynamic punch you want your Target to match.

Matchering matches both tracks, delivering a mastered Target track with the same RMS loudness, frequency response (FR), stereo width, and peak headroom as the Reference track.

---

## Features

- **Mid/Side Matrixing**: Independent frequency and dynamic processing for the center and stereo sides.
- **Loudest Pieces Extraction**: Automatically identifies and aligns the highest-energy sections (drops, choruses).
- **Spectral Matching (FIR EQ)**: Computes log-frequency smoothed matching curves and generates linear-phase FIR filters.
- **Iterative RMS Convergence**: Multi-step level corrections to ensure perceptual loudness matching.
- **Hyrax Lookahead Brickwall Limiter**: Transparent peak control without digital clipping.
- **A/B/C Comparison Monitor**: Instant, position-synchronized switching between Original Target (A), Matchering Master (B), and Commercial Reference (C).
- **Interactive Audio Visualizers**: Real-time waveform scrubber and comparative frequency spectrum display.
- **Mastering Report**: Comprehensive analysis of RMS (dBFS), True Peak, stereo correlation, and loudness gain.
- **Export & Share**: Save and share mastered tracks as 16-bit PCM, 24-bit studio, or 32-bit float WAV files.
- **Built-in Sample Tracks**: Test instantly with authentic unmastered and reference audio presets with 0 setup.

---

## Tech Stack

- **Target OS**: Android (minSdk 26, targetSdk 36)
- **Language**: Kotlin 2.2+
- **UI Framework**: Jetpack Compose with Material Design 3
- **Audio Engine**: Pure Kotlin DSP (Radix-2 Cooley-Tukey FFT, FIR Convolution, Hyrax Peak Limiter)
- **Audio Playback**: Low-latency `AudioTrack` PCM streaming
- **Audio Decoding/Encoding**: `MediaCodec`, `MediaExtractor`, and Custom WAV Encoder

---

## Original Authors & Credits

- Original Python/C implementation by **Sergree** ([@sergree](https://github.com/sergree)).
- Licensed under the GNU General Public License v3.0.
