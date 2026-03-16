package com.example.hermit

import android.content.Context
import java.io.File

class ModelManager(private val context: Context) {
    private val modelDir = File(context.getExternalFilesDir(null), "models")

    init {
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
    }

    fun getDownloadedModels(): List<LocalModel> {
        return modelDir.listFiles { file -> file.extension == "gguf" }?.map {
            LocalModel(it.name, it.absolutePath, true)
        } ?: emptyList()
    }

    fun deleteModel(path: String): Boolean {
        val file = File(path)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}
