package com.titanbbl.funny.face.filter.game.model

/**
 * Represents a face set token for celebrity matching
 */
data class FaceSetToken(
    val token: String,
    val displayName: String,
    val description: String = ""
) {
    companion object {
        // Face set tokens for different categories
        val FAMOUS_SOCIAL_NETWORK = FaceSetToken(
            token = "adf1e56e0a10bdf00280ebc4462890ec",
            displayName = "Famous Social Network",
            description = "Social media influencers and content creators"
        )
        
        val FAMOUS_ASIA_SOCIAL_NETWORK = FaceSetToken(
            token = "e8480978aabc5fcd8d8fed1c3e46ecff",
            displayName = "Famous Asia Social Network",
            description = "Asian social media celebrities and influencers"
        )
        
        val TOP_100_BEAUTY_WOMEN = FaceSetToken(
            token = "2159d5f354ff25946a6144de0ed23381",
            displayName = "Top 100 Beauty Women",
            description = "Most beautiful women in the world"
        )
        
        val TOP_100_HANDSOME_MEN = FaceSetToken(
            token = "05d0c5b2036b5dc8a04ff7d6b5e352f8",
            displayName = "Top 100 Handsome Men",
            description = "Most handsome men in the world"
        )
        
        val FAMOUS_FOOTBALL_PLAYER = FaceSetToken(
            token = "47d39a5aa9d0a8e1c39b17edc514d85c",
            displayName = "Famous Football Player",
            description = "Professional football players and legends"
        )
        
        val BEST_FOOTBALLERS_2023 = FaceSetToken(
            token = "f07078111810ea5bc514ce72b8462f7b",
            displayName = "Best Footballers 2023",
            description = "Top football players of 2023"
        )
        
        val TOP_BASKETBALL_PLAYER = FaceSetToken(
            token = "24a419ad63f40f8ec643ec29629d5a19",
            displayName = "Top Basketball Player",
            description = "Professional basketball players"
        )
        
        val TOP_BODY_BUILDER = FaceSetToken(
            token = "ec6fed95498b3a6373109fd2cb4e68e9",
            displayName = "Top Body Builder",
            description = "Professional bodybuilders and fitness athletes"
        )
        
        val TOP_MODELS = FaceSetToken(
            token = "024de0c6e3b9944233baf4e4f3829a40",
            displayName = "Top Models",
            description = "Professional fashion models"
        )
        
        val RICHEST_PEOPLE = FaceSetToken(
            token = "8ee7767e079f39cedc4f3431b5a42e42",
            displayName = "Richest People",
            description = "Billionaires and wealthy business leaders"
        )
    }
}