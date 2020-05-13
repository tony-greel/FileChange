package com.example.filechange

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class XActivity: Activity() {

    private lateinit var screenShotListenManager: ScreenShotListenManager
    private var isHasScreenShotListener = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_x)
        screenShotListenManager = ScreenShotListenManager.newInstance(this)

        check()
    }

    private fun check() {
        val checkSelfPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 99)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode ==99) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startScreenShotListen()
    }

    private fun startScreenShotListen() {
        if (!isHasScreenShotListener) {
            screenShotListenManager.setListener(object : ScreenShotListenManager.OnScreenShotListener {

                override fun onShot(imagePath: String?) {
                    Log.i("XActivity", "->获得截图路径：$imagePath")
                }
            })
            screenShotListenManager.startListen()
            isHasScreenShotListener = true
        }
    }

    override fun onPause() {
        super.onPause()
        stopScreenShotListen()
    }

    private fun stopScreenShotListen() {
        if (isHasScreenShotListener) {
            screenShotListenManager.stopListen()
            isHasScreenShotListener = false
        }
    }
}