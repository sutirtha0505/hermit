package com.example.nativelib

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NativeLib {

    /**
     * Loads the model from the given path.
     * @param modelPath Absolute path to the .gguf model file.
     * @return 0 on success, non-zero on error.
     */
    external fun loadModel(modelPath: String): Int

    /**
     * Frees the loaded model and associated resources.
     */
    external fun unloadModel()

    /**
     * Generates a response for the given prompt.
     * This is a simplified synchronous version for the JNI baseline.
     */
    external fun completion(prompt: String): String

    /**
     * Cancels an ongoing generation.
     */
    external fun stopGeneration()

    /**
     * Interface for streaming tokens.
     */
    private var onTokenReceived: ((String) -> Unit)? = null

    fun setTokenListener(listener: (String) -> Unit) {
        onTokenReceived = listener
    }

    // This would be called from C++
    private fun postToken(token: String) {
        onTokenReceived?.invoke(token)
    }

    companion object {
        init {
            System.loadLibrary("nativelib")
        }
    }
}