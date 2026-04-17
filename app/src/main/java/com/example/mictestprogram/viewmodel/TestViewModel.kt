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
    "파워온", "파워오프", "홈으로", "그만", "계속", "하지마", "해제", "도우미",
    "뉴스", "뉴스안내", "뉴스정보", "날씨정보", "현재시간", "할일", "현재", "확인",
    "도시안내", "행사안내", "학습안내", "홍보관소개", "회전", "걷기동작", "그쪽으로",
    "현관으로와", "냉장고로와", "나를봐", "느리게", "공가져와", "도리도리", "꼬리흔들어",
    "티비켜", "티비꺼", "티비전원", "채널4번", "티비일번", "티비이번", "티비오번",
    "티비육번", "티비칠번", "티비십일번", "거실불켜라", "거실소등", "거실점등", "거실어둡게",
    "거실조명꺼", "거실조명꺼라", "홈바불켜", "홈바불꺼", "홈바소등", "풍량설정"
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
    val totalRounds: Int = testWords.size,
    val currentWord: String = "",
    val phaseMessage: String = "시작 버튼을 누르면 테스트가 시작됩니다.",
    val lastRecognizedWord: String = "",
    val remainingMillis: Long = 0L,
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

    fun startTest(rounds: Int = testWords.size, speakingDurationMillis: Long = 2_500L) {
        if (_uiState.value.isRunning) return
        if (!_uiState.value.hasPermission) {
            _uiState.value = _uiState.value.copy(errorMessage = "마이크 권한이 필요합니다.")
            return
        }

        viewModelScope.launch {
            val results = mutableListOf<TestRoundResult>()
            val effectiveRounds = rounds.coerceAtMost(testWords.size)
            val selectedWords = testWords.take(effectiveRounds)

            _uiState.value = UiState(
                hasPermission = true,
                isRunning = true,
                totalRounds = effectiveRounds,
                phaseMessage = "테스트를 준비 중입니다..."
            )

            selectedWords.forEachIndexed { index, targetWord ->
                _uiState.value = _uiState.value.copy(
                    currentRound = index + 1,
                    currentWord = targetWord,
                    phaseMessage = "삐 소리 후 ${speakingDurationMillis / 1000.0}초 동안 단어를 말해주세요.",
                    lastRecognizedWord = "(대기중)",
                    remainingMillis = speakingDurationMillis,
                    currentRoundCorrect = null,
                    errorMessage = null
                )

                delay(500)
                beep()
                delay(120)

                val timerJob = viewModelScope.launch {
                    var remain = speakingDurationMillis
                    while (remain >= 0L) {
                        _uiState.value = _uiState.value.copy(remainingMillis = remain)
                        delay(100)
                        remain -= 100
                    }
                }

                val recognized = runCatching {
                    val wav = recorder.recordWav(speakingDurationMillis)
                    recognizer.recognize(wav)
                }.getOrElse {
                    _uiState.value = _uiState.value.copy(errorMessage = "음성 인식 중 오류: ${it.message}")
                    ""
                }.also {
                    timerJob.cancel()
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
                    remainingMillis = 0L,
                    currentRoundCorrect = isCorrect,
                    phaseMessage = "제시 단어: $targetWord / 인식 단어: ${cleanedRecognized.ifBlank { "(인식 실패)" }}"
                )

                delay(1_000)
            }

            val correctCount = results.count { it.isCorrect }
            val accuracy = if (effectiveRounds == 0) 0.0 else (correctCount * 100.0 / effectiveRounds)

            val summary = SessionSummary(
                total = effectiveRounds,
                correct = correctCount,
                accuracy = (accuracy * 10).roundToInt() / 10.0,
                roundResults = results
            )

            repository.saveResult(
                TestResultEntity(
                    testDateMillis = System.currentTimeMillis(),
                    totalCount = effectiveRounds,
                    correctCount = correctCount,
                    accuracy = summary.accuracy,
                    details = results.joinToString(" | ") {
                        "${it.targetWord}:${it.recognizedWord}:${if (it.isCorrect) "OK" else "NG"}"
                    }
                )
            )

            _uiState.value = _uiState.value.copy(
                isRunning = false,
                remainingMillis = 0L,
                sessionSummary = summary,
                phaseMessage = "테스트 완료"
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun beep() {
        ToneGenerator(AudioManager.STREAM_ALARM, 100).apply {
            startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
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
