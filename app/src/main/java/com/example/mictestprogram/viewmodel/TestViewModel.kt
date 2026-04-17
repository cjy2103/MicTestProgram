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

data class TestCommand(
    val id: Int,
    val mainCategory: String,
    val subCategory: String,
    val word: String
)

private val testCommands = listOf(
    TestCommand(1, "기본제어", "전원", "파워온"),
    TestCommand(2, "기본제어", "전원", "파워오프"),
    TestCommand(3, "기본제어", "이동", "홈으로"),
    TestCommand(4, "기본제어", "제어", "그만"),
    TestCommand(5, "기본제어", "제어", "계속"),
    TestCommand(6, "기본제어", "제어", "하지마"),
    TestCommand(7, "기본제어", "제어", "해제"),
    TestCommand(8, "기본제어", "호출", "도우미"),
    TestCommand(9, "정보조회", "뉴스", "뉴스"),
    TestCommand(10, "정보조회", "뉴스", "뉴스안내"),
    TestCommand(11, "정보조회", "뉴스", "뉴스정보"),
    TestCommand(12, "정보조회", "날씨", "날씨정보"),
    TestCommand(13, "정보조회", "시간", "현재시간"),
    TestCommand(14, "정보조회", "일정", "할일"),
    TestCommand(15, "정보조회", "상태", "현재"),
    TestCommand(16, "정보조회", "상태", "확인"),
    TestCommand(17, "정보조회", "안내", "도시안내"),
    TestCommand(18, "정보조회", "안내", "행사안내"),
    TestCommand(19, "정보조회", "안내", "학습안내"),
    TestCommand(20, "정보조회", "안내", "홍보관소개"),
    TestCommand(21, "이동동작", "이동", "회전"),
    TestCommand(22, "이동동작", "이동", "걷기동작"),
    TestCommand(23, "이동동작", "이동", "그쪽으로"),
    TestCommand(24, "이동동작", "이동", "현관으로와"),
    TestCommand(25, "이동동작", "이동", "냉장고로와"),
    TestCommand(26, "이동동작", "시선", "나를봐"),
    TestCommand(27, "이동동작", "속도", "느리게"),
    TestCommand(28, "이동동작", "행동", "공가져와"),
    TestCommand(29, "이동동작", "행동", "도리도리"),
    TestCommand(30, "이동동작", "행동", "꼬리흔들어"),
    TestCommand(31, "TV미디어", "전원", "티비켜"),
    TestCommand(32, "TV미디어", "전원", "티비꺼"),
    TestCommand(33, "TV미디어", "전원", "티비전원"),
    TestCommand(34, "TV미디어", "채널", "채널4번"),
    TestCommand(35, "TV미디어", "채널", "티비일번"),
    TestCommand(36, "TV미디어", "채널", "티비이번"),
    TestCommand(37, "TV미디어", "채널", "티비오번"),
    TestCommand(38, "TV미디어", "채널", "티비육번"),
    TestCommand(39, "TV미디어", "채널", "티비칠번"),
    TestCommand(40, "TV미디어", "채널", "티비십일번"),
    TestCommand(41, "조명가전", "거실조명", "거실불켜라"),
    TestCommand(42, "조명가전", "거실조명", "거실소등"),
    TestCommand(43, "조명가전", "거실조명", "거실점등"),
    TestCommand(44, "조명가전", "거실조명", "거실어둡게"),
    TestCommand(45, "조명가전", "거실조명", "거실조명꺼"),
    TestCommand(46, "조명가전", "거실조명", "거실조명꺼라"),
    TestCommand(47, "조명가전", "홈바조명", "홈바불켜"),
    TestCommand(48, "조명가전", "홈바조명", "홈바불꺼"),
    TestCommand(49, "조명가전", "홈바조명", "홈바소등"),
    TestCommand(50, "조명가전", "환경설정", "풍량설정")
)

private val testWords = testCommands.map { it.word }


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
                    phaseMessage = "삐 소리 후 ${speakingDurationMillis / 2000.0}초 동안 단어를 말해주세요.",
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
