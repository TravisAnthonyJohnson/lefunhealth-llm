package com.lefunhealth.llm

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ProcessLifecycleOwner
import com.lefunhealth.llm.service.LLMService
import com.lefunhealth.llm.util.ModelManager

class LefunHealthApp : Application() {
    lateinit var modelManager: ModelManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        modelManager = ModelManager(this)
        
        // Initialize the model in background
        ProcessLifecycleOwner.get().lifecycle.addObserver(modelManager)
        
        // Start the LLM service
        startLLMService()
    }

    private fun startLLMService() {
        val serviceIntent = Intent(this, LLMService::class.java)
        startForegroundService(serviceIntent)
    }

    companion object {
        lateinit var instance: LefunHealthApp
            private set
    }
}