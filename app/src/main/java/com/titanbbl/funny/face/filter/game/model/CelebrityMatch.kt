package com.titanbbl.funny.face.filter.game.model

/**
 * Represents a celebrity match result
 */
data class CelebrityMatch(
    val name: String,
    val confidence: Double,
    val faceToken: String,
    val info: CelebrityInfo? = null
) 