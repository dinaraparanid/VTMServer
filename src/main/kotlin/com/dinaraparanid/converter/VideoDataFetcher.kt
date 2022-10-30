package com.dinaraparanid.converter

import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val fetcherScope = CoroutineScope(Dispatchers.IO)

private val json = Json { ignoreUnknownKeys = true }

internal suspend fun getVideoDataAsync(url: String) =
    fetcherScope.async(Dispatchers.IO) {
        if (!isYoutubeDLUpdateTaskStarted)
            updateYoutubeDLAsync()

        val request = YoutubeDLRequest(url).apply {
            setOption("dump-json")
            setOption("no-playlist")
        }

        kotlin.runCatching {
            YoutubeDLRequestStatus.Success(
                json
                    .decodeFromString<VideoInfo>(YoutubeDL.execute(request).out)
                    .withFileNameWithoutExt
            )
        }.getOrElse { exception ->
            ConversionException(exception).error
        }
    }