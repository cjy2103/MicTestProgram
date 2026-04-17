package com.example.mictestprogram.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mictestprogram.viewmodel.TestViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Screen(val route: String, val title: String) {
    Test("test", "테스트"),
    History("history", "기록표")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MicTestApp(viewModel: TestViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(if (screen == Screen.Test) "🎙️" else "📋") },
                        label = { Text(screen.title) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Test.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Test.route) {
                TestScreen(
                    state = uiState,
                    onStartClick = { viewModel.startTest() }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(history = history)
            }
        }
    }
}

@Composable
private fun TestScreen(
    state: com.example.mictestprogram.viewmodel.UiState,
    onStartClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("음성 인식 평가", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            text = state.phaseMessage,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        if (state.isRunning) {
            Text("남은 시간", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = "${"%.1f".format(state.remainingMillis / 1000f)}초",
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))

            Text("테스트 단어", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = state.currentWord.ifBlank { "(준비중)" },
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))

            Text("내가 말한 단어", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = state.lastRecognizedWord.ifBlank { "(대기중)" },
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
        }

        Text("진행: ${state.currentRound}/${state.totalRounds}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Button(onClick = onStartClick, enabled = !state.isRunning) {
            Text(if (state.isRunning) "테스트 진행 중..." else "테스트 시작", fontSize = 24.sp)
        }

        Spacer(Modifier.height(20.dp))
        state.sessionSummary?.let { summary ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("결과", style = MaterialTheme.typography.titleMedium)
                    Text("정상 인식 횟수: ${summary.correct} / ${summary.total}")
                    Text("정확도: ${summary.accuracy}%")
                }
            }

            Spacer(Modifier.height(12.dp))
            LazyColumn(contentPadding = PaddingValues(bottom = 64.dp)) {
                items(summary.roundResults) { round ->
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("제시: ${round.targetWord}")
                                Text("인식: ${round.recognizedWord}")
                            }
                            Text(if (round.isCorrect) "정상" else "오인식")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(history: List<com.example.mictestprogram.data.TestResultEntity>) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text("기록표", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
        }

        if (history.isEmpty()) {
            item {
                Text("아직 저장된 기록이 없습니다.")
            }
        } else {
            items(history) { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(formatter.format(Date(result.testDateMillis)), fontWeight = FontWeight.Bold)
                        Text("정상 인식: ${result.correctCount}/${result.totalCount}")
                        Text("정확도: ${result.accuracy}%")
                        Text("상세: ${result.details}")
                    }
                }
            }
        }
    }
}
