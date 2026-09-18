package com.matchering.app

import android.content.Context
import com.chaquo.python.Python
import java.io.File

/**
 * Orchestrates the on-device matchering process:
 * 1. Convert input audio files to WAV (via AudioConverter)
 * 2. Call the Python bridge (mg_android.py) to run matchering
 * 3. Return the path to the mastered output WAV
 */
class MatcheringProcessor(private val context: Context) {

    /**
     * @param targetUri    Content Uri of the target track (to be mastered)
     * @param referenceUri Content Uri of the reference track (sound to match)
     * @return The output WAV File on success.
     * @throws Exception if processing fails.
     */
    fun process(targetUri: android.net.Uri, referenceUri: android.net.Uri): File {
        val workDir = File(context.filesDir, "matchering").apply { mkdirs() }

        // Convert both inputs to WAV
        val targetWav = File(workDir, "target_input.wav")
        val referenceWav = File(workDir, "reference_input.wav")
        val outputWav = File(workDir, "result_output.wav")

        if (!AudioConverter.convertToWav(context, targetUri, targetWav)) {
            throw Exception("Failed to decode the target audio file.")
        }
        if (!AudioConverter.convertToWav(context, referenceUri, referenceWav)) {
            throw Exception("Failed to decode the reference audio file.")
        }

        // Run matchering via the Python bridge
        val py = Python.getInstance()
        val module = py.getModule("mg_android")
        module.callAttr("run_matchering", targetWav.absolutePath, referenceWav.absolutePath, outputWav.absolutePath)

        return outputWav
    }

    /** Return log output from the last Python run. */
    fun getLogs(): String {
        val py = Python.getInstance()
        val module = py.getModule("mg_android")
        return module.callAttr("get_logs").toString()
    }
}
