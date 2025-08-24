package com.qq7te.totalrecall.export

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.qq7te.totalrecall.BuildConfig
import com.qq7te.totalrecall.data.Entry
import com.qq7te.totalrecall.data.EntryRepository
import com.qq7te.totalrecall.data.ExportData
import com.qq7te.totalrecall.data.toExportEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Service responsible for exporting journal entries to a zip file
 */
class ExportService(
    private val context: Context,
    private val repository: EntryRepository
) {
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    /**
     * Export result containing the file path and any error message
     */
    data class ExportResult(
        val success: Boolean,
        val filePath: String? = null,
        val error: String? = null
    )
    
    /**
     * Exports all journal entries to a zip file containing JSON data and photos
     * 
     * @param outputFile The file where the zip should be created
     * @return ExportResult indicating success/failure and file path
     */
    suspend fun exportToZip(outputFile: File): ExportResult = withContext(Dispatchers.IO) {
        try {
            // Get all entries from the database
            val entries = repository.getAllEntriesForExport()
            
            if (entries.isEmpty()) {
                return@withContext ExportResult(
                    success = false,
                    error = "No entries to export"
                )
            }
            
            // Create a map to track unique photo filenames
            val photoFilenameMap = mutableMapOf<String, String>()
            val exportEntries = mutableListOf<com.qq7te.totalrecall.data.ExportEntry>()
            
            // Process each entry and generate unique photo filenames
            entries.forEach { entry ->
                val originalPhotoPath = entry.photoPath
                val uniqueFilename = generateUniquePhotoFilename(originalPhotoPath, photoFilenameMap)
                photoFilenameMap[originalPhotoPath] = uniqueFilename
                
                exportEntries.add(entry.toExportEntry(uniqueFilename))
            }
            
            // Create the export data structure
            val exportData = ExportData(
                appVersion = BuildConfig.VERSION_NAME,
                entries = exportEntries
            )
            
            // Create the JSON string
            val jsonData = gson.toJson(exportData)
            
            // Create the zip file
            ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
                // Add JSON data
                val jsonEntry = ZipEntry("data.json")
                zipOut.putNextEntry(jsonEntry)
                zipOut.write(jsonData.toByteArray())
                zipOut.closeEntry()
                
                // Add photos
                photoFilenameMap.forEach { (originalPath, uniqueFilename) ->
                    try {
                        // Handle content URI using ContentResolver
                        val uri = Uri.parse(originalPath)
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val photoEntry = ZipEntry("photos/$uniqueFilename")
                            zipOut.putNextEntry(photoEntry)
                            inputStream.copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                    } catch (e: Exception) {
                        // Log error but continue with other photos
                        // Could be that the content URI is no longer valid
                        println("Failed to export photo $originalPath: ${e.message}")
                    }
                }
            }
            
            ExportResult(
                success = true,
                filePath = outputFile.absolutePath
            )
            
        } catch (e: Exception) {
            ExportResult(
                success = false,
                error = "Export failed: ${e.message}"
            )
        }
    }
    
    /**
     * Generates a unique filename for a photo based on its content URI
     * Ensures no duplicates by using counter or UUID if necessary
     */
    private fun generateUniquePhotoFilename(
        originalPath: String,
        existingFilenames: Map<String, String>
    ): String {
        // If this path was already processed, return the same unique name
        existingFilenames[originalPath]?.let { return it }
        
        // Try to extract meaningful name from content URI
        val baseName = try {
            val uri = Uri.parse(originalPath)
            // Extract the last segment (usually the media ID)
            uri.lastPathSegment?.let { segment ->
                // Remove any non-alphanumeric characters for filename safety
                segment.replace("[^a-zA-Z0-9]".toRegex(), "")
            } ?: "photo"
        } catch (e: Exception) {
            "photo"
        }
        
        val extension = "jpg" // Default to jpg for photos
        
        // Try the base filename first
        var candidateName = "${baseName}.${extension}"
        var counter = 1
        
        // Check if this filename is already used by another photo
        while (existingFilenames.values.contains(candidateName)) {
            candidateName = "${baseName}_${counter}.${extension}"
            counter++
        }
        
        // If we still have conflicts after reasonable attempts, use UUID
        if (counter > 100) {
            candidateName = "${UUID.randomUUID()}.${extension}"
        }
        
        return candidateName
    }
    
    /**
     * Creates a default export filename with timestamp
     */
    fun createDefaultExportFilename(): String {
        val timestamp = System.currentTimeMillis()
        return "photojournal_export_${timestamp}.zip"
    }
}
