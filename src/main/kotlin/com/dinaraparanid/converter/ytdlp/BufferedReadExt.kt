package com.dinaraparanid.converter

import java.io.BufferedReader

internal fun BufferedReader.readCurrentLines(): List<String> {
    val lines = mutableListOf<String>()
    var s: String? = null

    while (ready() && readLine().also { s = it } != null)
        lines.add(s!!)

    return lines
}