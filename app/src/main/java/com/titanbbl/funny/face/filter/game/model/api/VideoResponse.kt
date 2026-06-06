package com.titanbbl.funny.face.filter.game.model.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VideoResponse(
    @Json(name = "items")
    val items: List<VideoItem>
)

@JsonClass(generateAdapter = true)
data class VideoItem(
    @Json(name = "id")
    val id: Int,
    @Json(name = "rank")
    val rank: Int,
    @Json(name = "video_name")
    val videoName: String,
    @Json(name = "likes")
    val likes: Int,
    @Json(name = "views")
    val views: Int,
    @Json(name = "shares")
    val shares: Int,
    @Json(name = "category")
    val category: String,
    @Json(name = "filter_name")
    val filterName: String
) 