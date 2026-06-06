package com.titanbbl.funny.face.filter.game.data.network

import com.titanbbl.funny.face.filter.game.data.network.model.NetworkUserDetails
import retrofit2.http.GET
import retrofit2.http.Path

interface UserDetailsService {

    @GET("/users/{user}")
    suspend fun getUserDetails(@Path("user") user: String): NetworkUserDetails
}