package com.dinaraparanid.data

import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.YoutubeDLException
import com.sapher.youtubedl.YoutubeDLRequest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.math.pow

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

internal fun getVideoData(url: String): YoutubeDLRequestStatus {
    val request = YoutubeDLRequest(url).apply {
        setOption("dump-json")
        setOption("no-progress")
        setOption("no-playlist")
    }

    val (title, duration, description, fileName, thumbnail) = try {
        json.decodeFromString<VideoInfo>(YoutubeDL.execute(request).out)
    } catch (e: YoutubeDLException) {
        return e.errorType
    }

    return YoutubeDLRequestStatus.Success(VideoInfo(title, duration, description, fileName, thumbnail))
}

internal fun convertVideo(url: String, ext: TrackFileExtension, videoTitle: String): YoutubeDLRequestStatus {
    val request = YoutubeDLRequest(url).apply {
        setOption("extract-audio")
        setOption("audio-format", ext.extension)
        setOption("output", "$CONVERTED_TRACKS_PATH/%(title)s.%(ext)s")
        setOption("socket-timeout", "1")
        setOption("retries", "infinite")
    }

    val (fileName) = try {
        YoutubeDL.execute(request).out.split('\n').map(String::trim)
    } catch (e: YoutubeDLException) {
        return e.errorType
    }

    return getFileOrError(fileName, ext, videoTitle)
}

private fun getFileOrError(filename: String, ext: TrackFileExtension, videoTitle: String): YoutubeDLRequestStatus {
    val path = "$CONVERTED_TRACKS_PATH/$filename.$ext"
    val file = File(path)

    if (!file.exists())
        return YoutubeDLRequestStatus.Error.UNKNOWN_ERROR

    AudioFileIO.read(file).run {
        tagOrCreateAndSetDefault.setField(FieldKey.TITLE, videoTitle)
        commit()
    }

    return YoutubeDLRequestStatus.Success(File(path))
}