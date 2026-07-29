package com.nmorrione.divemeter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiveDao {
    @Insert
    suspend fun insert(dive: Dive): Long

    @Query("SELECT * FROM dives ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<Dive>>

    @Query("SELECT * FROM dives ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun getMostRecent(): Dive?

    @Query("SELECT * FROM dives WHERE spotName LIKE '%' || :query || '%' ORDER BY timestampMillis DESC")
    fun search(query: String): Flow<List<Dive>>
}
