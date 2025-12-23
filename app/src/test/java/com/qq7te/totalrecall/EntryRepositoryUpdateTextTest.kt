package com.qq7te.totalrecall

import com.qq7te.totalrecall.data.Entry
import com.qq7te.totalrecall.data.EntryDao
import com.qq7te.totalrecall.data.EntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.Date

/**
 * Tests for the text-only update path introduced for editing existing entries.
 *
 * These tests use a simple in-memory FakeEntryDao so we can verify that only the
 * text field is changed and all other fields remain untouched.
 */
class EntryRepositoryUpdateTextTest {

    private class FakeEntryDao : EntryDao {
        private val entries = mutableListOf<Entry>()
        private var nextId: Long = 1L

        override suspend fun insert(entry: Entry): Long {
            val id = if (entry.id == 0L) nextId++ else entry.id
            val stored = entry.copy(id = id)
            entries.removeAll { it.id == id }
            entries.add(stored)
            return id
        }

        override fun getAllEntriesByTimestamp(): Flow<List<Entry>> =
            flow { emit(entries.sortedByDescending { it.timestamp }) }

        override fun searchEntries(searchQuery: String): Flow<List<Entry>> =
            flow { emit(entries.filter { it.text.contains(searchQuery) }.sortedByDescending { it.timestamp }) }

        override fun getEntryById(id: Long): Entry =
            entries.first { it.id == id }

        override suspend fun updateEntryText(id: Long, text: String): Int {
            val index = entries.indexOfFirst { it.id == id }
            if (index == -1) return 0
            val existing = entries[index]
            entries[index] = existing.copy(text = text)
            return 1
        }

        override suspend fun delete(entry: Entry): Int {
            val removed = entries.removeIf { it.id == entry.id }
            return if (removed) 1 else 0
        }

        override suspend fun getAllEntriesForExport(): List<Entry> =
            entries.sortedByDescending { it.timestamp }

        override suspend fun clearAllEntries() {
            entries.clear()
        }
    }

    @Test
    fun `updateEntryText only changes text and keeps other fields intact`() = runBlocking {
        val dao = FakeEntryDao()
        val repository = EntryRepository(dao)

        val originalTimestamp = Date(1_756_052_557_311L)
        val originalEntry = Entry(
            id = 0L,
            text = "Original notes",
            photoPath = "content://media/external/images/media/1000000001",
            timestamp = originalTimestamp,
            latitude = 42.0,
            longitude = -71.0
        )

        val id = repository.insert(originalEntry)

        val newText = "Updated wine tasting notes"
        repository.updateEntryText(id, newText)

        val updated = repository.getEntryById(id)

        assertEquals("Text should be updated", newText, updated.text)
        assertEquals("Photo path should be unchanged", originalEntry.photoPath, updated.photoPath)
        assertEquals("Latitude should be unchanged", originalEntry.latitude, updated.latitude)
        assertEquals("Longitude should be unchanged", originalEntry.longitude, updated.longitude)
        assertEquals("Timestamp value should be unchanged", originalEntry.timestamp.time, updated.timestamp.time)
        // Same instance check for timestamp to ensure we didn't accidentally create a new Date in the DAO
        assertSame("Timestamp instance should be the same", originalTimestamp, updated.timestamp)
    }

    @Test
    fun `updateEntryText on missing id does not modify existing entries`() = runBlocking {
        val dao = FakeEntryDao()
        val repository = EntryRepository(dao)

        val entry = Entry(
            id = 0L,
            text = "Entry that should stay the same",
            photoPath = "content://media/external/images/media/1000000002",
            timestamp = Date(1_756_052_557_311L),
            latitude = null,
            longitude = null
        )

        val id = repository.insert(entry)

        // Attempt to update a non-existent entry id
        repository.updateEntryText(id + 1, "This should not be applied")

        val stillOriginal = repository.getEntryById(id)

        assertEquals("Text should be unchanged when updating a different id", entry.text, stillOriginal.text)
        assertEquals("Photo path should still match", entry.photoPath, stillOriginal.photoPath)
        assertEquals("Latitude should still match", entry.latitude, stillOriginal.latitude)
        assertEquals("Longitude should still match", entry.longitude, stillOriginal.longitude)
    }
}
