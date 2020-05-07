package com.example.filechange

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.FileObserver
import android.util.Log
import android.widget.Button
import java.io.File
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    val PATH = "${Environment.getExternalStorageDirectory()}/DCIM/Screenshots/"
    private var mFileChangedObserver: FileChangedObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        Log.i("zym", "PATH: $PATH")

        val file = getDir()
        mFileChangedObserver = FileChangedObserver(file.path, FileObserver.CREATE)

        val random = Random(10)
        findViewById<Button>(R.id.create).setOnClickListener {
            val filePath = file.absolutePath + File.separator + random.nextInt() + ".txt"
            val newFile = File(filePath)
            newFile.createNewFile()
        }
    }

    private fun getDir(): File {
        var file = getExternalFilesDir(null)
        if (file == null) {
            file = File(filesDir, "FileObserver")
        }
        if (!file.exists()) {
            file.mkdir()
        }
        return file
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
