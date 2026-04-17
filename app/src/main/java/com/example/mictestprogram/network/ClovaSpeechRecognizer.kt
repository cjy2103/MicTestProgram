package com.example.mictestprogram.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ClovaSpeechRecognizer {

    private val endpoint = "https://naveropenapi.apigw.ntruss.com/recog/v1/stt?lang=Kor"

    private val apiKeyId = "여기에 키를 입력하세요"
    private val apiKeySecret = "여기에 키를 입력하세요"

    suspend fun recognize(wavBytes: ByteArray): String = withContext(Dispatchers.IO) {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Content-Type", "application/octet-stream")
            setRequestProperty("X-NCP-APIGW-API-KEY-ID", apiKeyId)
            setRequestProperty("X-NCP-APIGW-API-KEY", apiKeySecret)
        }

        return@withContext try {
            BufferedOutputStream(connection.outputStream).use { output ->
                output.write(wavBytes)
                output.flush()
            }

            val responseText = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: ""
            }

            parseRecognizedText(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRecognizedText(response: String): String {
        if (response.isBlank()) return ""
        return runCatching {
            val json = JSONObject(response)
            json.optString("text", "")
        }.getOrDefault("")
    }
}
