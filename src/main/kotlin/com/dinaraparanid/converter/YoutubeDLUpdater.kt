package com.dinaraparanid.converter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import kotlin.time.toKotlinDuration

private val updaterScope = CoroutineScope(Dispatchers.IO)

@Volatile
internal var isYoutubeDLUpdateTaskStarted = false

internal suspend fun updateYoutubeDLAsync() = updaterScope.launch(Dispatchers.IO) {
    isYoutubeDLUpdateTaskStarted = true

    while (true) {
        Runtime.getRuntime().exec("youtube-dl -U")
        delay(Duration.ofHours(1).toKotlinDuration())
    }
}