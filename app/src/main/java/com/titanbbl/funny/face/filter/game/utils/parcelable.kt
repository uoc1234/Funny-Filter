package com.titanbbl.funny.face.filter.game.utils

import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat

// Function definition
inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? {
    return if (SDK_INT >= 33) {
        IntentCompat.getParcelableExtra(this, key, T::class.java)
    } else {
        @Suppress("DEPRECATION") getParcelableExtra(key) as? T
    }
}



inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? {
    return if (SDK_INT >= 33) {
        BundleCompat.getParcelable(this, key, T::class.java)
    } else {
        @Suppress("DEPRECATION") getParcelable(key) as? T
    }
}

// For Serializable in Intent
inline fun <reified T : Serializable> Intent.serializable(key: String): T? {
    return if (SDK_INT >= 33) {
        getSerializableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION") getSerializableExtra(key) as? T
    }
}

// For Serializable in Bundle
inline fun <reified T : Serializable> Bundle.serializable(key: String): T? {
    return if (SDK_INT >= 33) {
        getSerializable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION") getSerializable(key) as? T
    }
}