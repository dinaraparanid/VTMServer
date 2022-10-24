package com.dinaraparanid.data

import com.sapher.youtubedl.mapper.VideoInfo
import kotlinx.serialization.Serializable

@Serializable
data class VideoInfo(val title: String, val duration: Int, val description: String) {
    constructor(ydlVideoInfo: VideoInfo) : this(ydlVideoInfo.title, ydlVideoInfo.duration, ydlVideoInfo.description)
}
