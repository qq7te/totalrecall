package com.example.captureapp.ui.browse

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.captureapp.data.Entry
import com.example.captureapp.data.EntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class BrowseViewModel(private val repository: EntryRepository) : ViewModel() {
    
    private val searchQuery = MutableStateFlow("")
    
    val entries: LiveData<List<Entry>> = searchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.allEntries
            } else {
                repository.searchEntries(query)
            }
        }
        .asLiveData()
    
    fun searchEntries(query: String) {
        searchQuery.value = query
    }
}

class BrowseViewModelFactory(private val repository: EntryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BrowseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}