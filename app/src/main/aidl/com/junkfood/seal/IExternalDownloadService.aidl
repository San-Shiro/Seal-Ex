// IExternalDownloadService.aidl
package com.junkfood.seal;

import com.junkfood.seal.IDownloadCallback;

/**
 * AIDL interface for external apps to request media downloads
 * through Seal's download engine (yt-dlp, aria2c, cookies, etc.).
 *
 * Bind to this service using:
 *   Intent intent = new Intent("com.junkfood.seal.action.EXTERNAL_DOWNLOAD");
 *   intent.setPackage("com.junkfood.seal");
 *   bindService(intent, connection, Context.BIND_AUTO_CREATE);
 */
interface IExternalDownloadService {

    /**
     * Request a download using default preferences or a named command template.
     *
     * @param url        The URL to download (YouTube, etc.)
     * @param presetName Name of a command template to use, or empty/null for defaults
     * @return taskId    A unique identifier for tracking this download
     */
    String requestDownload(String url, String presetName);

    /**
     * Request a download with specific configuration overrides.
     * Fields present in configJson override defaults; absent fields use defaults.
     *
     * @param url        The URL to download
     * @param configJson JSON partial of DownloadPreferences fields to override.
     *                   Example: {"extractAudio": true, "aria2c": true}
     * @return taskId    A unique identifier for tracking this download
     */
    String requestDownloadWithConfig(String url, String configJson);

    /**
     * Cancel an in-progress download.
     *
     * @param taskId The task identifier returned by requestDownload
     */
    void cancelDownload(String taskId);

    /**
     * List all available presets (default config + saved command templates).
     *
     * @return JSON array of preset objects:
     *   [
     *     {"name": "__default__", "type": "preferences", ...current defaults...},
     *     {"name": "Template Name", "type": "command_template", "id": 1, "template": "..."}
     *   ]
     */
    String getPresets();

    /**
     * Get the current default download preferences as a full JSON object.
     *
     * @return JSON string of all DownloadPreferences fields
     */
    String getDefaultConfig();

    /**
     * Register a callback to receive download progress and results.
     * Multiple callbacks can be registered simultaneously.
     *
     * @param callback The callback interface to register
     */
    void registerCallback(IDownloadCallback callback);

    /**
     * Unregister a previously registered callback.
     *
     * @param callback The callback interface to unregister
     */
    void unregisterCallback(IDownloadCallback callback);
}
