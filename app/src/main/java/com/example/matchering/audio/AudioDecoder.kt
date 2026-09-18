package com.example.matchering.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.example.matchering.model.AudioData
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

object AudioDecoder {

    /**
     * Decodes an audio file from Uri into AudioData.
     * Uses MediaExtractor + MediaCodec to support MP3, WAV, FLAC, AAC, OGG.
     */
    fun decodeFromUri(context: Context, uri: Uri, title: String): AudioData {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val numTracks = extractor.trackCount
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until numTracks) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }

            if (audioTrackIndex < 0 || format == null) {
                throw IllegalArgumentException("No audio track found in selected file")
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val pcmBytes = ArrayList<ByteArray>()
            val bufferInfo = MediaCodec.BufferInfo()
            var isEos = false
            val timeoutUs = 5000L

            while (!isEos) {
                val inIndex = codec.dequeueInputBuffer(timeoutUs)
                if (inIndex >= 0) {
                    val inBuffer = codec.getInputBuffer(inIndex)
                    inBuffer?.clear()
                    val sampleSize = inBuffer?.let { extractor.readSampleData(it, 0) } ?: -1
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEos = true
                    } else {
                        val sampleTime = extractor.sampleTime
                        codec.queueInputBuffer(inIndex, 0, sampleSize, sampleTime, 0)
                        extractor.advance()
                    }
                }

                var outIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                while (outIndex >= 0) {
                    val outBuffer = codec.getOutputBuffer(outIndex)
                    if (outBuffer != null && bufferInfo.size > 0) {
                        outBuffer.position(bufferInfo.offset)
                        outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        val chunk = ByteArray(bufferInfo.size)
                        outBuffer.get(chunk)
                        pcmBytes.add(chunk)
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    outIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            // Calculate total bytes
            var totalBytes = 0
            for (chunk in pcmBytes) totalBytes += chunk.size
            val fullPcm = ByteArray(totalBytes)
            var dstOffset = 0
            for (chunk in pcmBytes) {
                System.arraycopy(chunk, 0, fullPcm, dstOffset, chunk.size)
                dstOffset += chunk.size
            }

            // Convert 16-bit PCM bytes to stereo float arrays
            val totalSamples = totalBytes / (2 * channelCount)
            val left = FloatArray(totalSamples)
            val right = FloatArray(totalSamples)

            val byteBuf = ByteBuffer.wrap(fullPcm).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until totalSamples) {
                val lSample = byteBuf.short.toFloat() / 32768.0f
                val rSample = if (channelCount >= 2) {
                    byteBuf.short.toFloat() / 32768.0f
                } else {
                    lSample
                }
                left[i] = lSample
                right[i] = rSample
            }

            return AudioData(
                title = title,
                sampleRate = sampleRate,
                left = left,
                right = right
            )
        } catch (e: Exception) {
            extractor.release()
            throw e
        }
    }
}
