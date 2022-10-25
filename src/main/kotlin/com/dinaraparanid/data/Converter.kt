package com.dinaraparanid.data

import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.YoutubeDLException
import com.sapher.youtubedl.YoutubeDLRequest
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

private val CONVERTED_TRACKS_PATH = "${System.getProperty("user.dir")}/vtm_tracks"

internal fun getVideoData(url: String) = VideoInfo(YoutubeDL.getVideoInfo(url)!!)

sealed interface ConversionStatus {
    data class Success(val file: File) : ConversionStatus

    enum class Error : ConversionStatus {
        NO_INTERNET,
        INCORRECT_URL_LINK,
        UNKNOWN_ERROR
    }
}

internal fun convertVideo(url: String, ext: TrackFileExtension, videoTitle: String): ConversionStatus {
    val request = YoutubeDLRequest(url).apply {
        setOption("--get-filename")
        setOption("--extract-audio")
        setOption("--audio-format", ext.extension)
        setOption("-o", "$CONVERTED_TRACKS_PATH/%(title)s.%(ext)s")
        setOption("--socket-timeout", "1")
        setOption("--retries", "infinite")
    }

    val (fileName) = try {
        YoutubeDL.execute(request).out.split('\n').map(String::trim)
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

    return getFileOrError(fileName, ext, videoTitle)
}

private fun getFileOrError(filename: String, ext: TrackFileExtension, videoTitle: String): ConversionStatus {
    val path = "$CONVERTED_TRACKS_PATH/$filename.$ext"
    val file = File(path)

    if (!file.exists())
        return ConversionStatus.Error.UNKNOWN_ERROR

    AudioFileIO.read(file).run {
        tagOrCreateAndSetDefault.setField(FieldKey.TITLE, videoTitle)
        commit()
    }

    return ConversionStatus.Success(File(path))
}