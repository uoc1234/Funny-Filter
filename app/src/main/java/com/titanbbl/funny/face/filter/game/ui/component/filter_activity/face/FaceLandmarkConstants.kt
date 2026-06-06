package com.titanbbl.funny.face.filter.game.ui.component.filter_activity.face

/**
 * MediaPipe Face Landmark Constants
 * 
 * This class contains the predefined indices for MediaPipe's 468 face landmarks.
 * These indices are standardized by Google's MediaPipe library.
 * 
 * Reference: https://github.com/google-ai-edge/mediapipe-samples/tree/main/examples/face_landmarker/android
 */
object FaceLandmarkConstants {
    
    /**
     * Left eye landmark indices (MediaPipe Face Mesh standard - 468 points)
     * Complete eye contour including upper and lower eyelids
     */
    val LEFT_EYE_INDICES = listOf(
        33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246
    )
    
    /**
     * Right eye landmark indices (MediaPipe Face Mesh standard - 468 points)
     * Complete eye contour including upper and lower eyelids
     */
    val RIGHT_EYE_INDICES = listOf(
        362, 382, 381, 380, 374, 373, 390, 249, 263, 466, 388, 387, 386, 385, 384, 398
    )
    
    /**
     * Left eyebrow landmark indices (MediaPipe Face Mesh standard)
     * Complete eyebrow contour from inner to outer corner
     */
    val LEFT_EYEBROW_INDICES = listOf(
        70, 63, 105, 66, 107, 55, 65, 52, 53, 46
    )
    
    /**
     * Right eyebrow landmark indices (MediaPipe Face Mesh standard)
     * Complete eyebrow contour from inner to outer corner
     */
    val RIGHT_EYEBROW_INDICES = listOf(
        296, 334, 293, 300, 276, 283, 282, 295, 285, 336
    )
    
    /**
     * Nose landmark indices (MediaPipe Face Mesh standard)
     * Complete nose structure including bridge, tip, and nostrils
     */
    val NOSE_INDICES = listOf(
        1, 2, 5, 4, 6, 168, 8, 9, 10, 151, 195, 197, 196, 3, 51, 115, 131, 134, 102, 49, 220, 305, 289, 290, 327, 326
    )
    
    /**
     * Mouth landmark indices (MediaPipe Face Mesh standard)
     * Complete mouth/lips outer contour
     */
    val MOUTH_INDICES = listOf(
        61, 146, 91, 181, 84, 17, 314, 405, 320, 307, 375, 269, 267, 269, 270, 267, 271, 272
    )
    
    /**
     * Face oval landmark indices
     * These points define the outer contour of the face
     */
    val FACE_OVAL_INDICES = listOf(
        10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288, 397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136, 172, 58, 132, 93, 234, 127, 162, 21, 54, 103, 67, 109
    )
    
    /**
     * Inner lips landmark indices
     * These points define the inner contour of the lips
     */
    val INNER_LIPS_INDICES = listOf(
        78, 95, 88, 178, 87, 14, 317, 402, 318, 324, 308
    )
    
    /**
     * Outer lips landmark indices  
     * These points define the outer contour of the lips
     */
    val OUTER_LIPS_INDICES = listOf(
        61, 146, 91, 181, 84, 17, 314, 405, 320, 307, 375, 269, 267, 269, 270, 267, 271, 272
    )
    
    // Simplified and more accurate landmark indices for better detection
    
    /**
     * Simplified left eye landmarks - more accurate for detection
     */
    val LEFT_EYE_SIMPLE = listOf(33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246)
    
    /**
     * Simplified right eye landmarks - more accurate for detection
     */
    val RIGHT_EYE_SIMPLE = listOf(362, 382, 381, 380, 374, 373, 390, 249, 263, 466, 388, 387, 386, 385, 384, 398)
    
    /**
     * Simplified left eyebrow landmarks - more accurate for detection
     */
    val LEFT_EYEBROW_SIMPLE = listOf(46, 53, 52, 51, 48, 115, 131, 134, 102, 64, 68)
    
    /**
     * Simplified right eyebrow landmarks - more accurate for detection
     */
    val RIGHT_EYEBROW_SIMPLE = listOf(276, 283, 282, 281, 278, 344, 360, 363, 331, 294, 298)
    
    /**
     * Simplified nose landmarks - more accurate for detection
     */
    val NOSE_SIMPLE = listOf(1, 2, 5, 4, 6, 19, 20, 94, 125, 141, 235, 236, 3, 51, 115, 131, 134, 102, 49, 220, 305, 289, 290, 327, 326)
    
    /**
     * Simplified mouth landmarks - more accurate for detection
     */
    val MOUTH_SIMPLE = listOf(61, 84, 17, 314, 405, 320, 307, 375, 321, 308, 324, 318, 13, 82, 81, 80, 78)

    /**
     * Get facial feature indices by name
     * @param featureName The name of the facial feature
     * @param useSimple Whether to use simplified indices for better detection
     * @return List of landmark indices for the specified feature
     */
    fun getFeatureIndices(featureName: String, useSimple: Boolean = true): List<Int> {
        return when (featureName.lowercase()) {
            "left_eye", "lefteye" -> if (useSimple) LEFT_EYE_SIMPLE else LEFT_EYE_INDICES
            "right_eye", "righteye" -> if (useSimple) RIGHT_EYE_SIMPLE else RIGHT_EYE_INDICES
            "left_eyebrow", "lefteyebrow" -> if (useSimple) LEFT_EYEBROW_SIMPLE else LEFT_EYEBROW_INDICES
            "right_eyebrow", "righteyebrow" -> if (useSimple) RIGHT_EYEBROW_SIMPLE else RIGHT_EYEBROW_INDICES
            "nose" -> if (useSimple) NOSE_SIMPLE else NOSE_INDICES
            "mouth", "lips" -> if (useSimple) MOUTH_SIMPLE else MOUTH_INDICES
            "face_oval", "faceoval" -> FACE_OVAL_INDICES
            "inner_lips", "innerlips" -> INNER_LIPS_INDICES
            "outer_lips", "outerlips" -> OUTER_LIPS_INDICES
            else -> emptyList()
        }
    }
    
    /**
     * Get all available feature names
     * @return List of all available facial feature names
     */
    fun getAllFeatureNames(): List<String> {
        return listOf(
            "left_eye", "right_eye", "left_eyebrow", "right_eyebrow", 
            "nose", "mouth", "face_oval", "inner_lips", "outer_lips"
        )
    }
} 