package com.example.filechange

import android.os.FileObserver
import android.util.Log

class FileChangedObserver(path: String) : FileObserver(path) {

    private var mFilePath: String? = path

    override fun onEvent(event: Int, path: String?) {
        Log.i("zym", "onEvent, path: $mFilePath$path")
    }
}