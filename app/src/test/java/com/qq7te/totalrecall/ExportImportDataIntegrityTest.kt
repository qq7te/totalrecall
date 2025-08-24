package com.qq7te.totalrecall

import com.google.gson.Gson
import com.qq7te.totalrecall.data.Entry
import com.qq7te.totalrecall.data.ExportData
import com.qq7te.totalrecall.data.ExportEntry
import com.qq7te.totalrecall.data.toExportEntry
import org.junit.Test
import org.junit.Assert.*
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import org.junit.Rule
import org.junit.rules.TemporaryFolder

/**
 * Tests focused on data integrity and serialization without Android dependencies
 * Validates that export/import preserves all data correctly
 */
class ExportImportDataIntegrityTest {
    
    @get:Rule
    val tempFolder = TemporaryFolder()
    
    private val gson = Gson()
    
    @Test
    fun `test Entry to ExportEntry conversion preserves all data`() {
        val originalEntry = Entry(
            id = 123,
            text = "Wine tasting notes\nDelicious Chianti from 2019\nPerfect with cheese",
            photoPath = "content://media/external/images/media/456",
            timestamp = Date(1640995200000), // Jan 1, 2022
            latitude = 43.7711,
            longitude = 11.2486
        )
        
        val exportEntry = originalEntry.toExportEntry("chianti_2019.jpg")
        
        assertEquals("ID should be preserved", originalEntry.id, exportEntry.id)
        assertEquals("Text should be preserved", originalEntry.text, exportEntry.text)
        assertEquals("Photo filename should be set", "chianti_2019.jpg", exportEntry.photoFilename)
        assertEquals("Timestamp should be preserved", originalEntry.timestamp.time, exportEntry.timestamp)
        assertEquals("Latitude should be preserved", originalEntry.latitude, exportEntry.latitude)
        assertEquals("Longitude should be preserved", originalEntry.longitude, exportEntry.longitude)
    }
    
    @Test
    fun `test ExportData JSON serialization and deserialization`() {
        val originalEntries = createTestExportEntries()
        val exportData = ExportData(
            appVersion = "1.0",
            entries = originalEntries
        )
        
        // Serialize to JSON
        val jsonString = gson.toJson(exportData)
        
        // Verify JSON contains expected fields
        assertTrue("JSON should contain app_version", jsonString.contains("\"app_version\":\"1.0\""))
        assertTrue("JSON should contain export_version", jsonString.contains("\"export_version\":\"1.0\""))
        assertTrue("JSON should contain entries array", jsonString.contains("\"entries\":["))
        assertTrue("JSON should contain export_timestamp", jsonString.contains("\"export_timestamp\":"))
        
        // Deserialize back from JSON
        val deserializedData = gson.fromJson(jsonString, ExportData::class.java)
        
        assertEquals("App version should match", exportData.appVersion, deserializedData.appVersion)
        assertEquals("Export version should match", exportData.exportVersion, deserializedData.exportVersion)
        assertEquals("Number of entries should match", originalEntries.size, deserializedData.entries.size)
        
        // Verify each entry's data integrity
        originalEntries.forEachIndexed { index, originalEntry ->
            val deserializedEntry = deserializedData.entries[index]
            assertEquals("Entry ID should match", originalEntry.id, deserializedEntry.id)
            assertEquals("Entry text should match", originalEntry.text, deserializedEntry.text)
            assertEquals("Photo filename should match", originalEntry.photoFilename, deserializedEntry.photoFilename)
            assertEquals("Timestamp should match", originalEntry.timestamp, deserializedEntry.timestamp)
            assertEquals("Latitude should match", originalEntry.latitude, deserializedEntry.latitude)
            assertEquals("Longitude should match", originalEntry.longitude, deserializedEntry.longitude)
        }
    }
    
    @Test
    fun `test ZIP file creation and structure validation`() {
        val exportEntries = createTestExportEntries()
        val exportData = ExportData(appVersion = "test", entries = exportEntries)
        val jsonData = gson.toJson(exportData)
        
        // Create ZIP file with proper structure
        val zipFile = tempFolder.newFile("test_structure.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // Add data.json
            val jsonEntry = ZipEntry("data.json")
            zos.putNextEntry(jsonEntry)
            zos.write(jsonData.toByteArray())
            zos.closeEntry()
            
            // Add dummy photos
            val photoData = "dummy photo content".toByteArray()
            exportEntries.forEach { entry ->
                if (entry.photoFilename != "null.jpg") {
                    val photoEntry = ZipEntry("photos/${entry.photoFilename}")
                    zos.putNextEntry(photoEntry)
                    zos.write(photoData)
                    zos.closeEntry()
                }
            }
        }
        
        // Validate ZIP structure
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries().toList()
            
            // Check data.json exists
            val jsonEntry = entries.find { it.name == "data.json" }
            assertNotNull("data.json should exist", jsonEntry)
            assertFalse("data.json should not be a directory", jsonEntry!!.isDirectory)
            assertTrue("data.json should have content", jsonEntry.size > 0)
            
            // Check photos directory structure
            val photoEntries = entries.filter { it.name.startsWith("photos/") && !it.isDirectory }
            val expectedPhotoCount = exportEntries.count { it.photoFilename != "null.jpg" }
            assertEquals("Should have correct number of photos", expectedPhotoCount, photoEntries.size)
            
            // Verify all photos are in photos/ subdirectory
            photoEntries.forEach { entry ->
                assertTrue("Photo should be in photos/ directory", entry.name.startsWith("photos/"))
                assertTrue("Photo should be JPEG", entry.name.endsWith(".jpg"))
                assertFalse("Photo should not be directory", entry.isDirectory)
                assertTrue("Photo should have content", entry.size > 0)
            }
            
            // Verify JSON content can be read back
            val jsonContent = zip.getInputStream(entries.find { it.name == "data.json" }!!).bufferedReader().readText()
            val parsedData = gson.fromJson(jsonContent, ExportData::class.java)
            assertEquals("Parsed entries should match original", exportEntries.size, parsedData.entries.size)
        }
    }
    
    @Test
    fun `test handling entries without photos`() {
        val entriesWithAndWithoutPhotos = listOf(
            ExportEntry(
                id = 1,
                text = "Entry with photo",
                photoFilename = "photo1.jpg",
                timestamp = System.currentTimeMillis(),
                latitude = 42.0,
                longitude = -71.0
            ),
            ExportEntry(
                id = 2, 
                text = "Entry without photo",
                photoFilename = "null.jpg", // No photo marker
                timestamp = System.currentTimeMillis(),
                latitude = null,
                longitude = null
            )
        )
        
        // Test JSON serialization handles null.jpg correctly
        val exportData = ExportData(appVersion = "test", entries = entriesWithAndWithoutPhotos)
        val jsonString = gson.toJson(exportData)
        val deserializedData = gson.fromJson(jsonString, ExportData::class.java)
        
        assertEquals("Should have 2 entries", 2, deserializedData.entries.size)
        assertEquals("First entry should have photo", "photo1.jpg", deserializedData.entries[0].photoFilename)
        assertEquals("Second entry should have null.jpg", "null.jpg", deserializedData.entries[1].photoFilename)
        assertNull("Entry without photo should have null latitude", deserializedData.entries[1].latitude)
        assertNull("Entry without photo should have null longitude", deserializedData.entries[1].longitude)
    }
    
    @Test
    fun `test timestamp precision preservation`() {
        val preciseTimestamp = 1756052557311L // Exact millisecond precision
        val entry = ExportEntry(
            id = 1,
            text = "Timestamp test",
            photoFilename = "test.jpg", 
            timestamp = preciseTimestamp,
            latitude = 42.123456789,
            longitude = -71.987654321
        )
        
        val exportData = ExportData(appVersion = "test", entries = listOf(entry))
        val jsonString = gson.toJson(exportData)
        val deserializedData = gson.fromJson(jsonString, ExportData::class.java)
        
        val deserializedEntry = deserializedData.entries[0]
        assertEquals("Timestamp precision should be preserved", preciseTimestamp, deserializedEntry.timestamp)
        assertEquals("Latitude precision should be preserved", 42.123456789, deserializedEntry.latitude!!, 0.000000001)
        assertEquals("Longitude precision should be preserved", -71.987654321, deserializedEntry.longitude!!, 0.000000001)
    }
    
    @Test
    fun `test special characters in text content`() {
        val textWithSpecialChars = """Wine tasting: "Château Margaux" 2010
        Notes:
        - Tannic & bold
        - Pairs w/ cheese 🧀
        - Price: €85/bottle
        - Rating: ★★★★★
        Unicode test: 中文 العربية русский""".trimIndent()
        
        val entry = ExportEntry(
            id = 1,
            text = textWithSpecialChars,
            photoFilename = "special_chars.jpg",
            timestamp = System.currentTimeMillis(),
            latitude = 48.8566,
            longitude = 2.3522
        )
        
        val exportData = ExportData(appVersion = "test", entries = listOf(entry))
        val jsonString = gson.toJson(exportData)
        val deserializedData = gson.fromJson(jsonString, ExportData::class.java)
        
        val deserializedText = deserializedData.entries[0].text
        assertEquals("Special characters should be preserved", textWithSpecialChars, deserializedText)
        assertTrue("Should contain emoji", deserializedText.contains("🧀"))
        assertTrue("Should contain Unicode", deserializedText.contains("中文"))
        assertTrue("Should contain quotes", deserializedText.contains("\"Château Margaux\""))
    }
    
    /**
     * Create test export entries similar to real data
     */
    private fun createTestExportEntries(): List<ExportEntry> {
        return listOf(
            ExportEntry(
                id = 1,
                text = "Terribile mild pecorino with truffles.\nnot a pecorino, barely truffely",
                photoFilename = "1000000255.jpg",
                timestamp = 1755813502803,
                latitude = 42.42302267,
                longitude = -71.19617533
            ),
            ExportEntry(
                id = 2,
                text = "Delicious sparkling rosé\nLandhaus Mayer",
                photoFilename = "1000000243.jpg", 
                timestamp = 1755729432550,
                latitude = 42.42299238,
                longitude = -71.19622105
            ),
            ExportEntry(
                id = 3,
                text = "Entry without photo\nJust text notes",
                photoFilename = "null.jpg", // No photo
                timestamp = System.currentTimeMillis(),
                latitude = null,
                longitude = null
            )
        )
    }
}
