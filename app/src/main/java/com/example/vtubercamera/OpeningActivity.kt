package com.example.vtubercamera

import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.vtubercamera.databinding.ActivityOpeningBinding
import com.example.vtubercamera.extentions.playSound

class OpeningActivity : AppCompatActivity(),View.OnClickListener {
    private lateinit var binding: ActivityOpeningBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpeningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.enterTap.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        playSound(MediaPlayer.create(this, R.raw.enter_app))
        finish()
    }


}