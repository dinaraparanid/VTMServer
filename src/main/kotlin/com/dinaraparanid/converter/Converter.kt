package com.dinaraparanid.converter

import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.YoutubeDLException
import com.sapher.youtubedl.YoutubeDLRequest
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
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
            "Unable to download webpage" in stackTrack -> YoutubeDLRequestStatus.Error.NO_INTERNET
            else -> YoutubeDLRequestStatus.Error.INCORRECT_URL_LINK
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
    videoTitle: String,
    videoThumbnail: String
) = converterScope.async(Dispatchers.IO) {
    val request = YoutubeDLRequest(url).apply {
        setOption(
            "audio-format",
            when (ext) {
                TrackFileExtension.MP4 -> TrackFileExtension.WAV
                else -> ext
            }.extension
        )

        setOption("output", "$CONVERTED_TRACKS_PATH/%(title)s.%(ext)s")
        setOption("socket-timeout", 1)
        setOption("retries", "infinite")
        setOption("audio-quality", 0)

        when (ext) {
            TrackFileExtension.MP4 -> setOption("keep-video")
            else -> setOption("extract-audio")
        }
    }

    val (fileName) = try {
        YoutubeDL.execute(request).out.split('\n').map(String::trim)
    } catch (e: YoutubeDLException) {
        return@async e.errorType
    }

    return@async getFileOrError(fileName, ext, videoTitle, videoThumbnail)
}

private fun getFileOrError(
    filename: String,
    ext: TrackFileExtension,
    videoTitle: String,
    videoThumbnail: String
): YoutubeDLRequestStatus {
    val convertedPath = "$CONVERTED_TRACKS_PATH/$filename.$ext"
    val correctPath = "$CONVERTED_TRACKS_PATH/$videoTitle.$ext"

    val file = File(convertedPath).let { convertedFile ->
        val correctFile = File(correctPath)
        convertedFile.renameTo(correctFile)
        correctFile
    }

    if (!file.exists())
        return YoutubeDLRequestStatus.Error.UNKNOWN_ERROR

    AudioFileIO.read(file).run {
        tagOrCreateAndSetDefault.run {
            setField(FieldKey.TITLE, videoTitle)
            setField(ArtworkFactory.createLinkedArtworkFromURL(videoThumbnail))
        }
        commit()
    }

    return YoutubeDLRequestStatus.Success(File(correctPath))
}