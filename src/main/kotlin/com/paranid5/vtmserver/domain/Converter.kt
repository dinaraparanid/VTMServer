package com.paranid5.vtmserver.domain

import arrow.core.Either
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
        val convertedFile = when (ext) {
            TrackFileExtension.MP3 -> trackFile.convertToMP3Async()
            TrackFileExtension.WAV -> trackFile.convertToWAVAsync()
            TrackFileExtension.AAC -> trackFile.convertToAACAsync()
            TrackFileExtension.FLAC -> trackFile.convertToFLACAsync()
            TrackFileExtension.M4A -> trackFile.convertToM4AAsync()
            TrackFileExtension.OPUS -> trackFile.convertToOPUSAsync()
            TrackFileExtension.VORBIS -> trackFile.convertToVORBISAsync()
        }.await() ?: return@async Either.Left(YTError.FILE_CONVERSION_ERROR)

        trackFile.delete()

        setTags(
            trackFile = convertedFile,
            trackTitle = trackTitle,
            trackArtist = trackArtist,
            trackAlbum = trackAlbum,
            trackNumberInAlbum = trackNumberInAlbum,
            coverPath = coverPath,
            storeThumbnailTask = storeThumbnailTask
        )

        removeFilesAfterTimeoutAsync(trackFile = convertedFile, coverFile = File(coverPath))
        convertedFile
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

/**
 * Converts video file to an audio file with ffmpeg
 * @param ext audio file format
 * @param ffmpegCmd ffmpeg cmd command to execute
 * @return file if conversion was successful, otherwise null
 */

private suspend inline fun File.convertToAudioFileAsync(
    ext: TrackFileExtension,
    crossinline ffmpegCmd: (File) -> String
) = coroutineScope {
    async(Dispatchers.IO) {
        val extension = when (ext) {
            TrackFileExtension.MP3 -> "mp3"
            TrackFileExtension.WAV -> "wav"
            TrackFileExtension.AAC -> "aac"
            TrackFileExtension.FLAC -> "flac"
            TrackFileExtension.M4A -> "m4a"
            TrackFileExtension.OPUS -> "opus"
            TrackFileExtension.VORBIS -> "vorbis"
        }

        val newFile = File("$CONVERTED_TRACKS_PATH/$nameWithoutExtension.$extension").apply {
            println(absolutePath)
        }

        val process = Runtime.getRuntime().exec(ffmpegCmd(newFile)).apply {
            println("FFMPEG:")
            errorStream.bufferedReader().lineSequence().forEach(::println)
        }

        when (process.waitFor()) {
            0 -> newFile
            else -> null
        }
    }
}

/**
 * Converts video file to .mp3 audio file with ffmpeg
 * @return .mp3 file if conversion was successful, otherwise null
 */

private suspend inline fun File.convertToMP3Async() =
    convertToAudioFileAsync(TrackFileExtension.MP3) { newFile ->
        "ffmpeg -y -i $absolutePath -vn -acodec libmp3lame -qscale:a 2 ${newFile.absolutePath}"
            .also(::println)
    }

/**
 * Converts video file to .wav audio file with ffmpeg
 * @return .wav file if conversion was successful, otherwise null
 */

private suspend inline fun File.convertToWAVAsync() =
    convertToAudioFileAsync(TrackFileExtension.WAV) { newFile ->
        "ffmpeg -y -i $absolutePath -vn -acodec pcm_s16le -ar 44100 ${newFile.absolutePath}"
    }

/**
 * Converts video file to .aac audio file with ffmpeg
 * @return .aac file if conversion was successful, otherwise null
 */

private suspend inline fun File.convertToAACAsync() =
    convertToAudioFileAsync(TrackFileExtension.AAC) { newFile ->
        "ffmpeg -y -i $absolutePath -vn -c:a aac -b:a 256k ${newFile.absolutePath}"
    }

/**
 * Converts video file to .flac audio file with ffmpeg
 * @return .flac file if conversion was successful, otherwise null
 */

private suspend inline fun File.convertToFLACAsync() =
    convertToAudioFileAsync(TrackFileExtension.FLAC) { newFile ->
        "ffmpeg -i $absolutePath -c:a flac ${newFile.absolutePath}"
    }

/**
 * Converts video file to .m4a audio file with ffmpeg
 * @return .m4a file if conversion was successful, otherwise null
 */

private suspend inline fun File.convertToM4AAsync() =
    convertToAudioFileAsync(TrackFileExtension.M4A) { newFile ->
        "ffmpeg -i $absolutePath -c:a aac ${newFile.absolutePath}"
    }

/**
 * Converts video file to .opus audio file with ffmpeg
 * @return .opus file if conversion was successful, otherwise null
 */

private suspend inline fun File.convertToOPUSAsync() =
    convertToAudioFileAsync(TrackFileExtension.OPUS) { newFile ->
        "ffmpeg -i $absolutePath -c:a libopus -strict experimental ${newFile.absolutePath}"
    }

/**
 * Converts video file to .vorbis audio file with ffmpeg
 * @return .vorbis file if conversion was successful, otherwise null
 */

private suspend inline fun File.convertToVORBISAsync() =
    convertToAudioFileAsync(TrackFileExtension.VORBIS) { newFile ->
        "ffmpeg -i $absolutePath -c:a libvorbis ${newFile.absolutePath}"
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