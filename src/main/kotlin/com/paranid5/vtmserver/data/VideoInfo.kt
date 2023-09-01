package com.paranid5.vtmserver.data

import com.github.kiulian.downloader.model.videos.VideoDetails
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoInfo(
    /** Usual title of YouTube video */
    @JvmField val title: String,

    /** Duration is seconds */
    @JvmField val duration: Int,

    /** Text under the video if there is any */
    @JvmField val description: String? = null,

    /** Url of video cover (image) */
    @SerialName("thumbnail") @JvmField val thumbnailURL: String
)

inline val VideoDetails.serializableInfo
    get() = VideoInfo(
        title = title(),
        duration = lengthSeconds(),
        description = description(),
        thumbnailURL = thumbnails().last()
    )