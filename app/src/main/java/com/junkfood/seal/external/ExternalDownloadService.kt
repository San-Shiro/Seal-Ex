package com.junkfood.seal.external

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.junkfood.seal.App
import com.junkfood.seal.IDownloadCallback
import com.junkfood.seal.IExternalDownloadService
import com.junkfood.seal.MainActivity
import com.junkfood.seal.util.NotificationUtil
import com.junkfood.seal.util.NotificationUtil.SERVICE_NOTIFICATION_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val TAG = "ExternalDownloadService"

/**
 * Exported bound service that allows external apps to request media downloads
 * through Seal's download engine via AIDL.
 *
 * External apps bind to this service using:
 * ```
 * val intent = Intent("com.junkfood.seal.action.EXTERNAL_DOWNLOAD")
 * intent.setPackage("com.junkfood.seal")
 * bindService(intent, connection, Context.BIND_AUTO_CREATE)
 * ```
 *
 * The service starts a foreground notification while bound, and auto-stops
 * when all clients unbind and no active downloads remain.
 */
class ExternalDownloadService : Service() {

    private val manager: ExternalDownloadManager by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var bindCount = 0

    private val binder = object : IExternalDownloadService.Stub() {

        override fun requestDownload(url: String?, presetName: String?): String {
            if (url.isNullOrBlank()) {
                throw IllegalArgumentException("URL must not be null or blank")
            }
            Log.d(TAG, "requestDownload: url=$url preset=$presetName")
            App.startService()
            return manager.requestDownload(url, presetName)
        }

        override fun requestDownloadWithConfig(url: String?, configJson: String?): String {
            if (url.isNullOrBlank()) {
                throw IllegalArgumentException("URL must not be null or blank")
            }
            Log.d(TAG, "requestDownloadWithConfig: url=$url")
            App.startService()
            return manager.requestDownloadWithConfig(url, configJson ?: "{}")
        }

        override fun cancelDownload(taskId: String?) {
            if (taskId.isNullOrBlank()) return
            Log.d(TAG, "cancelDownload: $taskId")
            manager.cancelDownload(taskId)
        }

        override fun getPresets(): String {
            var result = "[]"
            // getPresets is a suspend function, run it blocking on the binder thread
            kotlinx.coroutines.runBlocking {
                result = manager.getPresets()
            }
            return result
        }

        override fun getDefaultConfig(): String {
            return manager.getDefaultConfig()
        }

        override fun registerCallback(callback: IDownloadCallback?) {
            if (callback != null) {
                manager.callbacks.register(callback)
                Log.d(TAG, "Callback registered")
            }
        }

        override fun unregisterCallback(callback: IDownloadCallback?) {
            if (callback != null) {
                manager.callbacks.unregister(callback)
                Log.d(TAG, "Callback unregistered")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind: ${intent?.action}")
        bindCount++

        // Start foreground to keep process alive during downloads
        val pendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = NotificationUtil.makeServiceNotification(pendingIntent)
        startForeground(SERVICE_NOTIFICATION_ID + 1, notification)

        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        bindCount--

        if (bindCount <= 0) {
            // Check if there are still active external downloads
            if (!manager.hasActiveTasks()) {
                Log.d(TAG, "No active tasks, stopping service")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                // Wait for active tasks to finish, then stop
                serviceScope.launch {
                    while (manager.hasActiveTasks()) {
                        kotlinx.coroutines.delay(2000)
                    }
                    Log.d(TAG, "All tasks finished, stopping service")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }

        return false
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        manager.callbacks.kill()
        super.onDestroy()
    }
}
