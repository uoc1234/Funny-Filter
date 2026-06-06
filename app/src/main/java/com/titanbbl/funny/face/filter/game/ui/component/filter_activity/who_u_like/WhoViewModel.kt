package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.who_u_like

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.titanbbl.funny.face.filter.game.model.CelebrityInfo
import com.titanbbl.funny.face.filter.game.ui.bases.BaseViewModel
import com.titanbbl.funny.face.filter.game.model.CelebrityMatch
import com.titanbbl.funny.face.filter.game.model.FaceSetToken
import com.titanbbl.funny.face.filter.game.model.Resource
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.who_u_like.service.CelebrityApiService
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.who_u_like.service.FacePlusPlusService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
\\ \\ \\ \\ \\ \\ \\ \\ || || || || || || // // // // // // // //
\\ \\ \\ \\ \\ \\ \\        _ooOoo_          // // // // // // //
\\ \\ \\ \\ \\ \\          o8888888o            // // // // // //
\\ \\ \\ \\ \\             88" . "88               // // // // //
\\ \\ \\ \\                (| -_- |)                  // // // //
\\ \\ \\                   O\  =  /O                     // // //
\\ \\                   ____/`---'\____                     // //
\\                    .'  \\|     |//  `.                      //
==                   /  \\|||  :  |||//  \                     ==
==                  /  _||||| -:- |||||-  \                    ==
==                  |   | \\\  -  /// |   |                    ==
==                  | \_|  ''\---/''  |   |                    ==
==                  \  .-\__  `-`  ___/-. /                    ==
==                ___`. .'  /--.--\  `. . ___                  ==
==              ."" '<  `.___\_<|>_/___.'  >'"".               ==
==            | | :  `- \`.;`\ _ /`;.`/ - ` : | |              \\
//            \  \ `-.   \_ __\ /__ _/   .-` /  /              \\
//      ========`-.____`-.___\_____/___.-`____.-'========      \\
//                           `=---='                           \\
// //   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^  \\ \\
// // //    Buddha blessed    Never BUG    Never modify   \\ \\ \\
 **/
class WhoViewModel : BaseViewModel() {

    private val facePlusPlusService = FacePlusPlusService()
    private val celebrityApiService = CelebrityApiService()
    
    // LiveData for celebrity search results
    private val _celebritySearchResult = MutableLiveData<Resource<Map<String, Any>>>()
    val celebritySearchResult: LiveData<Resource<Map<String, Any>>> = _celebritySearchResult
    
    /**
     * Search for celebrity matches using a photo
     * @param filePath Path to the photo file
     * @param faceSetToken Face set token to search in
     */
    fun searchCelebrity(filePath: String, faceSetToken: FaceSetToken = FaceSetToken.FAMOUS_SOCIAL_NETWORK ) {
        _celebritySearchResult.postValue(Resource.Loading())
        
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Timber.d("Searching celebrities in category: ${faceSetToken.displayName}")
                    
                    val response = facePlusPlusService.searchFace(
                        filePath = filePath,
                        faceSetToken = faceSetToken.token,
                        apiType = FacePlusPlusService.ApiType.CELEBRITY_SEARCH
                    )
                    
                    // Parse the response
                    val jsonObject = JSONObject(response)
                    
                    // Check if the API call was successful
                    if (jsonObject.has("faces") && jsonObject.getJSONArray("faces").length() > 0) {
                        // Check if there are search results
                        if (jsonObject.has("results") && jsonObject.getJSONArray("results").length() > 0) {
                            val results = jsonObject.getJSONArray("results")
                            val celebrityMatches = mutableListOf<CelebrityMatch>()
                            
                            // Process all results (up to 5)
                            for (i in 0 until minOf(results.length(), 5)) {
                                val resultItem = results.getJSONObject(i)
                                val confidence = resultItem.getDouble("confidence")
                                val faceToken = resultItem.getString("face_token")
                                
                                // Fetch celebrity info from our API
                                val celebrityInfo = celebrityApiService.getCelebrityInfo(faceToken, faceSetToken)
                                val celebrityName = celebrityInfo?.name ?: "Unknown Celebrity"
                                
                                celebrityMatches.add(
                                    CelebrityMatch(
                                        name = celebrityName,
                                        confidence = confidence,
                                        faceToken = faceToken,
                                        info = celebrityInfo
                                    )
                                )
                            }
                            
                            // Create result map
                            val resultMap = mapOf(
                                "success" to true,
                                "matches" to celebrityMatches,
                                "category" to faceSetToken,
                                "topConfidence" to celebrityMatches.first().confidence,
                                "rawResponse" to response
                            )
                            
                            Timber.d("Celebrity search success: ${celebrityMatches.size} matches found")
                            _celebritySearchResult.postValue(Resource.Success(resultMap))
                        } else {
                            // No matches found
                            val resultMap = mapOf(
                                "success" to false,
                                "error" to "No celebrity matches found in ${faceSetToken.displayName}",
                                "category" to faceSetToken,
                                "rawResponse" to response
                            )
                            _celebritySearchResult.postValue(Resource.Error("No celebrity matches found", resultMap))
                        }
                    } else {
                        // No face detected
                        val resultMap = mapOf(
                            "success" to false,
                            "error" to "No face detected in the image",
                            "code" to -1,
                            "category" to faceSetToken,
                            "rawResponse" to response
                        )
                        _celebritySearchResult.postValue(Resource.Error("No face detected in the image", resultMap))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error searching celebrity in category: ${faceSetToken.displayName}")
                val resultMap = mapOf(
                    "success" to false,
                    "error" to "Error: ${e.message}",
                    "category" to faceSetToken
                )
                _celebritySearchResult.postValue(Resource.Error("Error: ${e.message}", resultMap))
            }
        }
    }
    
    /**
     * Get the first image URL for a celebrity
     * @param celebrityInfo The celebrity info object
     * @return First URL from the celebrity's image URLs or null if not available
     */
    fun getFirstImageUrl(celebrityInfo: CelebrityInfo?): String? {
        return celebrityInfo?.imageUrls?.firstOrNull()
    }
}