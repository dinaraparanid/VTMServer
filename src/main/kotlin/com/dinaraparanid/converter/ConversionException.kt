package com.dinaraparanid.converter

import com.dinaraparanid.converter.ytdlp.YtDlpRequestStatus
import java.io.PrintWriter
import java.io.Serial
import java.io.StringWriter

internal class ConversionException(cause: Throwable) : Exception(cause) {
    @JvmField
    internal val error = errorType

    private companion object {
        @Serial
        private const val serialVersionUID = -6545105434557564843L
    }
}

private inline val Throwable.errorType: YtDlpRequestStatus.Error
    get() {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)

        printStackTrace()
        printStackTrace(printWriter)
        val stackTrack = stringWriter.toString()

        return when {
            "Unable to download" in stackTrack -> YtDlpRequestStatus.Error.NO_INTERNET

            "is not a valid URL" in stackTrack ->
                YtDlpRequestStatus.Error.INCORRECT_URL_LINK

            "video available in your country" in stackTrack ->
                YtDlpRequestStatus.Error.GEO_RESTRICTED

            "Unexpected symbol '.' in numeric literal at path: \$.duration" in stackTrack ->
                YtDlpRequestStatus.Error.STREAM_CONVERSION

            else -> YtDlpRequestStatus.Error.UNKNOWN_ERROR
        }
    }