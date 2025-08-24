package com.qq7te.totalrecall.data

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Data class representing an exported entry for JSON serialization
 */
data class ExportEntry(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("text")
    val text: String,
    
    @SerializedName("photo_filename")
    val photoFilename: String,
    
    @SerializedName("timestamp")
    val timestamp: Long, // Unix timestamp in milliseconds
    
    @SerializedName("latitude")
    val latitude: Double?,
    
    @SerializedName("longitude")
    val longitude: Double?
)

/**
 * Root data class for the exported JSON file
 */
data class ExportData(
    @SerializedName("export_version")
    val exportVersion: String = "1.0",
    
    @SerializedName("export_timestamp")
    val exportTimestamp: Long = System.currentTimeMillis(),
    
    @SerializedName("app_version")
    val appVersion: String,
    
    @SerializedName("entries")
    val entries: List<ExportEntry>
)

/**
 * Extension function to convert Entry to ExportEntry with a unique photo filename
 */
fun Entry.toExportEntry(uniquePhotoFilename: String): ExportEntry {
    return ExportEntry(
        id = this.id,
        text = this.text,
        photoFilename = uniquePhotoFilename,
        timestamp = this.timestamp.time,
        latitude = this.latitude,
        longitude = this.longitude
    )
}
