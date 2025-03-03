package com.example.captureapp

import android.app.Application
import com.example.captureapp.data.AppDatabase
import com.example.captureapp.data.EntryRepository

class CaptureApplication : Application() {
    
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { EntryRepository(database.entryDao()) }
} 