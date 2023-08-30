package com.paranid5.vtmserver.domain

enum class TrackFileExtension {
    MP3, WAV, AAC, FLAC, M4A, OPUS, VORBIS;

    val extension get() = toString().lowercase()

    companion object {
        private val extensionMap by lazy {
            hashMapOf(
                *TrackFileExtension.values()
                    .map { it.extension to it }
                    .toTypedArray()
            )
        }

        fun fromString(extension: String) = extensionMap[extension]!!
    }
}