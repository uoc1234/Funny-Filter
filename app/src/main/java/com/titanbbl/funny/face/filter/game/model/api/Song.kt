package com.titanbbl.funny.face.filter.game.model.api


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Keep
@Parcelize
data class Song(
    @SerializedName("duration")
    var duration: String,
    @SerializedName("id")
    var id: Int,
    @SerializedName("title")
    var title: String,
    @SerializedName("url")
    var url: String,
    @SerializedName("isSelected")
    var isSelected: Boolean? = false
) : Parcelable