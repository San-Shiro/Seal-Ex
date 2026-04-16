// IDownloadCallback.aidl
package com.sanshiro.sealEx;

/**
 * Callback interface for receiving download progress and results
 * from the ExternalDownloadService.
 *
 * All methods are called on a Binder thread â€” clients should
 * dispatch to their main thread if needed.
 */
interface IDownloadCallback {

    /**
     * Called when video/audio metadata has been fetched successfully.
     *
     * @param taskId  Unique identifier for this download task
     * @param infoJson JSON object with fields:
     *   {
     *     "title": "...",
     *     "uploader": "...",
     *     "duration": 245,
     *     "thumbnailUrl": "https://...",
     *     "extractorKey": "Youtube",
     *     "fileSizeApprox": 52428800.0
     *   }
     */
    void onInfoFetched(String taskId, String infoJson);

    /**
     * Called repeatedly during download with current progress.
     *
     * @param taskId       Unique identifier for this download task
     * @param progress     Progress value from 0.0 to 1.0 (-1 for indeterminate)
     * @param progressText Human-readable progress text (e.g. "5.2MiB/s ETA 00:12")
     */
    void onProgress(String taskId, float progress, String progressText);

    /**
     * Called when the download completes successfully.
     *
     * @param taskId     Unique identifier for this download task
     * @param resultJson JSON object with fields:
     *   {
     *     "filePath": "/storage/.../video.mp4",
     *     "title": "...",
     *     "uploader": "...",
     *     "thumbnailUrl": "https://...",
     *     "duration": 245,
     *     "extractorKey": "Youtube"
     *   }
     */
    void onCompleted(String taskId, String resultJson);

    /**
     * Called when the download fails.
     *
     * @param taskId       Unique identifier for this download task
     * @param errorMessage Description of the error
     */
    void onError(String taskId, String errorMessage);

    /**
     * Called when the download is canceled by the client.
     *
     * @param taskId Unique identifier for this download task
     */
    void onCanceled(String taskId);
}
