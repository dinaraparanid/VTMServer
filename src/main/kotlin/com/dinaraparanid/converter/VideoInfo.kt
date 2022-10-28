package com.dinaraparanid.converter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoInfo(
    val title: String,
    val duration: Long,
    val description: String,
    @SerialName("_filename") val fileName: String,
    @SerialName("thumbnail") val thumbnailURL: String
)
