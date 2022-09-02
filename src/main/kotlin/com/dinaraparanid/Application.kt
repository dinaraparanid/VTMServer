package com.dinaraparanid

import com.dinaraparanid.plugins.configureRouting
import com.dinaraparanid.plugins.configureSerialization
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory

fun main() {
    embeddedServer(Netty, environment = applicationEngineEnvironment {
        log = LoggerFactory.getLogger("com.dinaraparanid.VTMServer")

        connector {
            host = "0.0.0.0"
            port = 1337
        }

        module {
            configureRouting()
            configureSerialization()
        }
    }).start(wait = true)
}
