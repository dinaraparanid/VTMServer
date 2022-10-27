package com.dinaraparanid.converter

internal sealed interface YoutubeDLRequestStatus {
    data class Success<T>(val data: T) : YoutubeDLRequestStatus

    enum class Error : YoutubeDLRequestStatus {
        NO_INTERNET,
        INCORRECT_URL_LINK,
        UNKNOWN_ERROR,
        INVALID_DATA
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <T> YoutubeDLRequestStatus.Success<*>.castAndGetData() = data as T