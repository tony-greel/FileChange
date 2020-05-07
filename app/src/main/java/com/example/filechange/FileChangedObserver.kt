package com.example.filechange

import android.os.FileObserver
import android.util.Log

class FileChangedObserver(path: String, mask: Int) : FileObserver(path, mask) {

    private var mFilePath: String? = path

    override fun onEvent(event: Int, path: String?) {
        if (path == null) return
        Log.i("zym", "onEvent, path: $mFilePath/$path")
    }
}