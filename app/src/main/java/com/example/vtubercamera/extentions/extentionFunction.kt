package com.example.vtubercamera.extentions

import android.media.MediaPlayer

fun playSound(mediaPlayer: MediaPlayer) {
    mediaPlayer.apply {
        isLooping = false
        start()
    }
}
