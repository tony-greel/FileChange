package com.example.filechange

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.FileObserver
import android.util.Log

class MainActivity : AppCompatActivity() {

    val PATH = "${Environment.getExternalStorageDirectory()}/DCIM/Screenshots/"
    private var mFileChangedObserver: FileChangedObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i("zym", "PATH: $PATH")
        mFileChangedObserver = FileChangedObserver(PATH)
    }

    override fun onResume() {
        super.onResume()
        mFileChangedObserver?.startWatching()
    }

    override fun onStop() {
        super.onStop()
        mFileChangedObserver?.stopWatching()
    }
}
