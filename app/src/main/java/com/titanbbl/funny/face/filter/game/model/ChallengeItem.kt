package com.titanbbl.funny.face.filter.game.model

import android.graphics.Color

data class ChallengeItem(
    val id: Int,
    val imageUrl: Int,
    val label: String,
    var isSelected: Boolean = false,
    val strokeWidth: Float = 10f,
    val strokeColor: Int = Color.WHITE
)