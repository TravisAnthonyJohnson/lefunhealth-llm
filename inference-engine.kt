package com.lefunhealth.llm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.MappedByteBuffer

class InferenceEngine(
    private val modelLoader: ModelLoader,
    private val responseHandler: ResponseHandler
) {
    companion object {
        private const val TAG = "InferenceEngine"
        private const val DEFAULT_BATCH_SIZE = 8
        private const val DEFAULT_MAX_TOKENS = 256
        private const val DEFAULT_TEMPERATURE = 0.7f
        private const val DEFAULT_TOP_K = 40
        private const val DEFAULT_TOP_P = 0.9f
    }

    private external fun nativeInitialize(modelBuffer: MappedByteBuffer): Long
    private external fun nativeRunInference(
        contextPtr: Long,
        input: String,
        maxTokens: Int,
        temperature: Float,
        topK: Int,
        topP: Float
    ): String
    private external fun nativeCleanup(contextPtr: Long)

    private var contextPtr: Long = 0L

    init {
        System.loadLibrary("lefunhealth_llm")
    }

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            modelLoader.getModelBuffer()?.let { buffer ->
                contextPtr = nativeInitialize(buffer)
                contextPtr != 0L
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing inference engine: ${e.message}")
            false
        }
    }

    suspend fun processQuery(
        query: String,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
        temperature: Float = DEFAULT_TEMPERATURE,
        topK: Int = DEFAULT_TOP_K,
        topP: Float = DEFAULT_TOP_P
    ): String = withContext(Dispatchers.IO) {
        try {
            if (contextPtr == 0L) {
                throw IllegalStateException("Inference engine not initialized")
            }

            val cachedResponse = responseHandler.getCachedResponse(query)
            if (cachedResponse != null) {
                return@withContext cachedResponse
            }

            val response = nativeRunInference(
                contextPtr,
                query,
                maxTokens,
                temperature,
                topK,
                topP
            )

            responseHandler.cacheResponse(query, response)
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error processing query: ${e.message}")
            "Error processing query. Please try again."
        }
    }

    fun cleanup() {
        if (contextPtr != 0L) {
            nativeCleanup(contextPtr)
            contextPtr = 0L
        }
    }
}