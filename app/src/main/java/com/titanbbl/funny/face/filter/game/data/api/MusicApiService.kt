package com.titanbbl.funny.face.filter.game.data.api

import com.titanbbl.funny.face.filter.game.BuildConfig
import com.titanbbl.funny.face.filter.game.model.api.MusicResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface MusicApiService {
    @GET("funnyfilter/soundFilter.json")
    suspend fun getMusic(): MusicResponse
    
    companion object {
        private const val BASE_URL = "https://bblcdn.b-cdn.net/"
        
        fun create(): MusicApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("accessKey", BuildConfig.BUNNY_CDN_ACCESS_KEY)
                        .build()
                    chain.proceed(request)
                }
                .addInterceptor(loggingInterceptor)
                .build()
                
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MusicApiService::class.java)
        }
    }
} 