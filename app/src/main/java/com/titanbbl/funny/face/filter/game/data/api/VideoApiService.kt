package com.titanbbl.funny.face.filter.game.data.api

import com.titanbbl.funny.face.filter.game.model.api.VideoResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

interface VideoApiService {
    @GET("funnyfilter/data.json")
    suspend fun getVideos(): VideoResponse
    
    companion object {
        private const val BASE_URL = "https://bblcdn.b-cdn.net/"
        
        fun create(): VideoApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .build()
                    chain.proceed(request)
                }
                .addInterceptor(loggingInterceptor)
                .build()
                
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(VideoApiService::class.java)
        }
    }
} 