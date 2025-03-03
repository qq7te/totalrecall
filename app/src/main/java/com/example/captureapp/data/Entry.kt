package com.example.captureapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val photoPath: String,
    val timestamp: Date,
    val latitude: Double?,
    val longitude: Double?
) 