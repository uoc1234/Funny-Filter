package com.titanbbl.funny.face.filter.game.model

import androidx.annotation.DrawableRes

data class LipFallItem(
    @DrawableRes val iconRes: Int,
    val title: String,
    val type: LipFallType
)

enum class LipFallType {
    FACEPUZZLE,
    ZOOMPUZZLE,
    SPINPUZZLE,

} 