package com.dinaraparanid

import com.dinaraparanid.plugins.*
import com.dinaraparanid.plugins.configureRouting
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun Application.configureModules() {
    configureSerialization()
    configureCORS()
    configureCompression()
    configureRouting()
    configureTemplating()
}

fun main() {
    embeddedServer(Netty, 1337) {
        configureModules()
    }.start(wait = true)
}
