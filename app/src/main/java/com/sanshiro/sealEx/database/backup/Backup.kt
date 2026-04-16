package com.sanshiro.sealEx.database.backup

import com.sanshiro.sealEx.database.objects.CommandTemplate
import com.sanshiro.sealEx.database.objects.DownloadedVideoInfo
import com.sanshiro.sealEx.database.objects.OptionShortcut
import kotlinx.serialization.Serializable

@Serializable
data class Backup(
    val templates: List<CommandTemplate>? = null,
    val shortcuts: List<OptionShortcut>? = null,
    val downloadHistory: List<DownloadedVideoInfo>? = null,
)
