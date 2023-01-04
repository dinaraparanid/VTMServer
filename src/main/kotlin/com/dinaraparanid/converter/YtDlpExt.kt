package com.dinaraparanid.converter

import com.dinaraparanid.ytdlp_kt.YtDlp
import kotlinx.coroutines.delay
import java.time.Duration
import kotlin.time.toKotlinDuration

suspend inline fun YtDlp.runUpdateLoop() {
    while (true) {
        updateAsync(isPythonExecutable = false).join()
        delay(Duration.ofDays(1).toKotlinDuration())
    }
}