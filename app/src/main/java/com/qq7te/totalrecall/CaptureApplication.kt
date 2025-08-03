package com.qq7te.totalrecall

import android.app.Application
import com.qq7te.totalrecall.data.AppDatabase
import com.qq7te.totalrecall.data.EntryRepository

class CaptureApplication : Application() {
    
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { EntryRepository(database.entryDao()) }
} 