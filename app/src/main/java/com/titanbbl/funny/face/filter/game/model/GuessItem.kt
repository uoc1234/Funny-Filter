package com.titanbbl.funny.face.filter.game.model

import androidx.annotation.DrawableRes

data class GuessItem(
    @DrawableRes val iconRes: Int,
    val title: String,
    val type: GuessType
)

enum class GuessType {
    CARTOON,
    SEASON,
    Y2K,
    GAME
} 