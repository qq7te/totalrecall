package com.qq7te.totalrecall.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qq7te.totalrecall.data.Entry
import com.qq7te.totalrecall.data.EntryRepository
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: EntryRepository,
    private val entryId: Long
) : ViewModel() {
    
    private val _entry = MutableLiveData<Entry?>()
    val entry: LiveData<Entry?> = _entry
    
    private val _deleteResult = MutableLiveData<Boolean?>()
    val deleteResult: LiveData<Boolean?> = _deleteResult
    
    init {
        refreshEntry()
    }
    
    fun refreshEntry() {
        viewModelScope.launch {
            try {
                _entry.value = repository.getEntryById(entryId)
            } catch (e: Exception) {
                // Handle the case where the entry doesn't exist
                _entry.value = null
            }
        }
    }
    
    fun deleteEntry() {
        val currentEntry = _entry.value
        if (currentEntry != null) {
            viewModelScope.launch {
                try {
                    repository.delete(currentEntry)
                    _deleteResult.value = true
                } catch (e: Exception) {
                    _deleteResult.value = false
                }
            }
        } else {
            _deleteResult.value = false
        }
    }
}

class DetailViewModelFactory(
    private val repository: EntryRepository,
    private val entryId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(repository, entryId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}