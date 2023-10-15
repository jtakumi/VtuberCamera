package com.example.vtubercamera.extentions

import android.media.MediaPlayer
import com.example.vtubercamera.R


fun playSound(mediaPlayer: MediaPlayer) {
    mediaPlayer.apply {
        isLooping = false
        start()
    }
}