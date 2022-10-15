package com.dinaraparanid.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        route("/convert_video") {
            get("{url?}{ext?}") {
                val url = call.parameters["url"] ?: return@get call.respondText(
                    "No URL to video provided",
                    status = HttpStatusCode.BadRequest
                )

                val ext = call.parameters["ext"] ?: return@get call.respondText(
                    "No output file extension provided",
                    status = HttpStatusCode.BadRequest
                )

                println("output file: $url.$ext")
                call.respondText("Your URL: $url, output file: $url.$ext")
            }
        }
    }
}
