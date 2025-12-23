package com.qq7te.totalrecall.ui.detail

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.qq7te.totalrecall.TotalRecallApplication
import com.qq7te.totalrecall.databinding.FragmentDetailBinding
import java.text.SimpleDateFormat
import java.util.Locale

class DetailFragment : Fragment() {
    
    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    
    private val args: DetailFragmentArgs by navArgs()
    
    private val viewModel: DetailViewModel by viewModels {
        DetailViewModelFactory(
            (requireActivity().application as TotalRecallApplication).repository,
            args.entryId
        )
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel.entry.observe(viewLifecycleOwner) { entry ->
            entry?.let {
                binding.textContent.text = it.text
                
                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                binding.timestamp.text = dateFormat.format(it.timestamp)
                
                if (it.latitude != null && it.longitude != null) {
                    binding.location.text = "Location: ${it.latitude}, ${it.longitude}"
                    binding.location.visibility = View.VISIBLE
                } else {
                    binding.location.visibility = View.GONE
                }
                
                Glide.with(this)
                    .load(Uri.parse(it.photoPath))
                    .fitCenter()
                    .into(binding.photo)

                // Cap photo height to 80% of the current viewport after layout
                binding.photo.post {
                    val parentHeight = binding.root.height
                    if (parentHeight > 0) {
                        binding.photo.maxHeight = (parentHeight * 0.8f).toInt()
                    }
                }
            }
        }
        
        viewModel.deleteResult.observe(viewLifecycleOwner) { success ->
            success?.let {
                if (it) {
                    Toast.makeText(requireContext(), "Entry deleted successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(requireContext(), "Failed to delete entry", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        binding.buttonDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.buttonEdit.setOnClickListener {
            val action = DetailFragmentDirections.actionDetailToCapture(args.entryId)
            findNavController().navigate(action)
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refreshEntry()
    }
    
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete this entry? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteEntry()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 