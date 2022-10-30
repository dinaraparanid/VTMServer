package com.dinaraparanid.converter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class VideoInfo(
    val title: String,
    val duration: Long,
    val description: String,
    @SerialName("_filename") val fileName: String,
    @SerialName("thumbnail") val thumbnailURL: String
)

inline val VideoInfo.withFileNameWithoutExt
    get() = VideoInfo(
        title,
        duration,
        description,
        File(fileName).nameWithoutExtension,
        thumbnailURL
    )