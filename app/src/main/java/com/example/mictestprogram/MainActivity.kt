package com.example.mictestprogram

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.mictestprogram.data.TestDatabase
import com.example.mictestprogram.data.TestRepository
import com.example.mictestprogram.network.ClovaSpeechRecognizer
import com.example.mictestprogram.ui.MicTestApp
import com.example.mictestprogram.viewmodel.TestViewModel
import com.example.mictestprogram.viewmodel.TestViewModelFactory

class MainActivity : ComponentActivity() {

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onPermissionGranted()
        }
    }

    private val viewModel: TestViewModel by viewModels {
        val dao = TestDatabase.getDatabase(applicationContext).testResultDao()
        val repository = TestRepository(dao)
        val recognizer = ClovaSpeechRecognizer()
        TestViewModelFactory(applicationContext, repository, recognizer)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            MicTestApp(viewModel = viewModel)
        }
    }
}
