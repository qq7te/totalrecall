package com.qq7te.totalrecall

import android.app.Application
import com.qq7te.totalrecall.data.AppDatabase
import com.qq7te.totalrecall.data.EntryRepository

/**
 * Application class for the TotalRecall app.
 * Provides lazy-initialised access to the Room database and repository.
 */
class TotalRecallApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { EntryRepository(database.entryDao()) }
}

