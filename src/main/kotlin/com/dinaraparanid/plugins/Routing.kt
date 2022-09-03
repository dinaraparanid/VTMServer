package com.dinaraparanid.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        route("/convert_video") {
            get("{url?}{file_name?}{ext?}") {
                val url = call.parameters["url"] ?: return@get call.respondText(
                    "No URL to video provided",
                    status = HttpStatusCode.BadRequest
                )

                val fileName = call.parameters["file_name"] ?: return@get call.respondText(
                    "No output file name provided",
                    status = HttpStatusCode.BadRequest
                )

                val ext = call.parameters["ext"] ?: return@get call.respondText(
                    "No output file extension provided",
                    status = HttpStatusCode.BadRequest
                )

                call.respondText("Your URL: $url, output file: $fileName.$ext")
            }
        }
    }
}
