package com.lefunhealth.llm.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.lefunhealth.llm.LefunHealthApp
import com.lefunhealth.llm.R
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class LLMService : Service(), CoroutineScope {
    private lateinit var wakeLock: PowerManager.WakeLock
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    override fun onCreate() {
        super.onCreate()
        setupWakeLock()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        wakeLock.acquire(10*60*1000L /*10 minutes*/)
        
        launch {
            try {
                LefunHealthApp.instance.modelManager.ensureModelLoaded()
            } finally {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private fun setupWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "LefunHealth::LLMServiceWakeLock"
        )
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(getString(R.string.service_running))
        .setSmallIcon(R.drawable.ic_notification)
        .setOngoing(true)
        .also { notification ->
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        .build()

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "LefunHealthLLMService"
    }
}