package com.dinaraparanid.plugins

import com.dinaraparanid.auth.firebase.FirebaseAuthProvider
import com.dinaraparanid.converter.*
import com.dinaraparanid.converter.convertVideoAsync
import com.dinaraparanid.ytdlp_kt.VideoInfo
import com.dinaraparanid.ytdlp_kt.YtDlp
import com.dinaraparanid.ytdlp_kt.YtDlpRequestStatus
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.request.*
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

        route("/convert_video") {
            authenticate(FirebaseAuthProvider.FIREBASE_AUTH) {
                get("/{url?}{ext?}{title?}{artist?}{album?}{numberInAlbum?}{coverUrl?}") {
                    convertAndRespondTrackFile(isAuthorized = true)
                }
            }

            get("/{url?}{ext?}") {
                convertAndRespondTrackFile(isAuthorized = false)
            }
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.respondVideoData() {
    val url = call.parameters["url"]?.trim()
        ?: return call.respondText("No URL to video provided", status = HttpStatusCode.BadRequest)

    call.onYoutubeDLRequest<VideoInfo>(YtDlp.getVideoDataAsync(url).await()) { videoInfo ->
        respondVideoInfo(videoInfo)
    }
}

private inline fun <T> getIfAuthorizedOrDefault(isAuthorized: Boolean, default: T, getData: () -> T?) =
    if (isAuthorized) getData() ?: default else default

@Suppress("IncorrectFormatting")
private suspend fun PipelineContext<Unit, ApplicationCall>.convertAndRespondTrackFile(isAuthorized: Boolean) {
    val url = call.parameters["url"]?.trim()
        ?: return call.respondText("No URL to video provided", status = HttpStatusCode.BadRequest)

    val trackExt = call.parameters["ext"]?.trim()?.let(TrackFileExtension::fromString)
        ?: return call.respondText("No output file extension provided", status = HttpStatusCode.BadRequest)

    val trackTitle = getIfAuthorizedOrDefault(isAuthorized, default = "") { call.parameters["title"]?.trim() }
    val trackArtist = getIfAuthorizedOrDefault(isAuthorized, default = "") { call.parameters["artist"]?.trim() }
    val trackAlbum = getIfAuthorizedOrDefault(isAuthorized, default = "") { call.parameters["album"]?.trim() }
    val trackNumberInAlbum =
        getIfAuthorizedOrDefault(isAuthorized, default = -1) { call.parameters["numberInAlbum"]?.trim()?.toInt() }
    val trackCoverUrl = if (isAuthorized) call.parameters["coverUrl"]?.trim() else null

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