package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.who_u_like.service

import com.titanbbl.funny.face.filter.game.model.CelebrityInfo
import com.titanbbl.funny.face.filter.game.model.FaceSetToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import timber.log.Timber

/**
 * Service for retrieving celebrity information
 */
class CelebrityApiService {
    
    private val okHttpClient = OkHttpClient()


    private val BASE_URL = "https://storage.bunnycdn.com/bblprivate/Face-Analysis/data/"
    private val IMAGE_BASE_URL = "https://storage.bunnycdn.com/bblprivate/Face-Analysis/images/"
    private val ACCESS_KEY = "03d01bc4-2cec-4d16-92ca5d0d2ca5-3eae-4cb8"
    
    /**
     * Get celebrity information based on face token and face set
     * @param faceToken The face token from search results
     * @param category The face set token used for search
     * @return CelebrityInfo object or null if not found
     */
    suspend fun getCelebrityInfo(faceToken: String, category: FaceSetToken): CelebrityInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = getUrlForCategory(category)
                val request = Request.Builder()
                    .url(url)
                    .addHeader("AccessKey", ACCESS_KEY)
                    .build()

                Timber.d("Fetching celebrity data from: $url")
                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    Timber.e("API request failed with code: ${response.code}")
                    return@withContext null
                }

                val jsonString = response.body?.string()

                if (jsonString != null) {
                    parseCelebrityInfo(jsonString, faceToken, category)
                } else {
                    Timber.e("Failed to fetch celebrity data: Empty response")
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch celebrity data")
                null
            }
        }
    }
    
    /**
     * Parse the JSON response to extract celebrity information
     * @param jsonString The JSON response string
     * @param faceToken The face token to match
     * @param category The face set category used for search
     * @return CelebrityInfo object or null if parsing fails
     */
    private fun parseCelebrityInfo(jsonString: String, faceToken: String, category: FaceSetToken): CelebrityInfo? {
        return try {
            // Parse the JSON array
            val jsonArray = JSONArray(jsonString)
            
            // Find the celebrity with matching token
            for (i in 0 until jsonArray.length()) {
                val celebrityObject = jsonArray.getJSONObject(i)
                val token = celebrityObject.getString("token")
                
                if (token == faceToken) {
                    // Found matching celebrity
                    val name = celebrityObject.getString("name")
                    val country = celebrityObject.optString("country", "")
                    val dateOfBirth = celebrityObject.optString("dateOfBirth", "")
                    
                    // Get career information
                    val careerArray = celebrityObject.optJSONArray("career")
                    val careers = mutableListOf<String>()
                    if (careerArray != null) {
                        for (j in 0 until careerArray.length()) {
                            careers.add(careerArray.getString(j))
                        }
                    }
                    
                    // Create description from available information
                    val description = buildString {
                        if (country.isNotEmpty()) append("$country ")
                        if (careers.isNotEmpty()) {
                            append(careers.joinToString(", "))
                            append(" ")
                        }
                        if (dateOfBirth.isNotEmpty()) append("Born: $dateOfBirth")
                    }.trim()
                    
                    // Generate image URLs
                    val formattedName = name.replace(" ", "-")
                    val imageUrls = listOf("$IMAGE_BASE_URL$formattedName.png")
                    
                    // Create additional info map
                    val additionalInfo = mapOf(
                        "country" to country,
                        "dateOfBirth" to dateOfBirth,
                        "careers" to careers
                    )
                    
                    return CelebrityInfo(
                        name = name,
                        description = description,
                        imageUrls = imageUrls,
                        faceToken = token,
                        additionalInfo = additionalInfo
                    )
                }
            }
            
            // If no matching celebrity found, try to use the first entry as a fallback
            if (jsonArray.length() > 0) {
                Timber.w("No exact match found for token: $faceToken. Using first celebrity as fallback.")
                val firstCelebrity = jsonArray.getJSONObject(0)
                
                val name = firstCelebrity.getString("name")
                val country = firstCelebrity.optString("country", "")
                val dateOfBirth = firstCelebrity.optString("dateOfBirth", "")
                val token = firstCelebrity.getString("token")
                
                // Get career information
                val careerArray = firstCelebrity.optJSONArray("career")
                val careers = mutableListOf<String>()
                if (careerArray != null) {
                    for (j in 0 until careerArray.length()) {
                        careers.add(careerArray.getString(j))
                    }
                }
                
                // Create description from available information
                val description = buildString {
                    if (country.isNotEmpty()) append("$country ")
                    if (careers.isNotEmpty()) {
                        append(careers.joinToString(", "))
                        append(" ")
                    }
                    if (dateOfBirth.isNotEmpty()) append("Born: $dateOfBirth")
                }.trim()
                
                // Generate image URLs
                val formattedName = name.replace(" ", "-")
                val imageUrls = listOf("$IMAGE_BASE_URL$formattedName.png")
                
                // Create additional info map
                val additionalInfo = mapOf(
                    "country" to country,
                    "dateOfBirth" to dateOfBirth,
                    "careers" to careers,
                    "isFallback" to true
                )
                
                return CelebrityInfo(
                    name = name,
                    description = description,
                    imageUrls = imageUrls,
                    faceToken = token,
                    additionalInfo = additionalInfo
                )
            }
            
            // If no celebrities found at all
            Timber.e("No celebrities found in the response")
            null
        } catch (e: Exception) {
            Timber.e(e, "Error parsing celebrity info: ${e.message}")
            null
        }
    }
    
    /**
     * Get API URL based on category
     * @param category The face set category
     * @return URL for the API endpoint
     */
    private fun getUrlForCategory(category: FaceSetToken): String {
        val filename = when (category) {
            FaceSetToken.TOP_100_BEAUTY_WOMEN -> "beauty_women.json"
            FaceSetToken.BEST_FOOTBALLERS_2023 -> "best_footballer.json"
            FaceSetToken.FAMOUS_FOOTBALL_PLAYER -> "famous_football_player.json"
            FaceSetToken.FAMOUS_SOCIAL_NETWORK -> "famous_people_social_networks.json"
            FaceSetToken.TOP_100_HANDSOME_MEN -> "handsome_men.json"
            FaceSetToken.RICHEST_PEOPLE -> "richest_people.json"
            FaceSetToken.TOP_BASKETBALL_PLAYER -> "top_basketball_player.json"
            FaceSetToken.TOP_BODY_BUILDER -> "top_body_builder.json"
            FaceSetToken.TOP_MODELS -> "top_models.json"
            FaceSetToken.FAMOUS_ASIA_SOCIAL_NETWORK -> "famous_people_social_networks.json" // Using the same file for now
            else -> {
                "top_models.json"
            }
        }
        return BASE_URL + filename
    }
    
    /**
     * Generate image URL for a celebrity
     * @param name The celebrity name
     * @return URL to the celebrity image
     */
    private fun generateImageUrl(name: String): String {
        val formattedName = name.replace(" ", "-")
        return "$IMAGE_BASE_URL$formattedName.png"
    }
} 