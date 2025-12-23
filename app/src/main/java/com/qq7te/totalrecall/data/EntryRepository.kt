package com.qq7te.totalrecall.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class EntryRepository(private val entryDao: EntryDao) {
    
    val allEntries: Flow<List<Entry>> = entryDao.getAllEntriesByTimestamp()
    
    suspend fun insert(entry: Entry): Long {
        return withContext(Dispatchers.IO) {
            entryDao.insert(entry)
        }
    }
    
    fun searchEntries(query: String): Flow<List<Entry>> {
        return entryDao.searchEntries(query)
    }
    
    suspend fun getEntryById(id: Long): Entry {
        return withContext(Dispatchers.IO) {
            entryDao.getEntryById(id)
        }
    }
    
    suspend fun updateEntryText(id: Long, text: String) {
        return withContext(Dispatchers.IO) {
            entryDao.updateEntryText(id, text)
        }
    }
    
    suspend fun delete(entry: Entry) {
        return withContext(Dispatchers.IO) {
            entryDao.delete(entry)
        }
    }
    
    suspend fun getAllEntriesForExport(): List<Entry> {
        return withContext(Dispatchers.IO) {
            entryDao.getAllEntriesForExport()
        }
    }
    
    suspend fun clearAllEntries() {
        return withContext(Dispatchers.IO) {
            entryDao.clearAllEntries()
        }
    }
} 