package com.example.smarttoolkit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smarttoolkit.databinding.ActivityMainBinding
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize AdMob
        MobileAds.initialize(this) {}

        // Load Banner Ad
        val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)

        // Set up listeners
        binding.btnOcr.setOnClickListener {
            startActivity(Intent(this, OcrActivity::class.java))
        }

        binding.btnQr.setOnClickListener {
            startActivity(Intent(this, QrScannerActivity::class.java))
        }

        binding.btnPdf.setOnClickListener {
            startActivity(Intent(this, ImageToPdfActivity::class.java))
        }
    }
}
