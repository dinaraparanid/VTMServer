package com.dinaraparanid.converter

import java.io.PrintWriter
import java.io.Serial
import java.io.StringWriter

internal class ConversionException(cause: Throwable) : Exception(cause) {
    internal val error = errorType

    private companion object {
        @Serial
        private const val serialVersionUID = -6545105434557564843L
    }
}

private inline val Throwable.errorType: YoutubeDLRequestStatus.Error
    get() {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)

        printStackTrace(printWriter)
        val stackTrack = stringWriter.toString()

        return when {
            "Unable to download" in stackTrack -> YoutubeDLRequestStatus.Error.NO_INTERNET
            "Video unavailable" in stackTrack -> YoutubeDLRequestStatus.Error.INCORRECT_URL_LINK
            "Unexpected symbol '.' in numeric literal at path: \$.duration" in stackTrack -> YoutubeDLRequestStatus.Error.STREAM_CONVERSION
            else -> YoutubeDLRequestStatus.Error.UNKNOWN_ERROR
        }
    }