package com.qq7te.totalrecall

import com.qq7te.totalrecall.data.Entry
import com.qq7te.totalrecall.data.EntryDao
import com.qq7te.totalrecall.data.EntryRepository
import com.qq7te.totalrecall.ui.capture.CaptureViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class CaptureViewModelTest {

    private class FakeEntryDao : EntryDao {
        val entries = mutableListOf<Entry>()
        private var nextId: Long = 1L

        override suspend fun insert(entry: Entry): Long {
            val id = nextId++
            val stored = entry.copy(id = id)
            entries.add(stored)
            return id
        }

        override fun getAllEntriesByTimestamp(): Flow<List<Entry>> =
            flow { emit(entries.sortedByDescending { it.timestamp }) }

        override fun searchEntries(searchQuery: String): Flow<List<Entry>> =
            flow { emit(entries.filter { it.text.contains(searchQuery) }) }

        override fun getEntryById(id: Long): Entry =
            entries.first { it.id == id }

        override suspend fun updateEntryText(id: Long, text: String): Int {
            val index = entries.indexOfFirst { it.id == id }
            if (index == -1) return 0
            entries[index] = entries[index].copy(text = text)
            return 1
        }

        override suspend fun delete(entry: Entry): Int {
            val removed = entries.removeIf { it.id == entry.id }
            return if (removed) 1 else 0
        }

        override suspend fun getAllEntriesForExport(): List<Entry> =
            entries.toList()

        override suspend fun clearAllEntries() {
            entries.clear()
        }
    }

    @Test
    fun `saveEntry with location stores latitude and longitude`() = runBlocking {
        val dao = FakeEntryDao()
        val repository = EntryRepository(dao)
        val viewModel = CaptureViewModel(repository)

        viewModel.saveEntry(
            text = "At the park",
            photoUri = "content://media/external/images/media/42",
            latitude = 37.7749,
            longitude = -122.4194
        )

        val saved = dao.entries.single()
        assertEquals("At the park", saved.text)
        assertEquals(37.7749, saved.latitude!!, 0.0001)
        assertEquals(-122.4194, saved.longitude!!, 0.0001)
        assertNotNull("Photo URI should be set", saved.photoPath)
    }

    @Test
    fun `saveEntry without location stores null lat and lng`() = runBlocking {
        val dao = FakeEntryDao()
        val repository = EntryRepository(dao)
        val viewModel = CaptureViewModel(repository)

        viewModel.saveEntry(
            text = "No location note",
            photoUri = "content://media/external/images/media/99",
            latitude = null,
            longitude = null
        )

        val saved = dao.entries.single()
        assertEquals("No location note", saved.text)
        assertNull("Latitude should be null when permission not granted", saved.latitude)
        assertNull("Longitude should be null when permission not granted", saved.longitude)
    }

    @Test
    fun `saveEntry multiple entries persist independently`() = runBlocking {
        val dao = FakeEntryDao()
        val repository = EntryRepository(dao)
        val viewModel = CaptureViewModel(repository)

        viewModel.saveEntry("First", "uri://1", 40.0, -74.0)
        viewModel.saveEntry("Second", "uri://2", null, null)
        viewModel.saveEntry("Third", "uri://3", 51.5, -0.12)

        assertEquals(3, dao.entries.size)
        assertEquals("First", dao.entries[0].text)
        assertEquals("Second", dao.entries[1].text)
        assertEquals("Third", dao.entries[2].text)
        assertEquals(40.0, dao.entries[0].latitude!!, 0.0001)
        assertEquals(-74.0, dao.entries[0].longitude!!, 0.0001)
        assertNull(dao.entries[1].latitude)
        assertNull(dao.entries[1].longitude)
        assertEquals(51.5, dao.entries[2].latitude!!, 0.0001)
        assertEquals(-0.12, dao.entries[2].longitude!!, 0.0001)
    }
}
