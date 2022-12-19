package com.dinaraparanid.converter

enum class TrackFileExtension {
    MP3, WAV, AAC, FLAC, M4A, OPUS, VORBIS;

    val extension get() = toString().lowercase()

    companion object {
        private val extensionMap = hashMapOf(
            *TrackFileExtension.values()
                .map { it.extension to it }
                .toTypedArray()
        )

        fun fromString(extension: String) = extensionMap[extension]!!
    }
}