package com.example.hermit

import android.content.Context
import java.io.File

class ModelRepository(private val context: Context) {

    fun getAvailableModels(): List<RemoteModel> {
        return listOf(
            RemoteModel(
                id = "gemma-3-1b",
                name = "Gemma 3 1B Q4_K_M",
                repo = "bartowski/google_gemma-3-1b-it-GGUF",
                description = "Google's lightweight 1B model, optimized for mobile.",
                sizeBytes = 850_000_000L,
                downloadUrl = "https://huggingface.co/bartowski/google_gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-Q4_K_M.gguf?download=true",
                fileName = "gemma-3-1b-it-Q4_K_M.gguf"
            ),
            RemoteModel(
                id = "qwen2.5-1.5b",
                name = "Qwen2.5 1.5B Q4_K_M",
                repo = "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
                description = "Alibaba's powerful small language model.",
                sizeBytes = 1_100_000_000L,
                downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf?download=true",
                fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf"
            ),
            RemoteModel(
                id = "phi-3-mini",
                name = "Phi-3 Mini Q4_K_M",
                repo = "microsoft/Phi-3-mini-4k-instruct-gguf",
                description = "Microsoft's highly capable 3.8B model (quantized).",
                sizeBytes = 2_300_000_000L,
                downloadUrl = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4_k_m.gguf?download=true",
                fileName = "Phi-3-mini-4k-instruct-q4_k_m.gguf"
            ),
            RemoteModel(
                id = "tiny-llama-1.1b",
                name = "TinyLlama 1.1B Q4_K_M",
                repo = "TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF",
                description = "Extremely compact model for low-end devices.",
                sizeBytes = 670_000_000L,
                downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf?download=true",
                fileName = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"
            )
        )
    }

    fun isModelDownloaded(fileName: String): Boolean {
        val file = File(context.getExternalFilesDir("models"), fileName)
        return file.exists() && file.length() > 0
    }
    
    fun getModelFile(fileName: String): File {
        return File(context.getExternalFilesDir("models"), fileName)
    }
}
