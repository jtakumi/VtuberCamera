package com.example.vtubercamera.data

import android.net.Uri

data class PhotoItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val size: Long,
    val mimeType: String,
    val isARPhoto: Boolean = false,
    val avatarName: String? = null,
    val poseName: String? = null,
    val expressionName: String? = null,
    val lightingPreset: String? = null
)