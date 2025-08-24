package com.qq7te.totalrecall.ui.browse

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.qq7te.totalrecall.data.Entry
import com.qq7te.totalrecall.data.EntryRepository
import com.qq7te.totalrecall.export.ExportService
import com.qq7te.totalrecall.import.ImportService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
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
    
    // Export state
    private val _exportState = MutableLiveData<ExportState>(ExportState.Idle)
    val exportState: LiveData<ExportState> = _exportState
    
    // Import state
    private val _importState = MutableLiveData<ImportState>(ImportState.Idle)
    val importState: LiveData<ImportState> = _importState
    
    fun searchEntries(query: String) {
        searchQuery.value = query
    }
    
    /**
     * Export all entries to a zip file
     */
    fun exportEntries(exportService: ExportService, outputFile: File) {
        viewModelScope.launch {
            _exportState.value = ExportState.InProgress
            
            val result = exportService.exportToZip(outputFile)
            
            _exportState.value = if (result.success) {
                ExportState.Success(result.filePath ?: "")
            } else {
                ExportState.Error(result.error ?: "Unknown error")
            }
        }
    }
    
    /**
     * Reset export state to idle
     */
    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }
    
    /**
     * Import entries from a zip file
     */
    fun importEntries(importService: ImportService, zipFile: File, mode: ImportService.ImportMode) {
        viewModelScope.launch {
            _importState.value = ImportState.InProgress
            
            val result = importService.importFromZip(zipFile, mode)
            
            _importState.value = if (result.success) {
                ImportState.Success(result.entriesImported, result.photosImported)
            } else {
                ImportState.Error(result.error ?: "Unknown error")
            }
        }
    }
    
    /**
     * Reset import state to idle
     */
    fun resetImportState() {
        _importState.value = ImportState.Idle
    }
    
    /**
     * Sealed class representing export states
     */
    sealed class ExportState {
        object Idle : ExportState()
        object InProgress : ExportState()
        data class Success(val filePath: String) : ExportState()
        data class Error(val message: String) : ExportState()
    }
    
    /**
     * Sealed class representing import states
     */
    sealed class ImportState {
        object Idle : ImportState()
        object InProgress : ImportState()
        data class Success(val entriesImported: Int, val photosImported: Int) : ImportState()
        data class Error(val message: String) : ImportState()
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