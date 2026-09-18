package com.example.matchering.audio

import android.content.Context
import android.net.Uri
import com.example.matchering.model.AudioData
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

enum class WavBitDepth(val bits: Int, val label: String) {
    BIT_16(16, "16-bit PCM WAV (CD Standard)"),
    BIT_24(24, "24-bit PCM WAV (High Res Studio)"),
    BIT_32_FLOAT(32, "32-bit Float WAV (Master Quality)")
}

object AudioEncoder {

    fun writeWav(audio: AudioData, outputStream: OutputStream, bitDepth: WavBitDepth = WavBitDepth.BIT_16) {
        val sampleRate = audio.sampleRate
        val numChannels = 2
        val numSamples = audio.sampleCount
        val bytesPerSample = bitDepth.bits / 8
        val subChunk2Size = numSamples * numChannels * bytesPerSample
        val chunkSize = 36 + subChunk2Size

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        // RIFF header
        header.put("RIFF".toByteArray())
        header.putInt(chunkSize)
        header.put("WAVE".toByteArray())

        // fmt subchunk
        header.put("fmt ".toByteArray())
        header.putInt(16) // Subchunk1Size
        val audioFormat = if (bitDepth == WavBitDepth.BIT_32_FLOAT) 3.toShort() else 1.toShort()
        header.putShort(audioFormat)
        header.putShort(numChannels.toShort())
        header.putInt(sampleRate)
        val byteRate = sampleRate * numChannels * bytesPerSample
        header.putInt(byteRate)
        val blockAlign = (numChannels * bytesPerSample).toShort()
        header.putShort(blockAlign)
        header.putShort(bitDepth.bits.toShort())

        // data subchunk
        header.put("data".toByteArray())
        header.putInt(subChunk2Size)

        outputStream.write(header.array())

        val bufferSize = 4096
        val buffer = ByteBuffer.allocate(bufferSize * numChannels * bytesPerSample).order(ByteOrder.LITTLE_ENDIAN)

        var idx = 0
        while (idx < numSamples) {
            buffer.clear()
            val chunkCount = kotlin.math.min(bufferSize, numSamples - idx)
            for (i in 0 until chunkCount) {
                val l = audio.left[idx + i].coerceIn(-1.0f, 1.0f)
                val r = audio.right[idx + i].coerceIn(-1.0f, 1.0f)
                when (bitDepth) {
                    WavBitDepth.BIT_16 -> {
                        buffer.putShort((l * 32767.0f).roundToInt().toShort())
                        buffer.putShort((r * 32767.0f).roundToInt().toShort())
                    }
                    WavBitDepth.BIT_24 -> {
                        putInt24(buffer, (l * 8388607.0f).roundToInt())
                        putInt24(buffer, (r * 8388607.0f).roundToInt())
                    }
                    WavBitDepth.BIT_32_FLOAT -> {
                        buffer.putFloat(l)
                        buffer.putFloat(r)
                    }
                }
            }
            outputStream.write(buffer.array(), 0, chunkCount * numChannels * bytesPerSample)
            idx += chunkCount
        }
        outputStream.flush()
    }

    private fun putInt24(buffer: ByteBuffer, value: Int) {
        buffer.put((value and 0xFF).toByte())
        buffer.put(((value shr 8) and 0xFF).toByte())
        buffer.put(((value shr 16) and 0xFF).toByte())
    }

    fun exportToCacheFile(context: Context, audio: AudioData, bitDepth: WavBitDepth = WavBitDepth.BIT_16): File {
        val sanitizedTitle = audio.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val file = File(context.cacheDir, "$sanitizedTitle.wav")
        FileOutputStream(file).use { out ->
            writeWav(audio, out, bitDepth)
        }
        return file
    }
}
