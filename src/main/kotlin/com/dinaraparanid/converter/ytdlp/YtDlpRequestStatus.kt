package com.dinaraparanid.converter.ytdlp

internal sealed interface YtDlpRequestStatus {
    data class Success<T>(@JvmField val data: T) : YtDlpRequestStatus

    enum class Error : YtDlpRequestStatus {
        NO_INTERNET,
        INCORRECT_URL_LINK,
        UNKNOWN_ERROR,
        INVALID_DATA,
        STREAM_CONVERSION,
        GEO_RESTRICTED
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <T> YtDlpRequestStatus.Success<*>.castAndGetData() = data as T