package com.dinaraparanid.converter.ytdlp

internal data class YtDlpResponse(
    @JvmField internal val command: String,
    @JvmField internal val options: Map<String, String?>,
    @JvmField internal val directory: String?,
    @JvmField internal val exitCode: Int,
    @JvmField internal val elapsedTime: Int,
    @JvmField internal val out: String,
    @JvmField internal val err: String
)

