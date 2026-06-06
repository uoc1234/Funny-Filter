package com.titanbbl.funny.face.filter.game.data.network

import com.titanbbl.funny.face.filter.game.data.network.model.NetworkUserListItem
import retrofit2.http.GET

interface UserListService {

    @GET("/repos/square/retrofit/stargazers")
    suspend fun getUserList(): List<NetworkUserListItem>
}