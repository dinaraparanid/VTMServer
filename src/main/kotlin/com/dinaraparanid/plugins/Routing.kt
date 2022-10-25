package com.dinaraparanid.plugins

import com.dinaraparanid.data.*
import com.dinaraparanid.data.YoutubeDLRequestStatus
import com.dinaraparanid.data.convertVideo
import com.dinaraparanid.data.getVideoData
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import java.io.File

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

    call.onYoutubeDLRequest<VideoInfo>(getVideoData(url)) { videoInfo ->
        respondVideoInfo(videoInfo)

        convertAndRespondVideoFile(
            videoTitle = videoInfo.title,
            fileName = videoInfo.fileName,
            url = url,
            trackExt = trackExt
        )
    }
}

private suspend inline fun <T> ApplicationCall.onYoutubeDLRequest(
    status: YoutubeDLRequestStatus,
    onSuccess: ApplicationCall.(T) -> Unit
) = when (status) {
    is YoutubeDLRequestStatus.Success<*> -> onSuccess(status.castAndGetData())
    YoutubeDLRequestStatus.Error.NO_INTERNET -> println("WARNING: No Internet Connection")
    YoutubeDLRequestStatus.Error.INCORRECT_URL_LINK -> respondText("Incorrect URL link")
    YoutubeDLRequestStatus.Error.UNKNOWN_ERROR -> respondText("Unknown error")
    YoutubeDLRequestStatus.Error.INVALID_DATA -> respondText("Invalid data")
}

private suspend fun ApplicationCall.respondVideoInfo(videoInfo: VideoInfo) = respond(message = videoInfo)

private suspend fun ApplicationCall.convertAndRespondVideoFile(
    videoTitle: String,
    fileName: String,
    url: String,
    trackExt: TrackFileExtension
) {
    response.header(
        name = HttpHeaders.ContentDisposition,
        value = ContentDisposition
            .Attachment
            .withParameter(ContentDisposition.Parameters.FileName, "$fileName.${trackExt.extension}")
            .toString()
    )

    onYoutubeDLRequest<File>(convertVideo(url, trackExt, videoTitle)) { respondFile(it) }
}