package com.dinaraparanid.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

private const val host = "localhost:3000"

fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowHost(host)
    }
}