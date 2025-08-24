package com.qq7te.totalrecall.ui.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.Toast
import android.os.Environment
import com.qq7te.totalrecall.TotalRecallApplication
import com.qq7te.totalrecall.databinding.FragmentBrowseBinding
import com.qq7te.totalrecall.export.ExportService
import java.io.File

class BrowseFragment : Fragment() {
    
    private var _binding: FragmentBrowseBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: BrowseViewModel by viewModels {
        BrowseViewModelFactory((requireActivity().application as TotalRecallApplication).repository)
    }
    
    private lateinit var adapter: EntryAdapter
    
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
        
        setupRecyclerView()
        setupSearch()
        setupExportButton()
        observeEntries()
        observeExportState()
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
    
    private fun setupExportButton() {
        binding.exportButton.setOnClickListener {
            startExport()
        }
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
                is BrowseViewModel.ExportState.Idle -> {
                    binding.exportButton.isEnabled = true
                    binding.exportButton.text = "Export"
                }
                is BrowseViewModel.ExportState.InProgress -> {
                    binding.exportButton.isEnabled = false
                    binding.exportButton.text = "Exporting..."
                }
                is BrowseViewModel.ExportState.Success -> {
                    binding.exportButton.isEnabled = true
                    binding.exportButton.text = "Export"
                    Toast.makeText(requireContext(), "Export completed: ${state.filePath}", Toast.LENGTH_LONG).show()
                    viewModel.resetExportState()
                }
                is BrowseViewModel.ExportState.Error -> {
                    binding.exportButton.isEnabled = true
                    binding.exportButton.text = "Export"
                    Toast.makeText(requireContext(), "Export failed: ${state.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetExportState()
                }
            }
        }
    }
    
    private fun startExport() {
        try {
            // Create export service
            val repository = (requireActivity().application as TotalRecallApplication).repository
            val exportService = ExportService(requireContext(), repository)
            
            // Create output file in Downloads directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val filename = exportService.createDefaultExportFilename()
            val outputFile = File(downloadsDir, filename)
            
            // Start export
            viewModel.exportEntries(exportService, outputFile)
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to start export: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}