package com.example.matchering.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchering.audio.AudioDecoder
import com.example.matchering.audio.AudioEncoder
import com.example.matchering.audio.AudioPlayer
import com.example.matchering.audio.AudioPresets
import com.example.matchering.audio.PlaybackSource
import com.example.matchering.audio.PlayerState
import com.example.matchering.audio.WavBitDepth
import com.example.matchering.dsp.MatcheringEngine
import com.example.matchering.model.AudioData
import com.example.matchering.model.MatcheringConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class MasteringReport(
    val targetRmsDbfs: Float,
    val targetPeakDbfs: Float,
    val referenceRmsDbfs: Float,
    val referencePeakDbfs: Float,
    val masteredRmsDbfs: Float,
    val masteredPeakDbfs: Float,
    val loudnessGainDb: Float,
    val targetCorrelation: Float,
    val masteredCorrelation: Float,
    val processingTimeMs: Long
)

data class MatcheringUiState(
    val targetAudio: AudioData? = null,
    val referenceAudio: AudioData? = null,
    val masteredAudio: AudioData? = null,
    val config: MatcheringConfig = MatcheringConfig(),
    val isProcessing: Boolean = false,
    val processingStage: String = "",
    val processingProgress: Float = 0f,
    val errorMessage: String? = null,
    val lastReport: MasteringReport? = null,
    val showReportDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val showConfigDialog: Boolean = false,
    val showInfoDialog: Boolean = false
)

class MatcheringViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MatcheringUiState())
    val uiState: StateFlow<MatcheringUiState> = _uiState.asStateFlow()

    val audioPlayer = AudioPlayer()
    val playerState: StateFlow<PlayerState> = audioPlayer.state

    init {
        // Automatically load demo tracks on first launch so user has instant interactive experience
        loadSampleTracks()
    }

    fun loadSampleTracks() {
        viewModelScope.launch(Dispatchers.Default) {
            val target = AudioPresets.generateTargetRawMix()
            val reference = AudioPresets.generateReferenceMaster()
            _uiState.value = _uiState.value.copy(
                targetAudio = target,
                referenceAudio = reference,
                masteredAudio = null,
                lastReport = null
            )
            audioPlayer.updateTracks(target, null, reference)
        }
    }

    fun loadAudioFromUri(context: Context, uri: Uri, isTarget: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: if (isTarget) "Target Track" else "Reference Track"
                val audio = AudioDecoder.decodeFromUri(context, uri, fileName)
                withContext(Dispatchers.Main) {
                    if (isTarget) {
                        _uiState.value = _uiState.value.copy(targetAudio = audio, masteredAudio = null, lastReport = null)
                        audioPlayer.updateTracks(audio, null, _uiState.value.referenceAudio)
                    } else {
                        _uiState.value = _uiState.value.copy(referenceAudio = audio, masteredAudio = null, lastReport = null)
                        audioPlayer.updateTracks(_uiState.value.targetAudio, null, audio)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(errorMessage = "Could not load audio: ${e.message}")
                }
            }
        }
    }

    fun startMastering() {
        val target = _uiState.value.targetAudio ?: return
        val reference = _uiState.value.referenceAudio ?: return
        if (_uiState.value.isProcessing) return

        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            processingProgress = 0.02f,
            processingStage = "Starting Matchering 2.0 engine...",
            errorMessage = null
        )

        viewModelScope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            try {
                val mastered = MatcheringEngine.process(
                    target = target,
                    reference = reference,
                    config = _uiState.value.config,
                    listener = object : MatcheringEngine.ProgressListener {
                        override fun onProgress(stage: String, percent: Float) {
                            _uiState.value = _uiState.value.copy(
                                processingStage = stage,
                                processingProgress = percent
                            )
                        }
                    }
                )

                val elapsedMs = System.currentTimeMillis() - startTime
                val report = MasteringReport(
                    targetRmsDbfs = target.rmsDbfs,
                    targetPeakDbfs = target.peakDbfs,
                    referenceRmsDbfs = reference.rmsDbfs,
                    referencePeakDbfs = reference.peakDbfs,
                    masteredRmsDbfs = mastered.rmsDbfs,
                    masteredPeakDbfs = mastered.peakDbfs,
                    loudnessGainDb = mastered.rmsDbfs - target.rmsDbfs,
                    targetCorrelation = target.stereoCorrelation,
                    masteredCorrelation = mastered.stereoCorrelation,
                    processingTimeMs = elapsedMs
                )

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        masteredAudio = mastered,
                        isProcessing = false,
                        processingProgress = 1.0f,
                        lastReport = report
                    )
                    audioPlayer.updateTracks(target, mastered, reference)
                    audioPlayer.switchSource(PlaybackSource.MASTERED)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = "Mastering failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateConfig(config: MatcheringConfig) {
        _uiState.value = _uiState.value.copy(config = config)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun setExportDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showExportDialog = visible)
    }

    fun setReportDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showReportDialog = visible)
    }

    fun setConfigDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showConfigDialog = visible)
    }

    fun setInfoDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showInfoDialog = visible)
    }

    fun exportMasteredWav(context: Context, bitDepth: WavBitDepth): File? {
        val audio = _uiState.value.masteredAudio ?: return null
        return AudioEncoder.exportToCacheFile(context, audio, bitDepth)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
