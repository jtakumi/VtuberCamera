package com.example.vtubercamera

import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.vtubercamera.databinding.ActivityOpeningBinding

class OpeningActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOpeningBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpeningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.enterTap.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            playSound()
            finish()
        }
    }
    private fun playSound(){
       val mediaPlayer = MediaPlayer.create(this,R.raw.enter_app).apply {
           isLooping = false
           start()
       }
    }
}