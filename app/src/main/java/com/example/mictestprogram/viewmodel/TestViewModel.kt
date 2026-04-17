package com.example.mictestprogram.viewmodel

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mictestprogram.audio.PcmRecorder
import com.example.mictestprogram.data.TestRepository
import com.example.mictestprogram.data.TestResultEntity
import com.example.mictestprogram.network.ClovaSpeechRecognizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val testWords = listOf(
    "마이크", "음성", "테스트", "안드로이드", "코틀린",
    "컴포즈", "인식", "정확도", "기록", "평가"
)

data class TestRoundResult(
    val targetWord: String,
    val recognizedWord: String,
    val isCorrect: Boolean
)

data class SessionSummary(
    val total: Int,
    val correct: Int,
    val accuracy: Double,
    val roundResults: List<TestRoundResult>
)

data class UiState(
    val hasPermission: Boolean = false,
    val isRunning: Boolean = false,
    val currentRound: Int = 0,
    val totalRounds: Int = 5,
    val currentWord: String = "",
    val phaseMessage: String = "시작 버튼을 누르면 테스트가 시작됩니다.",
    val lastRecognizedWord: String = "",
    val currentRoundCorrect: Boolean? = null,
    val sessionSummary: SessionSummary? = null,
    val errorMessage: String? = null
)

class TestViewModel(
    private val context: Context,
    private val repository: TestRepository,
    private val recognizer: ClovaSpeechRecognizer,
    private val recorder: PcmRecorder = PcmRecorder()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val history = repository.observeHistory().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun onPermissionGranted() {
        _uiState.value = _uiState.value.copy(hasPermission = true)
    }

    fun startTest(rounds: Int = 5, speakingDurationMillis: Long = 2_500L) {
        if (_uiState.value.isRunning) return
        if (!_uiState.value.hasPermission) {
            _uiState.value = _uiState.value.copy(errorMessage = "마이크 권한이 필요합니다.")
            return
        }

        viewModelScope.launch {
            val results = mutableListOf<TestRoundResult>()
            _uiState.value = UiState(
                hasPermission = true,
                isRunning = true,
                totalRounds = rounds,
                phaseMessage = "테스트를 준비 중입니다..."
            )

            repeat(rounds) { index ->
                val targetWord = testWords.shuffled().first()
                _uiState.value = _uiState.value.copy(
                    currentRound = index + 1,
                    currentWord = targetWord,
                    phaseMessage = "삐 소리 후 ${speakingDurationMillis / 2000.0}초 동안 단어를 말해주세요.",
                    lastRecognizedWord = "",
                    currentRoundCorrect = null,
                    errorMessage = null
                )

                delay(500)
                beep()
                delay(120)

                val recognized = runCatching {
                    val wav = recorder.recordWav(speakingDurationMillis)
                    recognizer.recognize(wav)
                }.getOrElse {
                    _uiState.value = _uiState.value.copy(errorMessage = "음성 인식 중 오류: ${it.message}")
                    ""
                }

                val cleanedTarget = targetWord.trim()
                val cleanedRecognized = recognized.trim()
                val isCorrect = cleanedTarget == cleanedRecognized

                results += TestRoundResult(
                    targetWord = targetWord,
                    recognizedWord = cleanedRecognized.ifBlank { "(인식 실패)" },
                    isCorrect = isCorrect
                )

                _uiState.value = _uiState.value.copy(
                    lastRecognizedWord = cleanedRecognized.ifBlank { "(인식 실패)" },
                    currentRoundCorrect = isCorrect,
                    phaseMessage = "제시 단어: $targetWord / 인식 단어: ${cleanedRecognized.ifBlank { "(인식 실패)" }}"
                )

                delay(1_000)
            }

            val correctCount = results.count { it.isCorrect }
            val accuracy = if (rounds == 0) 0.0 else (correctCount * 100.0 / rounds)

            val summary = SessionSummary(
                total = rounds,
                correct = correctCount,
                accuracy = (accuracy * 10).roundToInt() / 10.0,
                roundResults = results
            )

            repository.saveResult(
                TestResultEntity(
                    testDateMillis = System.currentTimeMillis(),
                    totalCount = rounds,
                    correctCount = correctCount,
                    accuracy = summary.accuracy,
                    details = results.joinToString(" | ") {
                        "${it.targetWord}:${it.recognizedWord}:${if (it.isCorrect) "OK" else "NG"}"
                    }
                )
            )

            _uiState.value = _uiState.value.copy(
                isRunning = false,
                sessionSummary = summary,
                phaseMessage = "테스트 완료"
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun beep() {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80).apply {
            startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            release()
        }
    }
}

class TestViewModelFactory(
    private val context: Context,
    private val repository: TestRepository,
    private val recognizer: ClovaSpeechRecognizer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TestViewModel(context, repository, recognizer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
