package com.dinaraparanid.converter.ytdlp

import java.io.Serial

internal class YtDlpException(message: String?) : Exception(message) {
    private companion object {
        @Serial
        private const val serialVersionUID = -4729057718417389335L
    }

    constructor(e: Throwable) : this(e.message)
}