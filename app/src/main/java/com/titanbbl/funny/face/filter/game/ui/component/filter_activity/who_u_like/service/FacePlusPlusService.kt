package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.who_u_like.service

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service for interacting with Face++ API
 */
class FacePlusPlusService {
    
    companion object {
        private const val SEARCH_API = "https://api-us.faceplusplus.com/facepp/v3/search"
        
        // API Keys (Base64 encoded for security)
        private const val CELEBRITY_API_KEY = "MEpyNUtiMVEzYzJNUG9QMU56czdSODRYS0JHdjFuVHc="
        private const val CELEBRITY_API_SECRET = "eF9paUlBSFJUVW5sd0phd0FQRW1Pb0pHbWZaa2RPbjI="
    }
    
    enum class ApiType {
        BEAUTY_SCANNER,
        CELEBRITY_SEARCH
    }
    
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * Get API key and secret based on API type
     */
    private fun getApiKeyAndSecret(apiType: ApiType): Pair<String, String> {
        return when (apiType) {
            ApiType.CELEBRITY_SEARCH -> {
                Pair(
                    String(Base64.decode(CELEBRITY_API_KEY, Base64.DEFAULT)),
                    String(Base64.decode(CELEBRITY_API_SECRET, Base64.DEFAULT))
                )
            }
            else -> {
                // Default to celebrity search for now
                Pair(
                    String(Base64.decode(CELEBRITY_API_KEY, Base64.DEFAULT)),
                    String(Base64.decode(CELEBRITY_API_SECRET, Base64.DEFAULT))
                )
            }
        }
    }
    
    /**
     * Search for a face in a face set
     * @param filePath Path to the image file
     * @param faceSetToken Token of the face set to search in
     * @param apiType Type of API to determine key and secret
     * @return JSON string containing the API response
     */
    fun searchFace(
        filePath: String,
        faceSetToken: String,
        apiType: ApiType = ApiType.CELEBRITY_SEARCH
    ): String {
        val file = File(filePath)
        
        if (!file.exists()) {
            throw IOException("File does not exist: $filePath")
        }
        
        val (apiKey, apiSecret) = getApiKeyAndSecret(apiType)
        
        Timber.d("FacePlusPlusService: Celebrity search with faceset token: $faceSetToken")
        
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image_file", 
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull())
            )
            .addFormDataPart("api_key", apiKey)
            .addFormDataPart("api_secret", apiSecret)
            .addFormDataPart("faceset_token", faceSetToken)
            .addFormDataPart("return_result_count", "5") // Return top 5 matches
            .build()

        val request = Request.Builder()
            .url(SEARCH_API)
            .post(requestBody)
            .addHeader("accept", "*/*")
            .addHeader("Content-Type", "multipart/form-data")
            .addHeader("Host", "api-us.faceplusplus.com")
            .build()
        
        Timber.d("FacePlusPlusService: Sending celebrity search request to $SEARCH_API")
        
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        
        Timber.d("FacePlusPlusService: Celebrity search response code: ${response.code}, body length: ${responseBody.length}")
        
        if (!response.isSuccessful) {
            Timber.e("FacePlusPlusService: Celebrity search API Error: $responseBody")
            throw IOException("API Error: ${response.code} - $responseBody")
        }
        
        // Log response preview for debugging
        val previewLength = minOf(responseBody.length, 300)
        Timber.d("FacePlusPlusService: Celebrity search response preview: ${responseBody.substring(0, previewLength)}...")
        
        return responseBody
    }
} 