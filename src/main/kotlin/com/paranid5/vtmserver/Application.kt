package com.paranid5.vtmserver

import com.paranid5.vtmserver.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking

fun Application.configureModules() {
    configureSerialization()
    configureCORS()
    configureCompression()
    configureAuthentication()
    configureRouting()
}

fun main() = runBlocking {
    embeddedServer(factory = Netty, port = 8080, module = Application::ApplicationModule).start(wait = true)
    Unit
}

fun Application.ApplicationModule() = configureModules()
