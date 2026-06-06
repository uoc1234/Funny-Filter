package com.titanbbl.funny.face.filter.game.app

import android.annotation.SuppressLint
import android.app.Application
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import com.titanbbl.funny.face.filter.game.BuildConfig
import timber.log.Timber


class GlobalApp : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: GlobalApp

    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Khởi tạo MusicManagerApp (object singleton)
        MusicManagerApp.init(this)
        val config =
            PRDownloaderConfig.newBuilder().setReadTimeout(30000).setConnectTimeout(30000).build()
        PRDownloader.initialize(this, config)

    }


}