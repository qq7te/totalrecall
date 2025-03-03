package com.example.captureapp.ui.detail

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.captureapp.CaptureApplication
import com.example.captureapp.databinding.FragmentDetailBinding
import java.text.SimpleDateFormat
import java.util.Locale

class DetailFragment : Fragment() {
    
    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    
    private val args: DetailFragmentArgs by navArgs()
    
    private val viewModel: DetailViewModel by viewModels {
        DetailViewModelFactory(
            (requireActivity().application as CaptureApplication).repository,
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
                    .into(binding.photo)
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 