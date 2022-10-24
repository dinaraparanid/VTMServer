package com.dinaraparanid.data

import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.YoutubeDLException
import com.sapher.youtubedl.YoutubeDLRequest
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

private val CONVERTED_TRACKS_PATH = "${System.getProperty("user.dir")}/vtm_tracks"

internal fun getVideoData(url: String) = VideoInfo(YoutubeDL.getVideoInfo(url)!!)

sealed interface ConversionStatus {
    data class Success(val file: File) : ConversionStatus

    enum class Error : ConversionStatus {
        NO_INTERNET,
        INCORRECT_URL_LINK
    }
}

internal fun convertVideo(url: String, ext: TrackFileExtension): ConversionStatus {
    val request = YoutubeDLRequest(url).apply {
        setOption("--extract-audio")
        setOption("--audio-format", ext.extension)
        setOption("-o", "$CONVERTED_TRACKS_PATH/%(title)s.%(ext)s")
        setOption("--socket-timeout", "1")
        setOption("--retries", "infinite")
    }

    try {
        YoutubeDL.execute(request)
    } catch (e: YoutubeDLException) {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)

        e.printStackTrace(printWriter)
        e.printStackTrace()

        val stackTrack = stringWriter.toString()

        return when {
            "Unable to download webpage" in stackTrack -> ConversionStatus.Error.NO_INTERNET
            else -> ConversionStatus.Error.INCORRECT_URL_LINK
        }
    }

    return ConversionStatus.Success(getFile(url, ext))
}

private fun getFile(url: String, ext: TrackFileExtension): File {
    val path = "$CONVERTED_TRACKS_PATH/$url.$ext"

    // TODO: Set file tags and return file

    return File(path)
}