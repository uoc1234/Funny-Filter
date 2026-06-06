package com.titanbbl.funny.face.filter.game.model

data class VideoFilterItem(
    val id: Int,
    val rank: Int,
    val videoUrl: String,
    val accessKey: String,
    val likes: String,
    val views: String,
    val shares: String,
    val category: String,
    val filterName: String
)