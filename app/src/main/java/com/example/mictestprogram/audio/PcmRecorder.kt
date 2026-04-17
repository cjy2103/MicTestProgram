package com.example.mictestprogram.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PcmRecorder {

    private val sampleRate = 16_000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    suspend fun recordWav(durationMillis: Long): ByteArray = withContext(Dispatchers.IO) {
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize * 4, 8192)
        val audioRecord = createBestAudioRecord(bufferSize)
            ?: throw IllegalStateException("AudioRecord 초기화 실패")

        val rawAudio = ByteArrayOutputStream()
        val buffer = ByteArray(bufferSize)

        audioRecord.startRecording()
        val sessionId = audioRecord.audioSessionId
        val noiseSuppressor = if (NoiseSuppressor.isAvailable()) {
            NoiseSuppressor.create(sessionId)?.apply { enabled = true }
        } else {
            null
        }
        val automaticGainControl = if (AutomaticGainControl.isAvailable()) {
            AutomaticGainControl.create(sessionId)?.apply { enabled = true }
        } else {
            null
        }
        val acousticEchoCanceler = if (AcousticEchoCanceler.isAvailable()) {
            AcousticEchoCanceler.create(sessionId)?.apply { enabled = true }
        } else {
            null
        }

        try {
            // 녹음 시작 직후 하드웨어 안정화 구간(잡음 많은 초반 샘플)은 버림
            val warmupEndTime = System.currentTimeMillis() + 200L
            while (System.currentTimeMillis() < warmupEndTime) {
                audioRecord.read(buffer, 0, buffer.size)
            }

            val endTime = System.currentTimeMillis() + durationMillis
            while (System.currentTimeMillis() < endTime) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    rawAudio.write(buffer, 0, read)
                }
            }
        } finally {
            noiseSuppressor?.release()
            automaticGainControl?.release()
            acousticEchoCanceler?.release()
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

    private fun createBestAudioRecord(bufferSize: Int): AudioRecord? {
        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
        )

        for (source in sources) {
            val record = AudioRecord(
                source,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            if (record.state == AudioRecord.STATE_INITIALIZED) {
                return record
            }
            record.release()
        }
        return null
    }
}
