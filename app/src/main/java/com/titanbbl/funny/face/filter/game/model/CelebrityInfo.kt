package com.titanbbl.funny.face.filter.game.model

/**
 * Represents detailed information about a celebrity
 */
data class CelebrityInfo(
    val name: String,
    val description: String,
    val imageUrls: List<String>,
    val faceToken: String,
    val additionalInfo: Map<String, Any> = emptyMap()
) 