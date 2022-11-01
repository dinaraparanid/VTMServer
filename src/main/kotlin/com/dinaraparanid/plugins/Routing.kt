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
        get("/get_video/{url?}") {
            respondVideData()
        }

        get("/convert_video/{url?}{ext?}{title?}{artist?}{album?}{numberInAlbum?}{coverUrl?}") {
            convertAndRespondTrackFile()
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

    val trackTitle = call.parameters["title"]?.trim() ?: ""
    val trackArtist = call.parameters["artist"]?.trim() ?: ""
    val trackAlbum = call.parameters["album"]?.trim() ?: ""
    val trackNumberInAlbum = call.parameters["numberInAlbum"]?.trim()?.toInt() ?: -1
    val trackCoverUrl = call.parameters["coverUrl"]?.trim() ?: ""

    call.onYoutubeDLRequest<VideoInfo>(getVideoDataAsync(url).await()) { (title, _, _, fileName, thumbnailURL) ->
        convertAndRespondVideoFile(
            url,
            trackExt,
            videoFileNameWithoutExtension = fileName,
            trackTitle.takeIf(String::isNotEmpty) ?: title,
            trackArtist,
            trackAlbum,
            trackNumberInAlbum,
            videoThumbnailURL = thumbnailURL,
            trackCoverUrl
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
    YoutubeDLRequestStatus.Error.STREAM_CONVERSION -> respondText("Stream conversion is forbidden")
}

private suspend fun ApplicationCall.respondVideoInfo(videoInfo: VideoInfo) = respond(message = videoInfo)

private suspend fun ApplicationCall.convertAndRespondVideoFile(
    url: String,
    trackExt: TrackFileExtension,
    videoFileNameWithoutExtension: String,
    trackTitle: String,
    trackArtist: String,
    trackAlbum: String,
    trackNumberInAlbum: Int,
    videoThumbnailURL: String,
    coverUrl: String? = null,
) = onYoutubeDLRequest<File>(
    convertVideoAsync(
        url,
        trackExt,
        videoFileNameWithoutExtension,
        trackTitle,
        trackArtist,
        trackAlbum,
        trackNumberInAlbum,
        videoThumbnailURL,
        coverUrl
    ).await()
) { convertedFile ->
    response.header(
        name = HttpHeaders.ContentDisposition,
        value = ContentDisposition
            .Attachment
            .withParameter(
                ContentDisposition.Parameters.FileName,
                "$trackTitle.${trackExt.extension}"
            )
            .toString()
    )

    respondFile(convertedFile)
}