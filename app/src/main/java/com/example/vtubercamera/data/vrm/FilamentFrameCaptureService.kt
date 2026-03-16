package com.example.vtubercamera.data.vrm

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.createBitmap
import javax.inject.Inject

class FilamentFrameCaptureService @Inject constructor() {
    fun capturePlaceholder(tag: String, width: Int, height: Int): Bitmap {
        Log.d(tag, "Capturing frame (placeholder)")
        return createBitmap(width, height)
    }
}
