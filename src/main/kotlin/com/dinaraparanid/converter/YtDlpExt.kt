package com.dinaraparanid.converter

import com.dinaraparanid.ytdlp_kt.YtDlp
import kotlinx.coroutines.delay
import java.time.Duration
import kotlin.time.toKotlinDuration

internal suspend fun YtDlp.runUpdateLoop() {
    while (true) {
        updateAsync().join()
        delay(Duration.ofHours(1).toKotlinDuration())
    }
}