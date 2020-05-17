package com.example.filechange

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.filechange.databinding.ActivityXBinding

class XActivity : Activity() {

    private lateinit var screenCaptureManager: ScreenCaptureManager
    private var hasSet = false
    private lateinit var mBinding: ActivityXBinding

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityXBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        screenCaptureManager = ScreenCaptureManager.newInstance(this)

        checkPermission()
    }

    private fun checkPermission() {
        val checkSelfPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 99
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 99) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        start()
    }

    private fun start() {
        if (!hasSet) {
            screenCaptureManager.setListener(object : ScreenCaptureManager.CaptureListener {

                override fun onCapture(imagePath: String?) {
                    mBinding.tips.text = imagePath.toString()
                    Toast.makeText(this@XActivity, "catch screen shot!", Toast.LENGTH_LONG).show()
                }
            })
            screenCaptureManager.start()
            hasSet = true
        }
    }

    override fun onPause() {
        super.onPause()
        stop()
    }

    private fun stop() {
        if (hasSet) {
            screenCaptureManager.stop()
            hasSet = false
        }
    }
}