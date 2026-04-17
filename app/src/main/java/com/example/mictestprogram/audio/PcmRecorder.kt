package com.example.mictestprogram.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PcmRecorder {

    private val sampleRate = 16_000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    @SuppressLint("MissingPermission")
    suspend fun recordWav(durationMillis: Long): ByteArray = withContext(Dispatchers.IO) {
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, 4096)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        val rawAudio = ByteArrayOutputStream()
        val buffer = ByteArray(bufferSize)

        audioRecord.startRecording()
        try {
            val endTime = System.currentTimeMillis() + durationMillis
            while (System.currentTimeMillis() < endTime) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    rawAudio.write(buffer, 0, read)
                }
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }

        val pcmBytes = rawAudio.toByteArray()
        wavHeader(pcmBytes.size) + pcmBytes
    }

    private fun wavHeader(audioLength: Int): ByteArray {
        val totalDataLen = audioLength + 36
        val byteRate = sampleRate * 2
        return ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(totalDataLen)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(sampleRate)
            putInt(byteRate)
            putShort(2)
            putShort(16)
            put("data".toByteArray())
            putInt(audioLength)
        }.array()
    }
}
