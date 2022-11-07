package com.dinaraparanid.plugins

import com.dinaraparanid.converter.*
import com.dinaraparanid.converter.convertVideoAsync
import com.dinaraparanid.converter.ytdlp.YtDlp
import com.dinaraparanid.converter.ytdlp.YtDlpRequestStatus
import com.dinaraparanid.converter.ytdlp.castAndGetData
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

    call.onYoutubeDLRequest<VideoInfo>(YtDlp.getVideoDataAsync(url).await()) { videoInfo ->
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
    val trackCoverUrl = call.parameters["coverUrl"]?.trim()

    call.onYoutubeDLRequest<VideoInfo>(YtDlp.getVideoDataAsync(url).await()) { (title, _, _, fileName, thumbnailURL) ->
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
    status: YtDlpRequestStatus,
    onSuccess: ApplicationCall.(T) -> Unit
) = when (status) {
    is YtDlpRequestStatus.Success<*> -> onSuccess(status.castAndGetData())

    YtDlpRequestStatus.Error.NO_INTERNET -> println("WARNING: No Internet Connection")

    YtDlpRequestStatus.Error.INCORRECT_URL_LINK -> respondText(
        text = "Incorrect URL link",
        status = HttpStatusCode.BadRequest
    )

    YtDlpRequestStatus.Error.UNKNOWN_ERROR -> respondText(
        text = "Unknown error",
        status = HttpStatusCode.InternalServerError
    )

    YtDlpRequestStatus.Error.INVALID_DATA -> respondText(
        text = "Invalid data",
        status = HttpStatusCode.BadRequest
    )

    YtDlpRequestStatus.Error.STREAM_CONVERSION -> respondText(
        text = "Stream conversion is forbidden",
        status = HttpStatusCode.BadRequest
    )

    YtDlpRequestStatus.Error.GEO_RESTRICTED -> respondText(
        text = "Sorry, this video is geo-restricted",
        status = HttpStatusCode.Locked
    )
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