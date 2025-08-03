package com.qq7te.totalrecall.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qq7te.totalrecall.data.Entry
import com.qq7te.totalrecall.data.EntryRepository
import java.util.Date

class CaptureViewModel(private val repository: EntryRepository) : ViewModel() {
    
    suspend fun saveEntry(text: String, photoUri: String, latitude: Double?, longitude: Double?) {
        val entry = Entry(
            text = text,
            photoPath = photoUri,
            timestamp = Date(),
            latitude = latitude,
            longitude = longitude
        )
        repository.insert(entry)
    }
}

class CaptureViewModelFactory(private val repository: EntryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CaptureViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CaptureViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}