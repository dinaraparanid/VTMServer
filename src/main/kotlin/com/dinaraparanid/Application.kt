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
    configureRouting()
    configureTemplating()
}

fun main() = runBlocking {
    launch(Dispatchers.IO) { YtDlp.runUpdateLoop() }
    embeddedServer(Netty, 1337) { configureModules() }.start(wait = true)
    Unit
}
