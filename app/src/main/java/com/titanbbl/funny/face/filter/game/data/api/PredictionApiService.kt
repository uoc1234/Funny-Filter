package com.titanbbl.funny.face.filter.game.data.api

import com.titanbbl.funny.face.filter.game.model.api.PredictionResponseItem
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

interface PredictionApiService {
    @GET("funnyfilter/quiz.json")
    suspend fun getPredictions(): List<PredictionResponseItem>
    
    companion object {
        private const val BASE_URL = "https://bblcdn.b-cdn.net/"
        
        fun create(): PredictionApiService {
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
                .create(PredictionApiService::class.java)
        }
    }
} 