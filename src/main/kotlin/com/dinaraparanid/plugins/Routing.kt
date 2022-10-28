package com.dinaraparanid.plugins

import com.dinaraparanid.converter.*
import com.dinaraparanid.converter.YoutubeDLRequestStatus
import com.dinaraparanid.converter.convertVideoAsync
import com.dinaraparanid.converter.getVideoDataAsync
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
        route("/get_video") {
            get("{url?}") { respondVideData() }
        }

        route("/convert_video") {
            get("{url?}{ext?}") { convertAndRespondTrackFile() }
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.respondVideData() {
    val url = call.parameters["url"]?.trim()
        ?: return call.respondText("No URL to video provided", status = HttpStatusCode.BadRequest)

    call.onYoutubeDLRequest<VideoInfo>(getVideoDataAsync(url).await()) { videoInfo ->
        respondVideoInfo(videoInfo)
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.convertAndRespondTrackFile() {
    val url = call.parameters["url"]?.trim()
        ?: return call.respondText("No URL to video provided", status = HttpStatusCode.BadRequest)

    val trackExt = call.parameters["ext"]?.trim()?.let(TrackFileExtension::fromString)
        ?: return call.respondText("No output file extension provided", status = HttpStatusCode.BadRequest)

    call.onYoutubeDLRequest<VideoInfo>(getVideoDataAsync(url).await()) { videoInfo ->
        val videoFile = File(videoInfo.fileName)

        convertAndRespondVideoFile(
            videoFileNameWithoutExtension = videoFile.nameWithoutExtension,
            videoTitle = videoInfo.title,
            videoThumbnail = videoInfo.thumbnailURL,
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
    videoFileNameWithoutExtension: String,
    videoTitle: String,
    videoThumbnail: String,
    url: String,
    trackExt: TrackFileExtension
) = onYoutubeDLRequest<File>(
    convertVideoAsync(url, trackExt, videoFileNameWithoutExtension, videoTitle, videoThumbnail).await()
) { convertedFile ->
    response.header(
        name = HttpHeaders.ContentDisposition,
        value = ContentDisposition
            .Attachment
            .withParameter(
                ContentDisposition.Parameters.FileName,
                "$videoTitle.${trackExt.extension}"
            )
            .toString()
    )

    respondFile(convertedFile)
}