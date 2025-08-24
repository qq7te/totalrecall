package com.qq7te.totalrecall.import

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.google.gson.Gson
import com.qq7te.totalrecall.data.Entry
import com.qq7te.totalrecall.data.EntryRepository
import com.qq7te.totalrecall.data.ExportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipFile

/**
 * Service responsible for importing journal entries from zip files
 * Ensures imported photos become "native" content URIs identical to camera captures
 */
class ImportService(
    private val context: Context,
    private val repository: EntryRepository
) {
    
    private val gson = Gson()
    
    /**
     * Import result containing success status and details
     */
    data class ImportResult(
        val success: Boolean,
        val entriesImported: Int = 0,
        val photosImported: Int = 0,
        val error: String? = null
    )
    
    /**
     * Import options for handling existing data
     */
    enum class ImportMode {
        ADD,        // Add to existing entries
        OVERWRITE   // Clear database and import
    }
    
    /**
     * Imports entries from a ZIP file, making photos native content URIs
     * 
     * @param zipFile The ZIP file containing data.json and photos/
     * @param mode How to handle existing database entries
     * @return ImportResult with success status and counts
     */
    suspend fun importFromZip(zipFile: File, mode: ImportMode): ImportResult = withContext(Dispatchers.IO) {
        try {
            // Validate ZIP file structure
            val validationResult = validateZipStructure(zipFile)
            if (!validationResult.isValid) {
                return@withContext ImportResult(
                    success = false,
                    error = validationResult.error
                )
            }
            
            ZipFile(zipFile).use { zip ->
                // Extract and parse JSON data
                val exportData = extractAndParseJson(zip)
                    ?: return@withContext ImportResult(
                        success = false,
                        error = "Failed to parse data.json"
                    )
                
                // Clear database if overwrite mode
                if (mode == ImportMode.OVERWRITE) {
                    clearAllEntries()
                }
                
                var photosImported = 0
                val entriesToInsert = mutableListOf<Entry>()
                
                // Process each entry and import photos as native content URIs
                exportData.entries.forEach { exportEntry ->
                    val nativePhotoUri = if (exportEntry.photoFilename != "null.jpg") {
                        importPhotoAsNative(zip, exportEntry.photoFilename)?.also {
                            photosImported++
                        }
                    } else {
                        null
                    }
                    
                    // Create native Entry with proper content URI
                    val entry = Entry(
                        id = 0, // Auto-generate new ID
                        text = exportEntry.text,
                        photoPath = nativePhotoUri?.toString() ?: "",
                        timestamp = Date(exportEntry.timestamp),
                        latitude = exportEntry.latitude,
                        longitude = exportEntry.longitude
                    )
                    
                    entriesToInsert.add(entry)
                }
                
                // Insert all entries into database
                entriesToInsert.forEach { entry ->
                    repository.insert(entry)
                }
                
                ImportResult(
                    success = true,
                    entriesImported = entriesToInsert.size,
                    photosImported = photosImported
                )
            }
            
        } catch (e: Exception) {
            ImportResult(
                success = false,
                error = "Import failed: ${e.message}"
            )
        }
    }
    
    /**
     * Validates the ZIP file has the expected structure
     */
    private fun validateZipStructure(zipFile: File): ValidationResult {
        try {
            ZipFile(zipFile).use { zip ->
                val entries = zip.entries().toList()
                
                // Check for data.json
                val hasDataJson = entries.any { it.name == "data.json" }
                if (!hasDataJson) {
                    return ValidationResult(false, "Missing data.json file")
                }
                
                // Check for photos/ directory (optional - some exports may have no photos)
                // Note: Photos are optional, so we don't validate their presence
                
                return ValidationResult(true)
            }
        } catch (e: Exception) {
            return ValidationResult(false, "Invalid ZIP file: ${e.message}")
        }
    }
    
    /**
     * Extracts and parses the JSON data from the ZIP
     */
    private fun extractAndParseJson(zip: ZipFile): ExportData? {
        return try {
            val jsonEntry = zip.getEntry("data.json")
            zip.getInputStream(jsonEntry).use { inputStream ->
                val jsonText = inputStream.bufferedReader().readText()
                gson.fromJson(jsonText, ExportData::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Imports a photo from ZIP and saves it as a native content URI
     * This replicates the exact process used by the camera capture
     */
    private fun importPhotoAsNative(zip: ZipFile, photoFilename: String): Uri? {
        return try {
            val photoEntry = zip.getEntry("photos/$photoFilename") ?: return null
            
            // Generate timestamp-based name (same format as camera)
            val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)
                .format(System.currentTimeMillis())
            val displayName = "imported_$timestamp"
            
            // Create ContentValues (same as camera capture)
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            }
            
            // Insert into MediaStore (same as camera capture)
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return null
            
            // Copy photo data to the new MediaStore entry
            zip.getInputStream(photoEntry).use { inputStream ->
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            uri // Return native content URI
            
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Clears all entries from the database (for overwrite mode)
     */
    private suspend fun clearAllEntries() {
        repository.clearAllEntries()
    }
    
    /**
     * Data class for validation results
     */
    private data class ValidationResult(
        val isValid: Boolean,
        val error: String? = null
    )
}
