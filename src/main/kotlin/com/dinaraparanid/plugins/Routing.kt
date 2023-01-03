package com.dinaraparanid.plugins

import com.dinaraparanid.auth.firebase.FirebaseAuthProvider
import com.dinaraparanid.converter.TrackFileExtension
import com.dinaraparanid.converter.convertVideoAsync
import com.dinaraparanid.ytdlp_kt.VideoInfo
import com.dinaraparanid.ytdlp_kt.YtDlp
import com.dinaraparanid.ytdlp_kt.YtDlpRequestStatus
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
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
            respondVideoData()
        }

        get("/convert_video/{url?}{ext?}") {
            convertAndRespondTrackFile(isAuthorized = false)
        }

        authenticate(FirebaseAuthProvider.FIREBASE_AUTH) {
            get("/convert_video_auth/{url?}{ext?}{title?}{artist?}{album?}{numberInAlbum?}{coverUrl?}") {
                convertAndRespondTrackFile(isAuthorized = true)
            }
        }
    }
}

private suspend inline fun PipelineContext<Unit, ApplicationCall>.respondVideoData() {
    val url = call.parameters["url"]?.trim()
        ?: return call.respondText("No URL to video provided", status = HttpStatusCode.BadRequest)

    call.onYoutubeDLRequest<VideoInfo>(YtDlp.getVideoDataAsync(url, isPythonExecutable = true).await()) { videoInfo ->
        respondVideoInfo(videoInfo)
    }
}

private inline fun <T> getIfAuthorizedOrDefault(isAuthorized: Boolean, default: T, getData: () -> T?) =
    if (isAuthorized) getData() ?: default else default

private inline fun <T> getIfAuthorizedOrDefaultNullable(isAuthorized: Boolean, default: T?, getData: () -> T?) =
    if (isAuthorized) getData() else default

private suspend inline fun PipelineContext<Unit, ApplicationCall>.convertAndRespondTrackFile(isAuthorized: Boolean) {
    val url = call.parameters["url"]?.trim()
        ?: return call.respondText("No URL to video provided", status = HttpStatusCode.BadRequest)

    val trackExt = call.parameters["ext"]?.trim()?.let(TrackFileExtension::fromString)
        ?: return call.respondText("No output file extension provided", status = HttpStatusCode.BadRequest)

    val trackTitle = getIfAuthorizedOrDefault(isAuthorized, default = "") {
        call.parameters["title"]?.trim()
    }

    val trackArtist = getIfAuthorizedOrDefault(isAuthorized, default = "") {
        call.parameters["artist"]?.trim()
    }

    val trackAlbum = getIfAuthorizedOrDefault(isAuthorized, default = "") { call.parameters["album"]?.trim() }

    val trackNumberInAlbum = getIfAuthorizedOrDefault(isAuthorized, default = -1) {
        call.parameters["numberInAlbum"]?.trim()?.toInt()
    }

    val trackCoverUrl = getIfAuthorizedOrDefaultNullable(isAuthorized, default = null) {
        call.parameters["coverUrl"]?.trim()
    }

    call.onYoutubeDLRequest<VideoInfo>(
        YtDlp.getVideoDataAsync(url, isPythonExecutable = true).await()
    ) { (title, _, _, fileName, thumbnailURL) ->
        convertAndRespondVideoFile(
            url = url,
            trackExt = trackExt,
            videoFileNameWithoutExtension = fileName,
            trackTitle = trackTitle.takeIf(String::isNotEmpty) ?: title,
            trackArtist = trackArtist,
            trackAlbum = trackAlbum,
            trackNumberInAlbum = trackNumberInAlbum,
            videoThumbnailURL = thumbnailURL,
            trackCoverUrl = trackCoverUrl
        )
    }
}

private suspend inline fun <T> ApplicationCall.onYoutubeDLRequest(
    status: YtDlpRequestStatus,
    onSuccess: ApplicationCall.(T) -> Unit
) = when (status) {
    is YtDlpRequestStatus.Success<*> -> onSuccess(status.castAndGetData())

    is YtDlpRequestStatus.Error.NoInternet -> println("WARNING: No Internet Connection")

    is YtDlpRequestStatus.Error.IncorrectUrl -> respondText(
        text = "Incorrect URL link",
        status = HttpStatusCode.BadRequest
    )

    is YtDlpRequestStatus.Error.UnknownError -> respondText(
        text = "Unknown error",
        status = HttpStatusCode.InternalServerError
    )

    is YtDlpRequestStatus.Error.StreamConversion -> respondText(
        text = "Stream conversion is forbidden",
        status = HttpStatusCode.BadRequest
    )

    is YtDlpRequestStatus.Error.GeoRestricted -> respondText(
        text = "Sorry, this video is geo-restricted",
        status = HttpStatusCode.Locked
    )
}

private suspend inline fun ApplicationCall.respondVideoInfo(videoInfo: VideoInfo) = respond(message = videoInfo)

private suspend inline fun ApplicationCall.convertAndRespondVideoFile(
    url: String,
    trackExt: TrackFileExtension,
    videoFileNameWithoutExtension: String,
    trackTitle: String,
    trackArtist: String,
    trackAlbum: String,
    trackNumberInAlbum: Int,
    videoThumbnailURL: String,
    trackCoverUrl: String? = null,
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
        trackCoverUrl
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