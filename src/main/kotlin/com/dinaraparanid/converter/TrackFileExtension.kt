package com.dinaraparanid.converter

internal enum class TrackFileExtension {
    MP3, WAV, AAC, FLAC, M4A, OPUS, VORBIS;

    internal val extension get() = toString().lowercase()

    internal companion object {
        private val extensionMap = hashMapOf(
            *TrackFileExtension.values()
                .map { it.extension to it }
                .toTypedArray()
        )

        internal fun fromString(extension: String) = extensionMap[extension]!!
    }
}