package com.titanbbl.funny.face.filter.game.ads

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.titanbbl.funny.face.filter.game.BuildConfig

object RemoteConfigUtils {

    private const val TAG = "RemoteConfigUtils"

    private const val ON_NATIVE_INTER_SPLASH = "on_native_inter_splash"
    private const val ON_NATIVE_LANGUAGE = "on_native_language"
    private const val ON_NATIVE_ON_BOARDING = "on_native_on_boarding"
    private const val ON_NATIVE_PERMISSION = "on_native_permission"


    var completed = false
    private val DEFAULTS: HashMap<String, Any> =
        hashMapOf(
            ON_NATIVE_LANGUAGE to true,
            ON_NATIVE_ON_BOARDING to true,
            ON_NATIVE_PERMISSION to true,
            ON_NATIVE_INTER_SPLASH to true
            )

    interface Listener {
        fun loadSuccess()
    }

    lateinit var listener: Listener
    private lateinit var remoteConfig: FirebaseRemoteConfig

    fun init(mListener: Listener) {
        listener = mListener
        remoteConfig =
            getFirebaseRemoteConfig()
    }

    private fun getFirebaseRemoteConfig(): FirebaseRemoteConfig {
        remoteConfig = Firebase.remoteConfig

        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) {
                0
            } else {
                60 * 60
            }
        }
        remoteConfig.apply {
            setConfigSettingsAsync(configSettings)
            setDefaultsAsync(DEFAULTS)
            fetchAndActivate().addOnCompleteListener {
                listener.loadSuccess()
                completed = true
            }
        }
        return remoteConfig
    }


    //native language
    fun getOnNativeLanguage(): Boolean {
        try {
            return if (!completed) {
                true
            } else {
                remoteConfig.getBoolean(ON_NATIVE_LANGUAGE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
    fun getOnNativeOnBoarding(): Boolean {
        try {
            return if (!completed) {
                true
            } else {
                remoteConfig.getBoolean(ON_NATIVE_ON_BOARDING)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun getOnNativePermission(): Boolean {
        try {
            return if (!completed) {
                true
            } else {
                remoteConfig.getBoolean(ON_NATIVE_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
    fun getOnInterSplash(): Boolean {
        try {
            return if (!completed) {
                true
            } else {
                remoteConfig.getBoolean(ON_NATIVE_INTER_SPLASH)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

}