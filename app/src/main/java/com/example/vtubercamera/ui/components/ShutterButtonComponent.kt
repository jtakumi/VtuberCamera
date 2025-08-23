package com.example.vtubercamera.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.vtubercamera.R

@Composable
fun ShutterButtonComponent(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Camera,
            contentDescription = stringResource(R.string.take_photo)
        )
    }
}
