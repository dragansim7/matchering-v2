package com.matchering.app

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Converts any Android-supported audio file (via content Uri) to a 16-bit PCM WAV file.
 *
 * Uses MediaExtractor + MediaCodec to decode the audio track to raw PCM,
 * then wraps it in a standard WAV (RIFF) header.
 */
object AudioConverter {

    /**
     * @return true on success, false if no audio track was found.
     */
    fun convertToWav(context: Context, uri: Uri, outputFile: File): Boolean {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)

        var audioTrackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                break
            }
        }
        if (audioTrackIndex == -1) {
            extractor.release()
            return false
        }

        extractor.selectTrack(audioTrackIndex)
        val format = extractor.getTrackFormat(audioTrackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        } else 1

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val pcmData = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        var sawEOS = false
        val timeoutUs = 10_000L

        while (true) {
            if (!sawEOS) {
                val inputIdx = codec.dequeueInputBuffer(timeoutUs)
                if (inputIdx >= 0) {
                    val inputBuf = codec.getInputBuffer(inputIdx)!!
                    inputBuf.clear()
                    val sampleSize = extractor.readSampleData(inputBuf, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(
                            inputIdx, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        sawEOS = true
                    } else {
                        codec.queueInputBuffer(
                            inputIdx, 0, sampleSize, extractor.sampleTime, 0
                        )
                        extractor.advance()
                    }
                }
            }

            val outputIdx = codec.dequeueOutputBuffer(info, timeoutUs)
            if (outputIdx >= 0) {
                if (info.size > 0) {
                    val outputBuf = codec.getOutputBuffer(outputIdx)!!
                    val chunk = ByteArray(info.size)
                    outputBuf.get(chunk)
                    pcmData.write(chunk)
                }
                codec.releaseOutputBuffer(outputIdx, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        writeWav(outputFile, pcmData.toByteArray(), sampleRate, channels)
        return true
    }

    /**
     * Write raw 16-bit PCM data into a WAV file with a standard RIFF header.
     */
    private fun writeWav(file: File, pcm: ByteArray, sampleRate: Int, channels: Int) {
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcm.size
        val totalSize = 36 + dataSize  // RIFF header (12) + fmt chunk (24) + data

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        // RIFF header
        header.put("RIFF".toByteArray())
        header.putInt(totalSize)
        header.put("WAVE".toByteArray())
        // fmt chunk
        header.put("fmt ".toByteArray())
        header.putInt(16)  // PCM fmt chunk size
        header.putShort(1)  // PCM format
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(bitsPerSample.toShort())
        // data chunk
        header.put("data".toByteArray())
        header.putInt(dataSize)

        FileOutputStream(file).use { fos ->
            fos.write(header.array())
            fos.write(pcm)
        }
    }
}
