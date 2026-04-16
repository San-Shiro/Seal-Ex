package com.junkfood.seal.external

import android.os.RemoteCallbackList
import android.util.Log
import androidx.compose.runtime.snapshotFlow
import com.junkfood.seal.IDownloadCallback
import com.junkfood.seal.database.objects.CommandTemplate
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.Task
import com.junkfood.seal.download.Task.DownloadState
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.DownloadUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

private const val TAG = "ExternalDownloadManager"

/**
 * Bridge between the AIDL interface and Seal's [DownloaderV2].
 *
 * Responsibilities:
 * - Creates [Task] objects from URLs + preset/config from external callers
 * - Enqueues them into [DownloaderV2]
 * - Monitors [Task.State] changes via SnapshotFlow and dispatches AIDL callbacks
 * - Provides preset/template listing from the database
 *
 * This class is Koin-managed and injected into [ExternalDownloadService].
 */
class ExternalDownloadManager(
    private val downloader: DownloaderV2,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val callbacks = RemoteCallbackList<IDownloadCallback>()

    /**
     * Set of task IDs that were created by the external service.
     * We only dispatch callbacks for these tasks, not for in-app downloads.
     */
    private val externalTaskIds = mutableSetOf<String>()

    /**
     * Map of task IDs to the last known download state, used to detect transitions
     * and avoid duplicate callbacks.
     */
    private val lastKnownState = mutableMapOf<String, DownloadState>()

    /**
     * Whether we've started monitoring the task state map.
     * We only start once there's at least one external task.
     */
    private var isMonitoring = false

    /** Enqueue a download using the current default preferences. */
    fun requestDownload(url: String, presetName: String?): String {
        val preferences = if (!presetName.isNullOrEmpty()) {
            // Look up template by name and create a custom command task
            val template = findTemplateByName(presetName)
            if (template != null) {
                return requestDownloadWithTemplate(url, template)
            }
            // If no template found by that name, fall back to defaults
            Log.w(TAG, "Template '$presetName' not found, using defaults")
            DownloadUtil.DownloadPreferences.createFromPreferences()
        } else {
            DownloadUtil.DownloadPreferences.createFromPreferences()
        }

        return enqueueTask(url, preferences)
    }

    /** Enqueue a download with specific config overrides merged onto defaults. */
    fun requestDownloadWithConfig(url: String, configJson: String): String {
        val override = try {
            externalJson.decodeFromString<ExternalConfigOverride>(configJson)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse config JSON, using defaults", e)
            return enqueueTask(url, DownloadUtil.DownloadPreferences.createFromPreferences())
        }

        val base = DownloadUtil.DownloadPreferences.createFromPreferences()
        val merged = override.applyTo(base)
        return enqueueTask(url, merged)
    }

    /** Cancel an external download task. */
    fun cancelDownload(taskId: String): Boolean {
        if (taskId !in externalTaskIds) return false
        return downloader.cancel(taskId)
    }

    /** Returns all available presets as a JSON array. */
    suspend fun getPresets(): String {
        val presets = mutableListOf<ExternalPresetInfo>()

        // 1. Current default preferences
        val defaults = DownloadUtil.DownloadPreferences.createFromPreferences()
        presets.add(
            ExternalPresetInfo(
                name = "__default__",
                type = "preferences",
                extractAudio = defaults.extractAudio,
                aria2c = defaults.aria2c,
                cookies = defaults.cookies,
                videoResolution = defaults.videoResolution,
                videoFormat = defaults.videoFormat,
                audioFormat = defaults.audioFormat,
                audioQuality = defaults.audioQuality,
                proxy = defaults.proxy,
                proxyUrl = defaults.proxyUrl,
            )
        )

        // 2. All saved command templates from the database
        try {
            val templates = DatabaseUtil.getTemplateList()
            templates.forEach { template ->
                presets.add(
                    ExternalPresetInfo(
                        name = template.name,
                        type = "command_template",
                        id = template.id,
                        template = template.template,
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch templates", e)
        }

        return externalJson.encodeToString(presets)
    }

    /** Returns the current default config as JSON. */
    fun getDefaultConfig(): String {
        val defaults = DownloadUtil.DownloadPreferences.createFromPreferences()
        return externalJson.encodeToString(defaults)
    }

    /** Check if there are any active external tasks. */
    fun hasActiveTasks(): Boolean {
        val taskMap = downloader.getTaskStateMap()
        return externalTaskIds.any { id ->
            val task = taskMap.keys.find { it.id == id }
            if (task != null) {
                val state = taskMap[task]?.downloadState
                state != null && state !is DownloadState.Completed &&
                    state !is DownloadState.Error && state !is DownloadState.Canceled
            } else false
        }
    }

    // ── Internal ────────────────────────────────────────────────────

    private fun enqueueTask(url: String, preferences: DownloadUtil.DownloadPreferences): String {
        val task = Task(url = url, preferences = preferences)
        val taskId = task.id

        synchronized(externalTaskIds) {
            externalTaskIds.add(taskId)
        }

        downloader.enqueue(task)
        ensureMonitoring()

        Log.d(TAG, "Enqueued external task: $taskId for URL: $url")
        return taskId
    }

    private fun requestDownloadWithTemplate(url: String, template: CommandTemplate): String {
        val preferences = DownloadUtil.DownloadPreferences.createFromPreferences()
        val task = Task(
            url = url,
            preferences = preferences,
            type = Task.TypeInfo.CustomCommand(template),
        )
        val taskId = task.id

        synchronized(externalTaskIds) {
            externalTaskIds.add(taskId)
        }

        downloader.enqueue(task)
        ensureMonitoring()

        Log.d(TAG, "Enqueued external template task: $taskId (${ template.name}) for URL: $url")
        return taskId
    }

    private fun findTemplateByName(name: String): CommandTemplate? {
        // Use the cached template list from PreferenceUtil which reads from a StateFlow
        return com.junkfood.seal.util.PreferenceUtil.templateListStateFlow.value
            .find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Start monitoring the DownloaderV2 task state map for changes.
     * Dispatches AIDL callbacks when external task states change.
     */
    private fun ensureMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        scope.launch(Dispatchers.Default) {
            snapshotFlow { downloader.getTaskStateMap().toMap() }
                .map { map -> map.filter { it.key.id in externalTaskIds } }
                .distinctUntilChanged()
                .collect { externalTasks ->
                    externalTasks.forEach { (task, state) ->
                        processStateChange(task, state)
                    }
                }
        }
    }

    private fun processStateChange(task: Task, state: Task.State) {
        val taskId = task.id
        val downloadState = state.downloadState
        val previousState = lastKnownState[taskId]

        // Skip if state hasn't actually changed
        if (previousState == downloadState) return
        lastKnownState[taskId] = downloadState

        when (downloadState) {
            is DownloadState.FetchingInfo -> {
                // No callback for fetching start — the client already knows from requestDownload
            }

            DownloadState.ReadyWithInfo -> {
                // Info has been fetched — dispatch onInfoFetched
                val infoJson = if (state.videoInfo != null) {
                    ExternalVideoInfo.fromVideoInfo(state.videoInfo).toJson()
                } else {
                    ExternalVideoInfo.fromViewState(state.viewState).toJson()
                }
                broadcastCallback { cb -> cb.onInfoFetched(taskId, infoJson) }
            }

            is DownloadState.Running -> {
                broadcastCallback { cb ->
                    cb.onProgress(taskId, downloadState.progress, downloadState.progressText)
                }
            }

            is DownloadState.Completed -> {
                val result =
                    ExternalDownloadResult.from(state.viewState, downloadState.filePath)
                broadcastCallback { cb -> cb.onCompleted(taskId, result.toJson()) }
                cleanupTask(taskId)
            }

            is DownloadState.Error -> {
                val message = downloadState.throwable.message ?: "Unknown error"
                broadcastCallback { cb -> cb.onError(taskId, message) }
                cleanupTask(taskId)
            }

            is DownloadState.Canceled -> {
                broadcastCallback { cb -> cb.onCanceled(taskId) }
                cleanupTask(taskId)
            }

            DownloadState.Idle -> {
                // Initial state, no callback
            }
        }
    }

    private fun cleanupTask(taskId: String) {
        synchronized(externalTaskIds) {
            externalTaskIds.remove(taskId)
        }
        lastKnownState.remove(taskId)
    }

    private inline fun broadcastCallback(action: (IDownloadCallback) -> Unit) {
        val n = callbacks.beginBroadcast()
        try {
            for (i in 0 until n) {
                try {
                    action(callbacks.getBroadcastItem(i))
                } catch (e: Exception) {
                    Log.e(TAG, "Callback dispatch failed for item $i", e)
                }
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }
}
