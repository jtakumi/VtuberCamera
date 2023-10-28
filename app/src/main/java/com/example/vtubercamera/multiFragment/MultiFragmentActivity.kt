package com.example.vtubercamera.multiFragment

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.vtubercamera.R
import com.example.vtubercamera.databinding.ActivityMultiFragmentBinding

class multiFragmentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMultiFragmentBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMultiFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar =binding.multiFragmentToolbar
        setSupportActionBar(toolbar)
        val fragmentManager =supportFragmentManager
        val initFragment = recyclerItemFragment()
        fragmentManager.beginTransaction().replace(R.id.multi_fragment_container,initFragment).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            androidx.appcompat.R.id.home->{
                val fragmentManager =supportFragmentManager
                val goBackToFragment = recyclerItemFragment()
                fragmentManager.beginTransaction().replace(R.id.multi_fragment_container,goBackToFragment).commit()
                goBackToFragment.refreshAuto()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}