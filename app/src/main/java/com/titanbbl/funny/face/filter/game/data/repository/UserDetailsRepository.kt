package com.titanbbl.funny.face.filter.game.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.titanbbl.funny.face.filter.game.data.database.UsersDatabase
import com.titanbbl.funny.face.filter.game.data.database.asDomainModel
import com.titanbbl.funny.face.filter.game.data.domain.UserDetails
import com.titanbbl.funny.face.filter.game.data.network.UserDetailsService
import com.titanbbl.funny.face.filter.game.data.network.model.asDatabaseModel
import timber.log.Timber

class UserDetailsRepository(
    private val userDetailsService: UserDetailsService,
    private val database: UsersDatabase
) {

    fun getUserDetails(user: String): LiveData<UserDetails> {
        return database.usersDao.getUserDetails(user).map {
            it.asDomainModel()
        }
    }

    suspend fun refreshUserDetails(user: String) {
        try {
            val userDetails = userDetailsService.getUserDetails(user)
            database.usersDao.insertUserDetails(userDetails.asDatabaseModel())
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    companion object {
        @Volatile
        private var instance: UserDetailsRepository? = null

        fun getInstance(
            userDetailsService: UserDetailsService,
            database: UsersDatabase
        ): UserDetailsRepository {
            return instance ?: synchronized(this) {
                instance ?: UserDetailsRepository(userDetailsService, database).also { instance = it }
            }
        }
    }
}