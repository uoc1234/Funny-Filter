package com.titanbbl.funny.face.filter.game.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
@Parcelize
data class PhysicalFeatureItem(
    val id: Int,
    val type: PhysicalFeatureType,
    val title: String,
    val subtitle: String? = null,
    val imageRes: Int,
    val overlayImageRes: Int? = null,
    val flags: List<Int> = emptyList()
): Parcelable

enum class PhysicalFeatureType {
    LIPS,
    NOSE, 
    HAND
} 