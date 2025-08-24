package com.qq7te.totalrecall.ui.browse

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.Toast
import android.os.Environment
import com.qq7te.totalrecall.R
import com.qq7te.totalrecall.TotalRecallApplication
import com.qq7te.totalrecall.databinding.FragmentBrowseBinding
import com.qq7te.totalrecall.export.ExportService
import com.qq7te.totalrecall.import.ImportService
import java.io.File
import java.io.FileOutputStream

class BrowseFragment : Fragment() {
    
    private var _binding: FragmentBrowseBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: BrowseViewModel by viewModels {
        BrowseViewModelFactory((requireActivity().application as TotalRecallApplication).repository)
    }
    
    private lateinit var adapter: EntryAdapter
    
    // File picker for import functionality
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowseBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupOptionsMenu()
        setupFilePickerLauncher()
        setupRecyclerView()
        setupSearch()
        observeEntries()
        observeExportState()
        observeImportState()
    }
    
    /**
     * Setup the options menu (three-dot menu) with import/export actions
     */
    private fun setupOptionsMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.browse_menu, menu)
            }
            
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_export -> {
                        startExport()
                        true
                    }
                    R.id.action_import -> {
                        startImport()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
    
    /**
     * Setup file picker launcher for importing ZIP files
     */
    private fun setupFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    handleImportFile(uri)
                }
            }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = EntryAdapter { entryId ->
            val action = BrowseFragmentDirections.actionBrowseToDetail(entryId)
            findNavController().navigate(action)
        }
        
        binding.entriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@BrowseFragment.adapter
        }
    }
    
    private fun setupSearch() {
        binding.searchView.isFocusable = true
        binding.searchView.onActionViewExpanded()
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchEntries(newText ?: "")
                return true
            }
        })
    }
    
    private fun observeEntries() {
        viewModel.entries.observe(viewLifecycleOwner) { entries ->
            adapter.submitList(entries)
            binding.emptyView.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    private fun observeExportState() {
        viewModel.exportState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BrowseViewModel.ExportState.Success -> {
                    Toast.makeText(requireContext(), "Export completed: ${state.filePath}", Toast.LENGTH_LONG).show()
                    viewModel.resetExportState()
                }
                is BrowseViewModel.ExportState.Error -> {
                    Toast.makeText(requireContext(), "Export failed: ${state.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetExportState()
                }
                is BrowseViewModel.ExportState.InProgress -> {
                    Toast.makeText(requireContext(), "Exporting...", Toast.LENGTH_SHORT).show()
                }
                is BrowseViewModel.ExportState.Idle -> {
                    // No action needed
                }
            }
        }
    }
    
    private fun observeImportState() {
        viewModel.importState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BrowseViewModel.ImportState.Success -> {
                    Toast.makeText(
                        requireContext(), 
                        "Import completed: ${state.entriesImported} entries, ${state.photosImported} photos",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.resetImportState()
                }
                is BrowseViewModel.ImportState.Error -> {
                    Toast.makeText(requireContext(), "Import failed: ${state.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetImportState()
                }
                is BrowseViewModel.ImportState.InProgress -> {
                    Toast.makeText(requireContext(), "Importing...", Toast.LENGTH_SHORT).show()
                }
                is BrowseViewModel.ImportState.Idle -> {
                    // No action needed
                }
            }
        }
    }
    
    /**
     * Start export process
     */
    private fun startExport() {
        try {
            val repository = (requireActivity().application as TotalRecallApplication).repository
            val exportService = ExportService(requireContext(), repository)
            
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val filename = exportService.createDefaultExportFilename()
            val outputFile = File(downloadsDir, filename)
            
            viewModel.exportEntries(exportService, outputFile)
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to start export: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Start import process - show file picker
     */
    private fun startImport() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/zip"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        
        try {
            filePickerLauncher.launch(Intent.createChooser(intent, "Select ZIP file to import"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No file manager available", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Handle selected import file
     */
    private fun handleImportFile(uri: Uri) {
        try {
            // Copy URI content to temporary file
            val tempFile = File(requireContext().cacheDir, "import_temp.zip")
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            // Check if database has entries to determine import mode
            viewModel.entries.value?.let { currentEntries ->
                if (currentEntries.isNotEmpty()) {
                    showImportModeDialog(tempFile)
                } else {
                    // Empty database - import directly
                    performImport(tempFile, ImportService.ImportMode.ADD)
                }
            } ?: run {
                // No entries observed yet - assume empty and import
                performImport(tempFile, ImportService.ImportMode.ADD)
            }
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to read import file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Show dialog to choose import mode (add or overwrite)
     */
    private fun showImportModeDialog(tempFile: File) {
        AlertDialog.Builder(requireContext())
            .setTitle("Import Mode")
            .setMessage("You have existing entries. How would you like to import?")
            .setPositiveButton("Add to existing") { _, _ ->
                performImport(tempFile, ImportService.ImportMode.ADD)
            }
            .setNegativeButton("Replace all") { _, _ ->
                performImport(tempFile, ImportService.ImportMode.OVERWRITE)
            }
            .setNeutralButton("Cancel", null)
            .show()
    }
    
    /**
     * Perform the actual import
     */
    private fun performImport(tempFile: File, mode: ImportService.ImportMode) {
        try {
            val repository = (requireActivity().application as TotalRecallApplication).repository
            val importService = ImportService(requireContext(), repository)
            
            viewModel.importEntries(importService, tempFile, mode)
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to start import: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
