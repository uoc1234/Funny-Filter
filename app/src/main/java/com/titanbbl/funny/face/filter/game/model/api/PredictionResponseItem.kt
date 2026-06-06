package com.titanbbl.funny.face.filter.game.model.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Keep
@Parcelize
@JsonClass(generateAdapter = true)
data class PredictionResponseItem(
    @Json(name = "description")
    var description: String,
    @Json(name = "image")
    var image: String,
    @Json(name = "predictions")
    var predictions: List<String>,
    @Json(name = "question")
    var question: String
) : Parcelable