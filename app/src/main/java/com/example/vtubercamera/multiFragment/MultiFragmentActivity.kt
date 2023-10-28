package com.example.vtubercamera.multiFragment

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.example.vtubercamera.R
import com.example.vtubercamera.databinding.ActivityMultiFragmentBinding

class multiFragmentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMultiFragmentBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMultiFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar =binding.multiFragmentToolbar
    }
}