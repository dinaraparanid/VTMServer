package com.paranid5.vtmserver.plugins

import arrow.core.Either
import com.paranid5.vtmserver.auth.firebase.FirebaseAuthProvider
import com.paranid5.vtmserver.data.VideoInfo
import com.paranid5.vtmserver.data.serializableInfo
import com.paranid5.vtmserver.domain.*
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
        get("/get_video{url?}") {
            respondVideoData()
        }

        get("/convert_video{url?}{ext?}") {
            convertAndRespondTrackFile(isAuthorized = false)
        }

        authenticate(FirebaseAuthProvider.FIREBASE_AUTH) {
            get("/convert_video_auth{url?}{ext?}{title?}{artist?}{album?}{numberInAlbum?}{coverUrl?}") {
                convertAndRespondTrackFile(isAuthorized = true)
            }
        }
    }
}

private suspend inline fun PipelineContext<Unit, ApplicationCall>.respondVideoData() {
    val url = call.parameters["url"]?.trim()
        ?: return call.respondText("No URL to video provided", status = HttpStatusCode.BadRequest)

    call.onYTRequest(
        status = getVideoInfoAsync(url).await().map { it.details().serializableInfo }
    ) { videoInfo -> respondVideoInfo(videoInfo) }
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

    val trackAlbum = getIfAuthorizedOrDefault(isAuthorized, default = "") {
        call.parameters["album"]?.trim()
    }

    val trackNumberInAlbum = getIfAuthorizedOrDefault(isAuthorized, default = -1) {
        call.parameters["numberInAlbum"]?.trim()?.toInt()
    }

    val trackCoverUrl = getIfAuthorizedOrDefaultNullable(isAuthorized, default = null) {
        call.parameters["coverUrl"]?.trim()
    }

    call.onYTRequest(status = getVideoInfoAsync(url).await()) { info ->
        convertAndRespondVideoFile(
            url = url,
            trackExt = trackExt,
            trackTitle = trackTitle.takeIf(String::isNotEmpty) ?: info.details().title().trim(),
            trackArtist = trackArtist.takeIf(String::isNotEmpty) ?: info.details().author().trim(),
            trackAlbum = trackAlbum,
            trackNumberInAlbum = trackNumberInAlbum,
            videoThumbnailURL = info.details().thumbnails().last(),
            trackCoverUrl = trackCoverUrl
        )
    }
}

private suspend inline fun <T> ApplicationCall.onYTRequest(
    status: Either<YTError, T>,
    onSuccess: ApplicationCall.(T) -> Unit
) = when (status) {
    is Either.Right<*> -> onSuccess(status.value as T)

    is Either.Left<*> -> when (status.value as YTError) {
        YTError.NOT_MATCH_REGEX -> respondText(
            text = "Incorrect URL link",
            status = HttpStatusCode.BadRequest
        )

        YTError.FILE_CONVERSION_ERROR -> respondText(
            text = "File conversion error",
            status = HttpStatusCode.BadRequest
        )

        YTError.UNKNOWN_ERROR -> respondText(
            text = "Unknown Error",
            status = HttpStatusCode.InternalServerError
        )
    }
}

private suspend inline fun ApplicationCall.respondVideoInfo(videoInfo: VideoInfo) = respond(message = videoInfo)

private suspend inline fun ApplicationCall.convertAndRespondVideoFile(
    url: String,
    trackExt: TrackFileExtension,
    trackTitle: String,
    trackArtist: String,
    trackAlbum: String,
    trackNumberInAlbum: Int,
    videoThumbnailURL: String,
    trackCoverUrl: String? = null,
) = onYTRequest(
    convertVideoAsync(
        url,
        trackExt,
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