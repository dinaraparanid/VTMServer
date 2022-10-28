package com.dinaraparanid.converter

internal sealed interface TrackFileExtension {
    enum class MusicExt : TrackFileExtension {
        MP3, WAV, AAC, FLAC, M4A, OPUS, VORBIS
    }

    enum class VideoExt : TrackFileExtension {
        MP4, FLV, OGG, WEBM, MKV, AVI
    }

    val extension get() = toString().lowercase()

    companion object {
        private val extensionMap = hashMapOf<String, TrackFileExtension>(
            *(TrackFileExtension.MusicExt.values().toList() + TrackFileExtension.VideoExt.values())
                .map { it.extension to it }
                .toTypedArray()
        )

        internal fun fromString(extension: String) = extensionMap[extension]!!
    }
}