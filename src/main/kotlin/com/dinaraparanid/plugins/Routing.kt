package com.dinaraparanid.plugins

import com.dinaraparanid.data.ConversionStatus
import com.dinaraparanid.data.TrackFileExtension
import com.dinaraparanid.data.convertVideo
import com.dinaraparanid.data.getVideoData
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*

fun Application.configureRouting() {
    install(PartialContent)
    install(AutoHeadResponse)

    routing {
        route("/convert_video") {
            get("{url?}{ext?}") { convertAndSendTrack() }
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.convertAndSendTrack() {
    val url = call.parameters["url"]?.trim()
        ?: return call.respondText("No URL to video provided", status = HttpStatusCode.BadRequest)

    val trackExt = call.parameters["ext"]?.trim()?.let(TrackFileExtension::fromString)
        ?: return call.respondText("No output file extension provided", status = HttpStatusCode.BadRequest)

    call.respond(message = getVideoData(url))

    call.response.header(
        name = HttpHeaders.ContentDisposition,
        value = ContentDisposition
            .Attachment
            .withParameter(ContentDisposition.Parameters.FileName, "$url.${trackExt.extension}")
            .toString()
    )

    call.respondConvertedFileOrError(convertVideo(url, trackExt))
}

private suspend fun ApplicationCall.respondConvertedFileOrError(conversionStatus: ConversionStatus) =
    when (conversionStatus) {
        is ConversionStatus.Success -> respondFile(conversionStatus.file)
        ConversionStatus.Error.NO_INTERNET -> println("WARNING: No Internet Connection")
        ConversionStatus.Error.INCORRECT_URL_LINK -> respondText("Incorrect URL link")
    }