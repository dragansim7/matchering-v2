package com.example.matchering.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.example.matchering.model.AudioData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

enum class PlaybackSource {
    TARGET,
    MASTERED,
    REFERENCE
}

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPositionSec: Float = 0f,
    val durationSec: Float = 0f,
    val activeSource: PlaybackSource = PlaybackSource.TARGET,
    val isLooping: Boolean = true
)

class AudioPlayer {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private var targetAudio: AudioData? = null
    private var masteredAudio: AudioData? = null
    private var referenceAudio: AudioData? = null

    @Volatile
    private var currentSampleOffset: Int = 0

    fun updateTracks(
        target: AudioData?,
        mastered: AudioData?,
        reference: AudioData?
    ) {
        targetAudio = target
        masteredAudio = mastered
        referenceAudio = reference

        val currentTrack = getActiveAudio()
        val duration = currentTrack?.durationSec ?: 0f
        _state.value = _state.value.copy(durationSec = duration)
    }

    fun switchSource(source: PlaybackSource) {
        _state.value = _state.value.copy(activeSource = source)
        val currentTrack = getActiveAudio()
        _state.value = _state.value.copy(durationSec = currentTrack?.durationSec ?: 0f)
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        val audio = getActiveAudio() ?: return
        if (_state.value.isPlaying) return

        initAudioTrack(audio.sampleRate)
        audioTrack?.play()
        _state.value = _state.value.copy(isPlaying = true)

        playbackJob?.cancel()
        playbackJob = scope.launch {
            streamAudioLoop()
        }
    }

    fun pause() {
        _state.value = _state.value.copy(isPlaying = false)
        playbackJob?.cancel()
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (_: Exception) {}
    }

    fun seekToFraction(fraction: Float) {
        val audio = getActiveAudio() ?: return
        val newOffset = (fraction.coerceIn(0f, 1f) * audio.sampleCount).toInt()
        currentSampleOffset = newOffset
        val posSec = if (audio.sampleRate > 0) newOffset.toFloat() / audio.sampleRate else 0f
        _state.value = _state.value.copy(currentPositionSec = posSec)
    }

    fun toggleLoop() {
        _state.value = _state.value.copy(isLooping = !_state.value.isLooping)
    }

    private fun getActiveAudio(): AudioData? {
        return when (_state.value.activeSource) {
            PlaybackSource.TARGET -> targetAudio
            PlaybackSource.MASTERED -> masteredAudio ?: targetAudio
            PlaybackSource.REFERENCE -> referenceAudio
        }
    }

    private fun initAudioTrack(sampleRate: Int) {
        if (audioTrack != null) return

        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = minBufSize.coerceAtLeast(4096 * 4)

        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build(),
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    private suspend fun streamAudioLoop() {
        val bufferChunkSamples = 2048
        val shortBuffer = ShortArray(bufferChunkSamples * 2)

        while (scope.isActive && _state.value.isPlaying) {
            val audio = getActiveAudio()
            if (audio == null || audio.sampleCount == 0) {
                delay(50)
                continue
            }

            val totalSamples = audio.sampleCount
            if (currentSampleOffset >= totalSamples) {
                if (_state.value.isLooping) {
                    currentSampleOffset = 0
                } else {
                    pause()
                    currentSampleOffset = 0
                    break
                }
            }

            val samplesToRead = min(bufferChunkSamples, totalSamples - currentSampleOffset)
            for (i in 0 until samplesToRead) {
                val idx = currentSampleOffset + i
                val l = audio.left[idx].coerceIn(-1.0f, 1.0f)
                val r = audio.right[idx].coerceIn(-1.0f, 1.0f)
                shortBuffer[i * 2] = (l * 32767f).roundToInt().toShort()
                shortBuffer[i * 2 + 1] = (r * 32767f).roundToInt().toShort()
            }

            audioTrack?.write(shortBuffer, 0, samplesToRead * 2)
            currentSampleOffset += samplesToRead

            val posSec = currentSampleOffset.toFloat() / audio.sampleRate
            _state.value = _state.value.copy(currentPositionSec = posSec)
        }
    }

    fun release() {
        pause()
        try {
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
