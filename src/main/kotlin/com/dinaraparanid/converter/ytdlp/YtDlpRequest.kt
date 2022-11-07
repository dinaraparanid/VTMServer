package com.dinaraparanid.converter.ytdlp

import java.util.HashMap

internal class YtDlpRequest(
    private var url: String? = null,
    @JvmField var directory: String? = null
) {

    internal val options: MutableMap<String, String?> = HashMap()

    internal fun setOption(key: String, value: String? = null) = options.set(key, value)
    internal fun setOption(key: String, value: Int) = options.set(key, value.toString())

    /**
     * Transform options to a string that the executable will execute
     * @return Command string
     */

    internal fun buildOptions() =
        StringBuilder()
            .also { builder -> url?.let { builder.append("$it ") } }
            .append(
                value = options
                    .entries
                    .map { (name, valueOrNull) -> name to (valueOrNull ?: "") }
                    .map { (name, value) -> "--$name $value".trim() }
                    .map { optionFormatted -> "$optionFormatted " }
                    .toTypedArray()
            )
            .toString()
            .trim()
}