package com.lefunhealth.llm.util

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.lefunhealth.llm.InferenceEngine
import com.lefunhealth.llm.ModelLoader
import com.lefunhealth.llm.ResponseHandler
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class ModelManager(
    private val context: Context
) : DefaultLifecycleObserver, CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    private val modelLoader = ModelLoader()
    private val responseHandler = ResponseHandler()
    private val inferenceEngine = InferenceEngine(modelLoader, responseHandler)
    
    private val isModelLoaded = AtomicBoolean(false)
    private val isInitializing = AtomicBoolean(false)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        launch {
            ensureModelLoaded()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (!isBackgroundProcessingRequired()) {
            unloadModel()
        }
    }

    suspend fun ensureModelLoaded() {
        if (isModelLoaded.get() || isInitializing.get()) return

        try {
            isInitializing.set(true)
            withContext(Dispatchers.IO) {
                if (modelLoader.loadModel(context)) {
                    if (inferenceEngine.initialize()) {
                        isModelLoaded.set(true)
                    }
                }
            }
        } finally {
            isInitializing.set(false)
        }
    }

    fun unloadModel() {
        if (!isModelLoaded.get()) return

        launch {
            withContext(Dispatchers.IO) {
                inferenceEngine.cleanup()
                modelLoader.unloadModel()
                isModelLoaded.set(false)
            }
        }
    }

    suspend fun processQuery(query: String): String {
        ensureModelLoaded()
        return inferenceEngine.processQuery(query)
    }

    private fun isBackgroundProcessingRequired(): Boolean {
        // Implement logic to determine if background processing is needed
        return false
    }

    fun cleanup() {
        unloadModel()
        job.cancel()
    }
}