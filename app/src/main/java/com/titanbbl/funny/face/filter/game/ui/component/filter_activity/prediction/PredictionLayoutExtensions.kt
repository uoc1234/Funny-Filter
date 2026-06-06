package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.prediction

import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity

/**
 * Extension function to set text and image in one call
 */
fun PredictionLayout.setContent(text: String, image: Any) {
    setText(text)
    setImage(image)
}

/**
 * Extension function to set text and image URL in one call
 */
fun PredictionLayout.setContentWithUrl(text: String, imageUrl: String) {
    setText(text)
    setImageUrl(imageUrl)
}

/**
 * Extension function to set text and image resource in one call
 */
fun PredictionLayout.setContentWithResource(text: String, imageResource: Int) {
    setText(text)
    setImageResource(imageResource)
}

/**
 * Extension function for FragmentActivity to find PredictionLayout
 */
fun FragmentActivity.findPredictionLayout(id: Int): PredictionLayout? {
    return findViewById(id)
}

/**
 * Extension function for FrameLayout to find PredictionLayout
 */
fun FrameLayout.findPredictionLayout(id: Int): PredictionLayout? {
    return findViewById(id)
} 