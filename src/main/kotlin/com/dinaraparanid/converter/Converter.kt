package com.dinaraparanid.converter

import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.YoutubeDLException
import com.sapher.youtubedl.YoutubeDLRequest
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.UnsupportedOperationException
import java.net.URL
import java.time.Duration
import kotlin.time.toKotlinDuration

private val CONVERTED_TRACKS_PATH = "${System.getProperty("user.dir")}/vtm_tracks"

private inline val YoutubeDLException.errorType: YoutubeDLRequestStatus.Error
    get() {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)

        printStackTrace(printWriter)
        printStackTrace()

        val stackTrack = stringWriter.toString()

        return when {
            "Unable to download" in stackTrack -> YoutubeDLRequestStatus.Error.NO_INTERNET
            "Video unavailable" in stackTrack -> YoutubeDLRequestStatus.Error.INCORRECT_URL_LINK
            else -> YoutubeDLRequestStatus.Error.UNKNOWN_ERROR
        }
    }

private val json = Json { ignoreUnknownKeys = true }

private val converterScope = object : CoroutineScope by MainScope() {}

@Volatile
private var isYoutubeDLUpdateTaskStarted = false

private suspend fun updateYoutubeDLAsync() = converterScope.launch(Dispatchers.IO) {
    isYoutubeDLUpdateTaskStarted = true

    while (true) {
        Runtime.getRuntime().exec("youtube-dl -U")
        delay(Duration.ofHours(1).toKotlinDuration())
    }
}

internal suspend fun getVideoDataAsync(url: String) = converterScope.async(Dispatchers.IO) {
    if (!isYoutubeDLUpdateTaskStarted)
        updateYoutubeDLAsync()

    val request = YoutubeDLRequest(url).apply {
        setOption("dump-json")
        setOption("no-playlist")
    }

    val (title, duration, description, fileName, thumbnail) = try {
        json.decodeFromString<VideoInfo>(YoutubeDL.execute(request).out)
    } catch (e: YoutubeDLException) {
        return@async e.errorType
    }

    return@async YoutubeDLRequestStatus.Success(VideoInfo(title, duration, description, fileName, thumbnail))
}

internal fun convertVideoAsync(
    url: String,
    ext: TrackFileExtension,
    videoFileNameWithoutExt: String,
    videoTitle: String,
    videoThumbnailURL: String
) = converterScope.async(Dispatchers.IO) {
    val coverPath = when (ext) {
        is TrackFileExtension.VideoExt -> null
        else -> "$CONVERTED_TRACKS_PATH/${videoFileNameWithoutExt}_cover.png"
    }

    val storeThumbnailTask = when (ext) {
        is TrackFileExtension.VideoExt -> null
        else -> storeThumbnailAsync(videoThumbnailURL, coverPath!!)
    }

    val request = YoutubeDLRequest(url, CONVERTED_TRACKS_PATH).apply {
        setOption(
            "audio-format",
            when (ext) {
                is TrackFileExtension.MusicExt -> ext
                else -> TrackFileExtension.MusicExt.WAV
            }.extension
        )

        setOption("socket-timeout", 1)
        setOption("retries", "infinite")
        setOption("extract-audio")
        setOption("format", "best")

        if (ext is TrackFileExtension.VideoExt) {
            setOption("keep-video")
            setOption("recode-video", ext.extension)
        }
    }

    var tries = 10
    var error: YoutubeDLRequestStatus.Error? = null

    while (tries > 0)
        try {
            YoutubeDL.execute(request)
            break
        } catch (e: YoutubeDLException) {
            tries--
            error = e.errorType
        }

    if (tries == 0)
        return@async error!!

    return@async getFileOrError(ext, videoFileNameWithoutExt, videoTitle, coverPath, storeThumbnailTask)
}

private fun storeThumbnailAsync(videoThumbnailURL: String, coverPath: String) = converterScope.launch(Dispatchers.IO) {
    val coverData = ByteArrayOutputStream().use {
        val coverData = URL(videoThumbnailURL).readBytes()
        it.write(coverData)
        it.toByteArray()
    }

    FileOutputStream(coverPath).use { it.write(coverData) }
}

private fun Tag.setCover(coverPath: String) = try {
    deleteArtworkField()
    setField(ArtworkFactory.createArtworkFromFile(File(coverPath)))
} catch (ignored: UnsupportedOperationException) {
    // Cover change not supported for this file
}

private suspend fun setTags(trackFile: File, videoTitle: String, coverPath: String, storeThumbnailTask: Job) =
    AudioFileIO.read(trackFile).run {
        tagOrCreateAndSetDefault?.run {
            setField(FieldKey.TITLE, videoTitle)
            storeThumbnailTask.join()
            setCover(coverPath)
        }
        commit()
    }

private fun removeFilesAfterTimeoutAsync(
    videoFileNameWithoutExt: String,
    ext: TrackFileExtension,
    trackFile: File,
    coverFile: File?
) = converterScope.launch(Dispatchers.IO) {
    when (ext) {
        is TrackFileExtension.MusicExt -> coverFile?.delete()
        else -> File("$CONVERTED_TRACKS_PATH/$videoFileNameWithoutExt.wav").delete()
    }

    delay(Duration.ofDays(1).toKotlinDuration())
    trackFile.delete()
}

private suspend fun getFileOrError(
    ext: TrackFileExtension,
    videoFileNameWithoutExt: String,
    videoTitle: String,
    coverPath: String?,
    storeThumbnailTask: Job?
): YoutubeDLRequestStatus {
    val trackFile = File("$CONVERTED_TRACKS_PATH/$videoFileNameWithoutExt.${ext.extension}")

    if (ext !is TrackFileExtension.VideoExt)
        setTags(trackFile, videoTitle, coverPath!!, storeThumbnailTask!!)

    removeFilesAfterTimeoutAsync(
        videoFileNameWithoutExt,
        ext,
        trackFile,
        coverFile = when (ext) {
            is TrackFileExtension.VideoExt -> null
            else -> File(coverPath!!)
        }
    )

    return YoutubeDLRequestStatus.Success(trackFile)
}