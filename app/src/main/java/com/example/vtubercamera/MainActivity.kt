package com.example.vtubercamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.vtubercamera.ui.screens.ARCameraScreen
import com.example.vtubercamera.ui.screens.AvatarLibraryScreen
import com.example.vtubercamera.ui.screens.CameraScreen
import com.example.vtubercamera.ui.theme.VTuberCameraTheme
import com.example.vtubercamera.utils.initializeAndroid15
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vtubercamera.ui.viewmodels.CameraViewModel

private enum class MainScreen {
    CAMERA,
    AR,
    AVATAR_LIBRARY
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android15の機能が使えるかどうか初期チェック
        initializeAndroid15()
        setContent {
            VTuberCameraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val cameraViewModel: CameraViewModel = hiltViewModel()
                    var currentScreen by rememberSaveable { mutableStateOf(MainScreen.CAMERA) }

                    when (currentScreen) {
                        MainScreen.CAMERA -> {
                            CameraScreen(
                                onNavigateToAR = { currentScreen = MainScreen.AR },
                                viewModel = cameraViewModel
                            )
                        }

                        MainScreen.AR -> {
                            ARCameraScreen(
                                onNavigateBack = { currentScreen = MainScreen.CAMERA },
                                onNavigateToAvatarLibrary = { currentScreen = MainScreen.AVATAR_LIBRARY },
                                viewModel = cameraViewModel
                            )
                        }

                        MainScreen.AVATAR_LIBRARY -> {
                            AvatarLibraryScreen(
                                onNavigateBack = { currentScreen = MainScreen.AR },
                                onAvatarSelected = { avatar ->
                                    cameraViewModel.selectAvatarFromLibrary(avatar.id)
                                    currentScreen = MainScreen.AR
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
