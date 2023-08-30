package com.paranid5.vtmserver.domain

import arrow.core.Either
import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo
import com.github.kiulian.downloader.downloader.response.ResponseStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.File

enum class YTError {
    NOT_MATCH_REGEX, UNKNOWN_ERROR
}

private val CONVERTED_TRACKS_PATH by lazy {
    "${System.getProperty("user.dir")}/vtm_tracks"
}

private val downloader by lazy {
    YoutubeDownloader()
}

private val youtubeUrlRegex by lazy {
    Regex("https://(www\\.youtube\\.com/watch\\?v=|youtu\\.be/)(\\S{11})(\\?si=\\S{16})?")
}

internal suspend inline fun getVideoInfoAsync(url: String) = coroutineScope {
    async(Dispatchers.IO) {
        when (val match = youtubeUrlRegex.find(url)) {
            null -> Either.Left(YTError.NOT_MATCH_REGEX)

            else -> {
                val videoId = match.groupValues[2]

                Either.Right(
                    downloader
                        .getVideoInfo(RequestVideoInfo(videoId))
                        .data()
                )
            }
        }
    }
}

internal suspend inline fun downloadVideoAsync(url: String) = coroutineScope {
    async(Dispatchers.IO) {
        val response = YoutubeDownloader().downloadVideoFile(
            RequestVideoFileDownload(getVideoInfoAsync(url).await().getOrNull()!!.bestAudioFormat())
                .saveTo(File(CONVERTED_TRACKS_PATH))
        )

        when (val status = response.status()) {
            ResponseStatus.completed -> Either.Right(response.data())

            ResponseStatus.error -> {
                response.error().printStackTrace()
                Either.Left(YTError.NOT_MATCH_REGEX)
            }

            else -> {
                println("Unknown status: $status")
                Either.Left(YTError.UNKNOWN_ERROR)
            }
        }
    }
}