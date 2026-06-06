package com.titanbbl.funny.face.filter.game.model.api


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Keep
@Parcelize
data class MusicResponse(
    @SerializedName("songs")
    var songs: List<Song>
) : Parcelable