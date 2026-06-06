package com.titanbbl.funny.face.filter.game.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.titanbbl.funny.face.filter.game.data.database.UsersDatabase
import com.titanbbl.funny.face.filter.game.data.database.asDomainModel
import com.titanbbl.funny.face.filter.game.data.domain.UserListItem
import com.titanbbl.funny.face.filter.game.data.network.UserListService
import com.titanbbl.funny.face.filter.game.data.network.model.asDatabaseModel
import timber.log.Timber

class UserListRepository(
    private val userListService: UserListService,
    private val database: UsersDatabase,
) {

    val users: LiveData<List<UserListItem>> =
        database.usersDao.getDatabaseUsers().map {
            it.asDomainModel()
        }

    suspend fun refreshUserList() {
        try {
            val users = userListService.getUserList()
            database.usersDao.insertAll(users.asDatabaseModel())
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    companion object {
        @Volatile
        private var instance: UserListRepository? = null

        fun getInstance(
            userListService: UserListService,
            database: UsersDatabase
        ): UserListRepository {
            return instance ?: synchronized(this) {
                instance ?: UserListRepository(userListService, database).also { instance = it }
            }
        }
    }
}