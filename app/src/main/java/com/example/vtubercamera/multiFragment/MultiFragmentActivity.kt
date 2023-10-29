package com.example.vtubercamera.multiFragment

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.vtubercamera.MainActivity
import com.example.vtubercamera.R
import com.example.vtubercamera.databinding.ActivityMultiFragmentBinding

class multiFragmentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMultiFragmentBinding
    private val presenter = MultiFragmentPresenter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMultiFragmentBinding.inflate(layoutInflater)
        val toolbar = binding.multiFragmentToolbar
        setContentView(binding.root)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val fragmentManager = supportFragmentManager
        val initFragment = recyclerItemFragment()
        fragmentManager.beginTransaction().replace(R.id.multi_fragment_container, initFragment)
            .commit()
        toolbar.setNavigationOnClickListener {
            checkPriviousFragment()
        }
    }

    private fun checkPriviousFragment() {
        val previousActivity = NavigationTracker.getPreviousActivity()
        if (previousActivity == secondFragment::class.java) {
            val goBackFragment = recyclerItemFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.multi_fragment_container, goBackFragment).addToBackStack(null)
                .commit()
            presenter.onBackButtonPressed()
        } else {
            moveActivities(MainActivity::class.java)
        }

    }


    private fun moveActivities(calledActivity: Class<*>) {
        val intent = Intent(this, calledActivity)
        startActivity(intent)
        finish()
    }
}