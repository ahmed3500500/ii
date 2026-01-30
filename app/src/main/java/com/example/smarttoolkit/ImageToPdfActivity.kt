package com.example.smarttoolkit

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.smarttoolkit.databinding.ActivityImageToPdfBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.io.OutputStream

class ImageToPdfActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageToPdfBinding
    private var selectedBitmap: Bitmap? = null
    private var interstitialAd: InterstitialAd? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                val inputStream = contentResolver.openInputStream(it)
                selectedBitmap = BitmapFactory.decodeStream(inputStream)
                binding.imageView.setImageBitmap(selectedBitmap)
                binding.btnConvert.isEnabled = true
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                selectedBitmap?.let { createPdf(it) }
            } else {
                Toast.makeText(this, "Storage permission required for Android < 10", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageToPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load Ads
        binding.adView.loadAd(AdRequest.Builder().build())
        loadInterstitialAd()

        binding.btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImage.launch(intent)
        }

        binding.btnConvert.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                selectedBitmap?.let { bitmap ->
                    createPdf(bitmap)
                }
            }
        }
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
            })
    }

    private fun showInterstitial() {
        if (interstitialAd != null) {
            interstitialAd?.show(this)
        }
    }

    private fun createPdf(bitmap: Bitmap) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas: Canvas = page.canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        pdfDocument.finishPage(page)

        try {
            val filename = "SmartToolKit_${System.currentTimeMillis()}.pdf"
            var outputStream: OutputStream? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
                }
                val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                if (uri != null) {
                    outputStream = contentResolver.openOutputStream(uri)
                }
            } else {
                // For older versions, would need to handle file paths manually and request WRITE_EXTERNAL_STORAGE
                // But for this demo, let's assume scoped storage or just MediaStore works for Documents on newer
                // or just use app specific dir if permission fails. 
                // Given the constraints and environment, we stick to MediaStore for best compatibility on modern devices
                // If on older device without scoped storage, we might need simple File API
                val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val file = java.io.File(path, filename)
                outputStream = java.io.FileOutputStream(file)
            }

            if (outputStream != null) {
                pdfDocument.writeTo(outputStream)
                outputStream.close()
                Toast.makeText(this, R.string.pdf_saved, Toast.LENGTH_LONG).show()
                showInterstitial()
            } else {
                Toast.makeText(this, "Failed to create file stream", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}
