package com.example.myapplication.data.local.dao

import androidx.room.*
import com.example.myapplication.data.local.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY fullName")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: Long): Flow<User>

    @Query("SELECT * FROM users WHERE fullName = :fullName LIMIT 1")
    fun getUserByName(fullName: String): Flow<User>

    @Insert
    fun insertUser(user: User)

    @Update
    fun updateUser(user: User)

    @Delete
    fun deleteUser(user: User)

    @Query("DELETE FROM users WHERE id = :userId")
    fun deleteUserById(userId: Long)
}