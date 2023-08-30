package com.paranid5.vtmserver.domain

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
import kotlinx.coroutines.*

private val CONVERTED_TRACKS_PATH by lazy {
    "${System.getProperty("user.dir")}/vtm_tracks"
}

private val converterScope = CoroutineScope(Dispatchers.IO)

fun convertVideoAsync(
    url: String,
    ext: TrackFileExtension,
    trackTitle: String,
    trackArtist: String,
    trackAlbum: String,
    trackNumberInAlbum: Int,
    videoThumbnailURL: String,
    coverUrl: String? = null,
) = converterScope.async(Dispatchers.IO) {
    val coverPath = "$CONVERTED_TRACKS_PATH/${trackTitle}_cover.png"
    val storeThumbnailTask = storeThumbnailAsync(coverUrl ?: videoThumbnailURL, coverPath)

    downloadVideoAsync(url).await().map { trackFile ->
        // TODO: Convert with ffmpeg

        setTags(
            trackFile = trackFile,
            trackTitle = trackTitle,
            trackArtist = trackArtist,
            trackAlbum = trackAlbum,
            trackNumberInAlbum = trackNumberInAlbum,
            coverPath = coverPath,
            storeThumbnailTask = storeThumbnailTask
        )

        removeFilesAfterTimeoutAsync(trackFile = trackFile, coverFile = File(coverPath))
        trackFile
    }
}

private fun storeThumbnailAsync(coverUrl: String, coverPath: String) = converterScope.launch(Dispatchers.IO) {
    val coverData = ByteArrayOutputStream().use {
        val coverData = URL(coverUrl).readBytes()
        it.write(coverData)
        it.toByteArray()
    }

    FileOutputStream(coverPath).use { it.write(coverData) }
}

private suspend inline fun setTags(
    trackFile: File,
    trackTitle: String,
    trackArtist: String,
    trackAlbum: String,
    trackNumberInAlbum: Int,
    coverPath: String,
    storeThumbnailTask: Job,
) = AudioFileIO.read(trackFile).run {
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

@Suppress("DirectUseOfResultType")
private fun Tag.setCoverCatching(coverPath: String) = kotlin.runCatching {
    deleteArtworkField()
    setField(ArtworkFactory.createArtworkFromFile(File(coverPath)))
}

private fun removeFilesAfterTimeoutAsync(
    trackFile: File,
    coverFile: File
) = converterScope.launch(Dispatchers.IO) {
    coverFile.delete()
    delay(Duration.ofDays(1).toKotlinDuration())
    trackFile.delete()
}