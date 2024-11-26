package com.lefunhealth.llm

import android.content.Context
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.util.Log

class ModelLoader {
    private var modelMmap: MappedByteBuffer? = null
    private var isModelLoaded = false
    
    companion object {
        private const val TAG = "ModelLoader"
        private const val MODEL_FILENAME = "models/Neo-BioMistral-7B-E3-V0-1-5-Q8.gguf"
    }

    fun loadModel(context: Context): Boolean {
        return try {
            val modelFile = context.assets.openFd(MODEL_FILENAME)
            modelMmap = modelFile.createMemoryMappedBuffer()
            isModelLoaded = true
            Log.d(TAG, "Model loaded successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}")
            false
        }
    }

    private fun FileChannel.createMemoryMappedBuffer(): MappedByteBuffer {
        return map(FileChannel.MapMode.READ_ONLY, 0, size())
    }

    fun unloadModel() {
        try {
            modelMmap?.clear()
            modelMmap = null
            isModelLoaded = false
            Log.d(TAG, "Model unloaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error unloading model: ${e.message}")
        }
    }

    fun isLoaded(): Boolean = isModelLoaded

    fun getModelBuffer(): MappedByteBuffer? = modelMmap
}