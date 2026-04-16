<div align="center">

<img width="" src="fastlane/metadata/android/en-US/images/icon.png"  width=160 height=160  align="center">

# SealEx

### Video/Audio Downloader for Android with Automation capabilities

> **Note:** SealEx is a fork of the original [Seal](https://github.com/JunkFood02/Seal) application. Our goal is to extend Seal's excellent core downloader with the ability to create automation

English
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/JunkFood02/Seal/blob/main/translations/README-zh_Hans.md">简体中文</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/JunkFood02/Seal/blob/main/translations/README-zh_Hant.md">繁體中文</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/JunkFood02/Seal/blob/main/translations/README-ar.md">العربية</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/JunkFood02/Seal/blob/main/translations/README-pt.md">Portuguese</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/JunkFood02/Seal/blob/main/translations/README-ua.md">Українська</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/JunkFood02/Seal/blob/main/translations/README-th.md">ภาษาไทย</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/JunkFood02/Seal/blob/main/translations/README-fa.md">فارسی</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/JunkFood02/Seal/blob/main/translations/README-it.md">Italiano</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/JunkFood02/Seal/blob/main/translations/README-az.md">Azərbaycanca</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/JunkFood02/Seal/blob/main/translations/README-ru.md">Русский</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/JunkFood02/Seal/blob/main/translations/README-sr.md">Српски</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/JunkFood02/Seal/blob/main/translations/README-ja.md">日本語</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/JunkFood02/Seal/blob/main/translations/README-id.md">Indonesia</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/JunkFood02/Seal/blob/main/translations/README-hi.md">हिंदी</a>
&nbsp;&nbsp;| &nbsp;&nbsp;
<a href="https://github.com/JunkFood02/Seal/blob/main/translations/README-bn.md">বাংলা</a>



[![GitHub release (latest by date)](https://img.shields.io/github/v/release/San-Shiro/SealEx?color=black&label=Stable&logo=github)](https://github.com/San-Shiro/SealEx/releases/latest/)


</div>


## 📱 Screenshots

<div align="center">
<div>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg" width="30%" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg" width="30%" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.jpg" width="30%" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.jpg" width="30%" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.jpg" width="30%" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.jpg" width="30%" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/7.jpg" width="30%" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/8.jpg" width="30%" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/9.jpg" width="30%" />
</div>
</div>

<br>

## 📖 Features
- Download videos and audio files from video platforms supported by [yt-dlp](https://github.com/yt-dlp/yt-dlp) (formerly youtube-dl).

- Embed metadata and video thumbnail into extracted audio files supported by [mutagen](https://github.com/quodlibet/mutagen).

- Download all videos in the playlist with one click.

- Use embedded [aria2c](https://github.com/aria2/aria2) as external downloader for all your downloads.

- Embed subtitles into the downloaded videos.

- Execute custom yt-dlp commands with templates.

- Manage in-app downloads and custom command templates.

- Easy to use and user-friendly.

- [Material Design 3](https://m3.material.io/) style UI, with dynamic color theme.

- MAD: UI and logic written with pure Kotlin. Single activity, no fragments, only composable destinations.



## 🤖 External Download Service (AIDL)

SealEx exposes a full AIDL-based IPC service that lets any Android app bind directly to SealEx's download engine and control it programmatically. This is ideal for automation apps (**Tasker**, **MacroDroid**), companion apps, and scripts that need real-time progress tracking and fine-grained control over downloads.

> [!IMPORTANT]
> You must first enable **"Enable External Download Service"** in SealEx → Settings → General to activate the service.

---

### Step 1 — Enable the Service

In SealEx, go to **Settings → General → Enable External Download Service** and toggle it on.

---

### Step 2 — Copy the AIDL files

Copy both AIDL files into your project under the **same package path**:

```
app/src/main/aidl/com/sanshiro/sealEx/
  ├── IExternalDownloadService.aidl
  └── IDownloadCallback.aidl
```

---

### Step 3 — Bind to the Service

```kotlin
val serviceIntent = Intent("com.sanshiro.sealEx.action.EXTERNAL_DOWNLOAD").apply {
    setPackage("com.sanshiro.sealex")   // SealEx package name
}

val connection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        val service = IExternalDownloadService.Stub.asInterface(binder)
        // service is ready to use
    }
    override fun onServiceDisconnected(name: ComponentName) { /* handle disconnect */ }
}

// Backward-compatible action also works:
// Intent("com.junkfood.seal.action.EXTERNAL_DOWNLOAD").setPackage("com.sanshiro.sealex")

context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
```

---

### IExternalDownloadService — Interface Methods

| Method | Description |
|--------|-------------|
| `requestDownload(url, presetName)` | Start a download using a named command template, or pass `null`/`""` for app defaults. Returns a `taskId`. |
| `requestDownloadWithConfig(url, configJson)` | Start a download with JSON-overridden preferences (e.g. `{"extractAudio": true}`). Returns a `taskId`. |
| `cancelDownload(taskId)` | Cancel an in-progress download by its `taskId`. |
| `getPresets()` | Returns a JSON array of all saved command templates plus the default config. |
| `getDefaultConfig()` | Returns the current default `DownloadPreferences` as a full JSON string. |
| `registerCallback(callback)` | Register an `IDownloadCallback` to receive live progress events. |
| `unregisterCallback(callback)` | Unregister a previously registered callback. |

**`requestDownloadWithConfig` — configJson example:**
```json
{
  "extractAudio": true,
  "audioFormat": "mp3",
  "aria2c": true,
  "embedThumbnail": true
}
```

---

### IDownloadCallback — Callback Methods

Register a callback with `registerCallback()` to receive real-time events:

| Callback | Parameters | Description |
|----------|-----------|-------------|
| `onInfoFetched` | `taskId`, `infoJson` | Fired when metadata is fetched. `infoJson` contains `title`, `uploader`, `duration`, `thumbnailUrl`, `fileSizeApprox`. |
| `onProgress` | `taskId`, `progress (0.0–1.0)`, `progressText` | Fired repeatedly during download with speed/ETA text. |
| `onCompleted` | `taskId`, `resultJson` | Fired on success. `resultJson` contains `filePath`, `title`, `uploader`, `duration`. |
| `onError` | `taskId`, `errorMessage` | Fired on failure with an error description. |
| `onCanceled` | `taskId` | Fired when the download is canceled by the client. |

**Full usage example:**
```kotlin
val callback = object : IDownloadCallback.Stub() {
    override fun onInfoFetched(taskId: String, infoJson: String) {
        val info = JSONObject(infoJson)
        Log.d("SealEx", "Downloading: ${info.getString("title")}")
    }
    override fun onProgress(taskId: String, progress: Float, progressText: String) {
        Log.d("SealEx", "Progress: $progressText")
    }
    override fun onCompleted(taskId: String, resultJson: String) {
        val result = JSONObject(resultJson)
        Log.d("SealEx", "Saved to: ${result.getString("filePath")}")
    }
    override fun onError(taskId: String, errorMessage: String) {
        Log.e("SealEx", "Error: $errorMessage")
    }
    override fun onCanceled(taskId: String) {
        Log.d("SealEx", "Canceled: $taskId")
    }
}

service.registerCallback(callback)
val taskId = service.requestDownload("https://youtube.com/watch?v=jNQXAC9IVRw", null)
```

---

### Supported Bind Actions

Both of these Intent actions are registered — your client can use either:

| Action | Description |
|--------|-------------|
| `com.sanshiro.sealEx.action.EXTERNAL_DOWNLOAD` | Primary SealEx action |
| `com.junkfood.seal.action.EXTERNAL_DOWNLOAD` | Backward-compatible (works with tools built for original Seal) |

## ⬇️ Download

For most devices, it is recommended to install the **arm64-v8a** version of the apks.

- Download the latest stable version straight from our [GitHub Releases](https://github.com/San-Shiro/SealEx/releases/latest)

## 🤝 Contributing

Contributions are welcome!

>[!Note]
>
>For submitting bug reports, feature requests, questions, or any other ideas to improve the fork, please file an issue on our GitHub tracker.

## 🧱 Credits

Seal is a simple GUI of [yt-dlp](https://github.com/yt-dlp/yt-dlp), based on [youtubedl-android](https://github.com/yausername/youtubedl-android)

Some of the UI designs and codes are borrowed from [Read You](https://github.com/Ashinch/ReadYou) and [Music You](https://github.com/Kyant0/MusicYou)

[dvd](https://github.com/yausername/dvd)

[Material color utilities](https://github.com/material-foundation/material-color-utilities)

[Monet](https://github.com/Kyant0/Monet)

## 📃 License

[![GitHub](https://img.shields.io/github/license/JunkFood02/Seal?style=for-the-badge)](https://github.com/JunkFood02/Seal/blob/main/LICENSE)

>[!Warning]
>
>Except for the source code licensed under the GPLv3 license,
>all other parties are prohibited from using Seal's name as a downloader app,
>and the same is true for Seal's derivatives.
>Derivatives include but are not limited to forks and unofficial builds.

<div align="right">
<table><td>
<a href="#start-of-content">👆 Scroll to top</a>
</td></table>
</div>
