package com.qq7te.totalrecall

import com.google.gson.Gson
import com.qq7te.totalrecall.data.ExportData
import com.qq7te.totalrecall.data.ExportEntry
import org.junit.Test
import org.junit.Assert.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Simple unit tests for export/import service logic
 * Tests the core functionality that we validated manually
 */
class ExportImportServiceLogicTest {
    
    @get:Rule
    val tempFolder = TemporaryFolder()
    
    private val gson = Gson()
    
    @Test
    fun `test export ZIP structure matches our manual testing`() {
        // Create export data similar to your wine journal
        val wineEntries = listOf(
            ExportEntry(
                id = 10,
                text = "terribile \nmild pecorino with truffles. \nnot a pecorino, barely truffely\n",
                photoFilename = "1000000255.jpg",
                timestamp = 1755813502803,
                latitude = 42.42302267,
                longitude = -71.19617533
            ),
            ExportEntry(
                id = 9,
                text = "stella 14\nbubbly red\nnot good\nsour, nothing interesting really ",
                photoFilename = "1000000244.jpg",
                timestamp = 1755732831390,
                latitude = 42.42299238,
                longitude = -71.19622105
            ),
            ExportEntry(
                id = 8,
                text = "delicious sparkling rosé\nlandhaus mayer\n",
                photoFilename = "1000000243.jpg",
                timestamp = 1755729432550,
                latitude = 42.42299238,
                longitude = -71.19622105
            )
        )
        
        val exportData = ExportData(
            appVersion = "0.2",
            entries = wineEntries
        )
        
        // Create ZIP file like our export service does
        val zipFile = tempFolder.newFile("wine_journal_test.zip")
        createTestZip(zipFile, exportData, wineEntries)
        
        // Validate ZIP structure matches what we tested manually
        validateZipMatchesManualTest(zipFile, wineEntries.size)
    }
    
    @Test 
    fun `test import ZIP validation logic`() {
        // Test 1: Valid ZIP with data.json should pass validation
        val validZip = createValidTestZip()
        assertTrue("Valid ZIP should pass validation", isValidZipStructure(validZip))
        
        // Test 2: ZIP without data.json should fail validation
        val invalidZip = createZipWithoutDataJson()
        assertFalse("ZIP without data.json should fail validation", isValidZipStructure(invalidZip))
        
        // Test 3: Corrupted file should fail validation
        val corruptedZip = tempFolder.newFile("corrupted.zip")
        corruptedZip.writeText("This is not a ZIP file")
        assertFalse("Corrupted file should fail validation", isValidZipStructure(corruptedZip))
    }
    
    @Test
    fun `test JSON parsing handles real wine journal data`() {
        val realWineJsonData = """{
            "app_version": "0.2",
            "entries": [
                {
                    "id": 11,
                    "latitude": 42.42313134,
                    "longitude": -71.19624307,
                    "photo_filename": "1000000256.jpg",
                    "text": "at least this works\n",
                    "timestamp": 1756052557311
                },
                {
                    "id": 10,
                    "latitude": 42.42302267,
                    "longitude": -71.19617533,
                    "photo_filename": "1000000255.jpg",
                    "text": "terribile \nmild pecorino with truffles. \nnot a pecorino, barely truffely\n",
                    "timestamp": 1755813502803
                }
            ],
            "export_timestamp": 1756054738865,
            "export_version": "1.0"
        }""".trimIndent()
        
        // Parse the JSON (this is what ImportService does)
        val parsedData = gson.fromJson(realWineJsonData, ExportData::class.java)
        
        assertNotNull("Parsed data should not be null", parsedData)
        assertEquals("Should have correct app version", "0.2", parsedData.appVersion)
        assertEquals("Should have 2 entries", 2, parsedData.entries.size)
        
        val firstEntry = parsedData.entries[0]
        assertEquals("First entry ID should be 11", 11, firstEntry.id)
        assertEquals("First entry should have correct text", "at least this works\n", firstEntry.text)
        assertEquals("First entry should have photo", "1000000256.jpg", firstEntry.photoFilename)
        assertEquals("Latitude should be precise", 42.42313134, firstEntry.latitude!!, 0.00000001)
        
        val secondEntry = parsedData.entries[1]
        assertTrue("Second entry should contain wine review", secondEntry.text.contains("pecorino"))
        assertEquals("Second entry photo should be correct", "1000000255.jpg", secondEntry.photoFilename)
    }
    
    @Test
    fun `test photo filename generation logic reproduces our results`() {
        // Test the unique filename generation that our export used
        val originalPaths = listOf(
            "content://media/external/images/media/1000000256",
            "content://media/external/images/media/1000000255", 
            "content://media/external/images/media/1000000244"
        )
        
        val generatedFilenames = mutableSetOf<String>()
        originalPaths.forEach { path ->
            val filename = generatePhotoFilename(path, generatedFilenames)
            generatedFilenames.add(filename)
        }
        
        assertEquals("Should generate 3 unique filenames", 3, generatedFilenames.size)
        generatedFilenames.forEach { filename ->
            assertTrue("All filenames should end with .jpg", filename.endsWith(".jpg"))
            assertTrue("Filename should contain digits", filename.matches(Regex(".*\\d+.*\\.jpg")))
        }
    }
    
    @Test
    fun `test round trip data integrity like our manual test`() {
        // Create original data
        val originalEntries = createWineJournalTestData()
        val exportData = ExportData(appVersion = "0.2", entries = originalEntries)
        
        // Export to ZIP (simulating ExportService)
        val zipFile = tempFolder.newFile("round_trip_test.zip")
        createTestZip(zipFile, exportData, originalEntries)
        
        // Import from ZIP (simulating ImportService)
        val importedData = extractDataFromZip(zipFile)
        
        // Verify data integrity
        assertEquals("Entry count should match", originalEntries.size, importedData.entries.size)
        assertEquals("App version should match", exportData.appVersion, importedData.appVersion)
        
        originalEntries.forEachIndexed { index, original ->
            val imported = importedData.entries[index]
            assertEquals("Entry ID should match", original.id, imported.id)
            assertEquals("Text should be identical", original.text, imported.text)
            assertEquals("Photo filename should match", original.photoFilename, imported.photoFilename)
            assertEquals("Timestamp should be exact", original.timestamp, imported.timestamp)
            assertEquals("Latitude should match", original.latitude, imported.latitude)
            assertEquals("Longitude should match", original.longitude, imported.longitude)
        }
    }
    
    /**
     * Create test ZIP file similar to our export service
     */
    private fun createTestZip(zipFile: File, exportData: ExportData, entries: List<ExportEntry>) {
        val jsonData = gson.toJson(exportData)
        
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // Add data.json
            val jsonEntry = ZipEntry("data.json")
            zos.putNextEntry(jsonEntry)
            zos.write(jsonData.toByteArray())
            zos.closeEntry()
            
            // Add dummy photos for entries that have them
            entries.forEach { entry ->
                if (entry.photoFilename != "null.jpg") {
                    val photoEntry = ZipEntry("photos/${entry.photoFilename}")
                    zos.putNextEntry(photoEntry)
                    zos.write("dummy photo data for ${entry.photoFilename}".toByteArray())
                    zos.closeEntry()
                }
            }
        }
    }
    
    /**
     * Validate ZIP structure like ImportService does
     */
    private fun isValidZipStructure(zipFile: File): Boolean {
        return try {
            ZipFile(zipFile).use { zip ->
                val entries = zip.entries().toList()
                entries.any { it.name == "data.json" }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Create valid test ZIP
     */
    private fun createValidTestZip(): File {
        val zipFile = tempFolder.newFile("valid_test.zip")
        val testData = ExportData(appVersion = "test", entries = emptyList())
        createTestZip(zipFile, testData, emptyList())
        return zipFile
    }
    
    /**
     * Create ZIP without data.json
     */
    private fun createZipWithoutDataJson(): File {
        val zipFile = tempFolder.newFile("no_data.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            val entry = ZipEntry("dummy.txt")
            zos.putNextEntry(entry)
            zos.write("dummy content".toByteArray())
            zos.closeEntry()
        }
        return zipFile
    }
    
    /**
     * Extract data from ZIP like ImportService does
     */
    private fun extractDataFromZip(zipFile: File): ExportData {
        return ZipFile(zipFile).use { zip ->
            val jsonEntry = zip.getEntry("data.json")
            val jsonText = zip.getInputStream(jsonEntry).bufferedReader().readText()
            gson.fromJson(jsonText, ExportData::class.java)
        }
    }
    
    /**
     * Validate ZIP matches our manual testing structure
     */
    private fun validateZipMatchesManualTest(zipFile: File, expectedEntryCount: Int) {
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries().toList()
            
            // Should have data.json like our real export
            assertTrue("Should contain data.json", entries.any { it.name == "data.json" })
            
            // Should have photos/ directory with correct structure
            val photoEntries = entries.filter { it.name.startsWith("photos/") && !it.isDirectory }
            assertEquals("Should have correct number of photos", expectedEntryCount, photoEntries.size)
            
            // All photos should be JPEGs like our real export
            photoEntries.forEach { entry ->
                assertTrue("Photo should be JPEG: ${entry.name}", entry.name.endsWith(".jpg"))
            }
            
            // JSON should be parseable
            val jsonEntry = zip.getEntry("data.json")
            val jsonContent = zip.getInputStream(jsonEntry).bufferedReader().readText()
            val parsedData = gson.fromJson(jsonContent, ExportData::class.java)
            assertEquals("Should parse correct number of entries", expectedEntryCount, parsedData.entries.size)
        }
    }
    
    /**
     * Generate photo filename like our export service
     */
    private fun generatePhotoFilename(originalPath: String, existingFilenames: Set<String>): String {
        val baseName = originalPath.substringAfterLast("/")
        val extension = "jpg"
        
        var candidateName = "$baseName.$extension"
        var counter = 1
        
        while (existingFilenames.contains(candidateName)) {
            candidateName = "${baseName}_${counter}.$extension"
            counter++
        }
        
        return candidateName
    }
    
    /**
     * Create wine journal test data based on our real data
     */
    private fun createWineJournalTestData(): List<ExportEntry> {
        return listOf(
            ExportEntry(
                id = 10,
                text = "terribile \nmild pecorino with truffles. \nnot a pecorino, barely truffely\n",
                photoFilename = "1000000255.jpg",
                timestamp = 1755813502803,
                latitude = 42.42302267,
                longitude = -71.19617533
            ),
            ExportEntry(
                id = 9,
                text = "stella 14\nbubbly red\nnot good\nsour, nothing interesting really ",
                photoFilename = "1000000244.jpg", 
                timestamp = 1755732831390,
                latitude = 42.42299238,
                longitude = -71.19622105
            ),
            ExportEntry(
                id = 8,
                text = "delicious sparkling rosé\nlandhaus mayer\n",
                photoFilename = "1000000243.jpg",
                timestamp = 1755729432550,
                latitude = 42.42299238,
                longitude = -71.19622105
            )
        )
    }
}
