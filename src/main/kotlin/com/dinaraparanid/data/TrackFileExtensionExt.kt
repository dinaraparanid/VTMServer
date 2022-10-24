package com.dinaraparanid.data

internal inline val extensionMap
    get() = hashMapOf(*TrackFileExtension.values().map { it.extension to it }.toTypedArray())