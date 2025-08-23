package com.example.vtubercamera.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vtubercamera.R

@Composable
fun PermissionRequestComponent(
    title: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onButtonClick, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)) {
            Text(buttonText)
        }
    }
}

@Preview(locale = "ja")
@Preview(locale = "en")
@Composable
fun CameraPermissionRequestComponentPreview() {
    PermissionRequestComponent(
        title = stringResource(id = R.string.camera_permission_required),
        buttonText = stringResource(id = R.string.grant_camera_permission),
        onButtonClick = {}
    )
}

@Preview(locale = "ja")
@Preview(locale = "en")
@Composable
fun PhotoPermissionRequestComponentPreview() {
    PermissionRequestComponent(
        title = stringResource(id = R.string.grant_storage_permission),
        buttonText = stringResource(id = R.string.grant_storage_permission),
        onButtonClick = {}
    )
}
