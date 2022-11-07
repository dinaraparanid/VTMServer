package com.dinaraparanid.converter.ytdlp

import com.dinaraparanid.converter.ConversionException
import com.dinaraparanid.converter.VideoInfo
import com.dinaraparanid.converter.withFileNameWithoutExt
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.time.Duration
import kotlin.time.toKotlinDuration

internal object YtDlp {
    private val executablePath = "${System.getProperty("user.dir")}/vtm_tracks"
    private val updaterScope = CoroutineScope(Dispatchers.IO)
    private val fetcherScope = CoroutineScope(Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    @JvmField
    internal var isYoutubeDLUpdateTaskStarted = false

    private fun buildCommand(command: String) = "yt-dlp $command"

    @JvmStatic
    @JvmName("execute")
    @Throws(YtDlpException::class)
    internal fun execute(request: YtDlpRequest): YtDlpResponse {
        val directory = request.directory
        val options = request.options
        val outBuffer = StringBuffer() //stdout
        val errBuffer = StringBuffer() //stderr

        val startTime = System.nanoTime()
        val command = buildCommand(request.buildOptions())
        val commandArr = java.lang.String(buildCommand(request.buildOptions())).split(" ")

        val processBuilder = ProcessBuilder(*commandArr).also { builder ->
            directory
                ?.let(::File)
                ?.let(builder::directory)
        }

        val process = try {
            processBuilder.start()
        } catch (e: IOException) {
            throw YtDlpException(e)
        }

        val outStream = process.inputStream
        val errStream = process.errorStream

        StreamGobbler(outBuffer, outStream)
        StreamGobbler(errBuffer, errStream)

        val exitCode = try {
            process.waitFor()
        } catch (e: InterruptedException) {
            // process exited for some reason
            throw YtDlpException(e)
        }

        val out = outBuffer.toString()
        val err = errBuffer.toString()

        if (exitCode > 0)
            throw YtDlpException(err)

        val elapsedTime = ((System.nanoTime() - startTime) / 1000000).toInt()
        return YtDlpResponse(command, options, directory, exitCode, elapsedTime, out, err)
    }

    private suspend fun updateAsync() = updaterScope.launch(Dispatchers.IO) {
        isYoutubeDLUpdateTaskStarted = true

        while (true) {
            Runtime.getRuntime().exec("yt-dlp -U")
            delay(Duration.ofHours(1).toKotlinDuration())
        }
    }

    @JvmStatic
    @JvmName("getVideoData")
    internal fun getVideoData(url: String) =
        kotlin.runCatching {
            YtDlpRequest(url)
                .apply {
                    setOption("dump-json")
                    setOption("no-playlist")
                }
                .let(YtDlp::execute)
                .let(YtDlpResponse::out)
                .let<String, VideoInfo>(json::decodeFromString)
                .withFileNameWithoutExt
                .let(YtDlpRequestStatus::Success)
        }.getOrElse { exception ->
            ConversionException(exception).error
        }

    @Throws(YtDlpException::class)
    internal fun getVideoDataAsync(url: String) = fetcherScope.async(Dispatchers.IO) {
        if (!isYoutubeDLUpdateTaskStarted)
            updateAsync()

        getVideoData(url)
    }
}