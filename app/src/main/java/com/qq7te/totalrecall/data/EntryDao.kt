package com.qq7te.totalrecall.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Insert
    suspend fun insert(entry: Entry): Long
    
    @Query("SELECT * FROM entries ORDER BY timestamp DESC")
    fun getAllEntriesByTimestamp(): Flow<List<Entry>>
    
    @Query("SELECT * FROM entries WHERE text LIKE '%' || :searchQuery || '%' ORDER BY timestamp DESC")
    fun searchEntries(searchQuery: String): Flow<List<Entry>>
    
    @Query("SELECT * FROM entries WHERE id = :id")
    fun getEntryById(id: Long): Entry
    
    @Delete
    suspend fun delete(entry: Entry): Int
    
    @Query("SELECT * FROM entries ORDER BY timestamp DESC")
    suspend fun getAllEntriesForExport(): List<Entry>
    
    @Query("DELETE FROM entries")
    suspend fun clearAllEntries()
} 