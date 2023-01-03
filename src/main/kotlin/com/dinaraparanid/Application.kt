package com.dinaraparanid

import com.dinaraparanid.converter.runUpdateLoop
import com.dinaraparanid.plugins.*
import com.dinaraparanid.plugins.configureRouting
import com.dinaraparanid.ytdlp_kt.YtDlp
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun Application.configureModules() {
    configureSerialization()
    configureCORS()
    configureCompression()
    configureAuthentication()
    configureRouting()
}

fun main() = runBlocking {
    Runtime.getRuntime().exec("unzip -l yt-dlp").waitFor()
    launch(Dispatchers.IO) { YtDlp.runUpdateLoop() }

    embeddedServer(
        factory = Netty,
        port = 1337,
        module = Application::ApplicationModule
    ).start(wait = true)
    Unit
}

fun Application.ApplicationModule() = configureModules()
