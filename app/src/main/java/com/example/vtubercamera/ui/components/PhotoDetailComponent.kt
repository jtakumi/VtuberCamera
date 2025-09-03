package com.example.vtubercamera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.vtubercamera.R
import com.example.vtubercamera.data.PhotoItem

@Composable
fun PhotoDetailView(
    photo: PhotoItem,
    onBack: () -> Unit,
    onDelete: (PhotoItem) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    hasNext: Boolean,
    hasPrevious: Boolean
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_photo_title)) },
            text = { Text(stringResource(R.string.delete_photo_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(photo)
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PhotoImage(photo = photo)
        TopActionBar(onBack = onBack, onDelete = { showDeleteDialog = true })
        NavigationButtons(
            hasNext = hasNext,
            hasPrevious = hasPrevious,
            onNext = onNext,
            onPrevious = onPrevious,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
private fun PhotoImage(photo: PhotoItem) {
    AsyncImage(
        model = photo.uri,
        contentDescription = photo.displayName,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun TopActionBar(
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Color.White
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun NavigationButtons(
    hasNext: Boolean,
    hasPrevious: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            if (hasPrevious) {
                FloatingActionButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.previous_photo)
                    )
                }
            }

            if (hasNext) {
                FloatingActionButton(
                    onClick = onNext,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.next_photo)
                    )
                }
            }
    }
}
