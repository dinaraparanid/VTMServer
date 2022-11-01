package com.dinaraparanid.converter

import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.YoutubeDLRequest
import kotlinx.coroutines.*
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.time.Duration
import kotlin.time.toKotlinDuration

private val CONVERTED_TRACKS_PATH = "${System.getProperty("user.dir")}/vtm_tracks"

private val converterScope = CoroutineScope(Dispatchers.IO)

@Volatile
private var downloadTries = 10

@Volatile
private var isDownloaded = false

@Volatile
private var downloadError: YoutubeDLRequestStatus.Error? = null

internal fun convertVideoAsync(
    url: String,
    ext: TrackFileExtension,
    videoFileNameWithoutExt: String,
    trackTitle: String,
    trackArtist: String,
    trackAlbum: String,
    trackNumberInAlbum: Int,
    videoThumbnailURL: String,
    coverUrl: String? = null,
) = converterScope.async(Dispatchers.IO) {
    val coverPath = "$CONVERTED_TRACKS_PATH/${videoFileNameWithoutExt}_cover.png"
    val storeThumbnailTask = storeThumbnailAsync(coverUrl ?: videoThumbnailURL, coverPath)

    val request = YoutubeDLRequest(url, CONVERTED_TRACKS_PATH).apply {
        setOption("audio-format", ext.extension)
        setOption("socket-timeout", 1)
        setOption("retries", "infinite")
        setOption("extract-audio")
        setOption("format", "best")
    }

    isDownloaded = false
    downloadTries = 10
    downloadError = null

    while (!isDownloaded && downloadTries > 0)
        kotlin.runCatching {
            YoutubeDL.execute(request)
            isDownloaded = true
            return@runCatching
        }.getOrElse { exception ->
            downloadTries--
            downloadError = ConversionException(exception).error
        }

    if (!isDownloaded)
        return@async downloadError!!

    return@async getFileOrError(
        ext,
        videoFileNameWithoutExt,
        trackTitle,
        trackArtist,
        trackAlbum,
        trackNumberInAlbum,
        coverPath,
        storeThumbnailTask
    )
}

private fun storeThumbnailAsync(coverUrl: String, coverPath: String) = converterScope.launch(Dispatchers.IO) {
    val coverData = ByteArrayOutputStream().use {
        val coverData = URL(coverUrl).readBytes()
        it.write(coverData)
        it.toByteArray()
    }

    FileOutputStream(coverPath).use { it.write(coverData) }
}

@Suppress("DirectUseOfResultType")
private fun Tag.setCoverCatching(coverPath: String) = kotlin.runCatching {
    deleteArtworkField()
    setField(ArtworkFactory.createArtworkFromFile(File(coverPath)))
}

private suspend fun setTags(
    trackFile: File,
    trackTitle: String,
    trackArtist: String,
    trackAlbum: String,
    trackNumberInAlbum: Int,
    coverPath: String,
    storeThumbnailTask: Job,
) =
    AudioFileIO.read(trackFile).run {
        tagOrCreateAndSetDefault?.run {
            setField(FieldKey.TITLE, trackTitle)
            setField(FieldKey.ARTIST, trackArtist)
            setField(FieldKey.ALBUM, trackAlbum)
            setField(FieldKey.TRACK, trackNumberInAlbum.toString())
            storeThumbnailTask.join()
            setCoverCatching(coverPath)
        }
        commit()
    }

private fun removeFilesAfterTimeoutAsync(
    trackFile: File,
    coverFile: File
) = converterScope.launch(Dispatchers.IO) {
    coverFile.delete()
    delay(Duration.ofDays(1).toKotlinDuration())
    trackFile.delete()
}

private suspend fun getFileOrError(
    ext: TrackFileExtension,
    videoFileNameWithoutExt: String,
    trackTitle: String,
    trackArtist: String,
    trackAlbum: String,
    trackNumberInAlbum: Int,
    coverPath: String,
    storeThumbnailTask: Job,
): YoutubeDLRequestStatus {
    val trackFile = File("$CONVERTED_TRACKS_PATH/$videoFileNameWithoutExt.${ext.extension}")
    setTags(trackFile, trackTitle, trackArtist, trackAlbum, trackNumberInAlbum, coverPath, storeThumbnailTask)
    removeFilesAfterTimeoutAsync(trackFile, coverFile = File(coverPath))
    return YoutubeDLRequestStatus.Success(trackFile)
}