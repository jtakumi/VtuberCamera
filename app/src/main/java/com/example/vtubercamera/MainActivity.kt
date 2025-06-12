package com.example.vtubercamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.vtubercamera.ui.screens.CameraScreen
import com.example.vtubercamera.ui.theme.VTuberCameraTheme
import com.example.vtubercamera.utils.initializeAndroid15

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
                    CameraScreen()
                }
            }
        }
    }
}
