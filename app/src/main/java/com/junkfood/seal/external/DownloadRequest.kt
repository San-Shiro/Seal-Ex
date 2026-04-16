package com.junkfood.seal.external

import com.junkfood.seal.download.Task
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.VideoInfo
import com.junkfood.seal.util.toHttpsUrl
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * JSON serialization helpers and data models for the external download AIDL interface.
 * These models decouple the AIDL layer from Seal's internal types.
 */

internal val externalJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}

// ── Preset listing models ──────────────────────────────────────────

@Serializable
data class ExternalPresetInfo(
    val name: String,
    val type: String, // "preferences" or "command_template"
    val id: Int? = null,
    val template: String? = null,
    // Key fields from DownloadPreferences (only for type="preferences")
    val extractAudio: Boolean? = null,
    val aria2c: Boolean? = null,
    val cookies: Boolean? = null,
    val videoResolution: Int? = null,
    val videoFormat: Int? = null,
    val audioFormat: Int? = null,
    val audioQuality: Int? = null,
    val proxy: Boolean? = null,
    val proxyUrl: String? = null,
)

// ── Info fetched model ─────────────────────────────────────────────

@Serializable
data class ExternalVideoInfo(
    val title: String,
    val uploader: String,
    val duration: Int,
    val thumbnailUrl: String?,
    val extractorKey: String,
    val fileSizeApprox: Double,
) {
    companion object {
        fun fromVideoInfo(info: VideoInfo): ExternalVideoInfo = ExternalVideoInfo(
            title = info.title,
            uploader = info.uploader ?: info.channel ?: info.uploaderId.toString(),
            duration = info.duration?.roundToInt() ?: 0,
            thumbnailUrl = info.thumbnail.toHttpsUrl(),
            extractorKey = info.extractorKey,
            fileSizeApprox = info.fileSize ?: info.fileSizeApprox ?: .0,
        )

        fun fromViewState(viewState: Task.ViewState): ExternalVideoInfo = ExternalVideoInfo(
            title = viewState.title,
            uploader = viewState.uploader,
            duration = viewState.duration,
            thumbnailUrl = viewState.thumbnailUrl,
            extractorKey = viewState.extractorKey,
            fileSizeApprox = viewState.fileSizeApprox,
        )
    }

    fun toJson(): String = externalJson.encodeToString(this)
}

// ── Download result model ──────────────────────────────────────────

@Serializable
data class ExternalDownloadResult(
    val filePath: String?,
    val title: String,
    val uploader: String,
    val thumbnailUrl: String?,
    val duration: Int,
    val extractorKey: String,
) {
    companion object {
        fun from(viewState: Task.ViewState, filePath: String?): ExternalDownloadResult =
            ExternalDownloadResult(
                filePath = filePath,
                title = viewState.title,
                uploader = viewState.uploader,
                thumbnailUrl = viewState.thumbnailUrl,
                duration = viewState.duration,
                extractorKey = viewState.extractorKey,
            )
    }

    fun toJson(): String = externalJson.encodeToString(this)
}

// ── Config override model ──────────────────────────────────────────

/**
 * Partial config override. Fields that are `null` will inherit from defaults.
 * This is what clients send in `requestDownloadWithConfig(url, configJson)`.
 */
@Serializable
data class ExternalConfigOverride(
    val extractAudio: Boolean? = null,
    val aria2c: Boolean? = null,
    val cookies: Boolean? = null,
    val videoResolution: Int? = null,
    val videoFormat: Int? = null,
    val audioFormat: Int? = null,
    val audioQuality: Int? = null,
    val concurrentFragments: Int? = null,
    val proxy: Boolean? = null,
    val proxyUrl: String? = null,
    val embedThumbnail: Boolean? = null,
    val embedMetadata: Boolean? = null,
    val sponsorBlock: Boolean? = null,
    val downloadSubtitle: Boolean? = null,
    val subtitleLanguage: String? = null,
    val forceIpv4: Boolean? = null,
    val restrictFilenames: Boolean? = null,
    val rateLimit: Boolean? = null,
    val maxDownloadRate: String? = null,
) {
    /**
     * Apply this partial override on top of the given base preferences.
     * Only non-null fields from the override replace the base value.
     */
    fun applyTo(base: DownloadUtil.DownloadPreferences): DownloadUtil.DownloadPreferences =
        base.copy(
            extractAudio = extractAudio ?: base.extractAudio,
            aria2c = aria2c ?: base.aria2c,
            cookies = cookies ?: base.cookies,
            videoResolution = videoResolution ?: base.videoResolution,
            videoFormat = videoFormat ?: base.videoFormat,
            audioFormat = audioFormat ?: base.audioFormat,
            audioQuality = audioQuality ?: base.audioQuality,
            concurrentFragments = concurrentFragments ?: base.concurrentFragments,
            proxy = proxy ?: base.proxy,
            proxyUrl = proxyUrl ?: base.proxyUrl,
            embedThumbnail = embedThumbnail ?: base.embedThumbnail,
            embedMetadata = embedMetadata ?: base.embedMetadata,
            sponsorBlock = sponsorBlock ?: base.sponsorBlock,
            downloadSubtitle = downloadSubtitle ?: base.downloadSubtitle,
            subtitleLanguage = subtitleLanguage ?: base.subtitleLanguage,
            forceIpv4 = forceIpv4 ?: base.forceIpv4,
            restrictFilenames = restrictFilenames ?: base.restrictFilenames,
            rateLimit = rateLimit ?: base.rateLimit,
            maxDownloadRate = maxDownloadRate ?: base.maxDownloadRate,
        )
}
