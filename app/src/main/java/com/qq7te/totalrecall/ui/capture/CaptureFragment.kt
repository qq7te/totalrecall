package com.qq7te.totalrecall.ui.capture

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.qq7te.totalrecall.TotalRecallApplication
import com.qq7te.totalrecall.R
import com.qq7te.totalrecall.databinding.FragmentCaptureBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CaptureFragment : Fragment() {
    
    private var _binding: FragmentCaptureBinding? = null
    private val binding get() = _binding!!
    
    private val args: CaptureFragmentArgs by navArgs()
    
    private val viewModel: CaptureViewModel by viewModels {
        CaptureViewModelFactory((requireActivity().application as TotalRecallApplication).repository)
    }
    
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var locationManager: LocationManager
    private var photoUri: Uri? = null
    private var editingEntryId: Long? = null
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Permissions not granted", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCaptureBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        binding.captureButton.setOnClickListener { takePhoto() }
        binding.saveButton.setOnClickListener { saveEntry() }
        binding.retakeButton.setOnClickListener { retakePhoto() }

        if (args.entryId > 0L) {
            // Edit existing entry: load current data, disable camera controls
            editingEntryId = args.entryId
            loadEntryForEditing(args.entryId)
        } else {
            // New entry flow: default to pre-capture UI and start camera
            showPreCaptureUI()

            // Only request permissions if not already granted
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                requestPermissions()
            }
        }
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        requestPermissionLauncher.launch(permissions)
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }
            
            imageCapture = ImageCapture.Builder().build()
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }
    
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }
        
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()
        
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    photoUri = output.savedUri
                    // switch to small preview mode
                    binding.photoPreview.setImageURI(photoUri)
                    showPostCaptureUI()
                }
                
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        requireContext(),
                        "Photo capture failed: ${exc.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
    
    private fun retakePhoto() {
        // Restore camera preview and hide small photo
        showPreCaptureUI()
        photoUri = null
        // restart camera in case it was stopped
        startCamera()
    }
    
    private fun saveEntry() {
        val text = binding.entryText.text.toString()
        if (text.isBlank()) {
            val message = if (editingEntryId != null) {
                "Please enter text"
            } else {
                "Please enter text and take a photo"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            return
        }

        // If editing an existing entry, only update the text and return to previous screen
        val existingId = editingEntryId
        if (existingId != null) {
            lifecycleScope.launch {
                viewModel.updateEntryText(existingId, text)
                Toast.makeText(requireContext(), "Entry updated successfully", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            return
        }
        
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val location = getLastKnownLocation()
            lifecycleScope.launch {
                viewModel.saveEntry(
                    text = text,
                    photoUri = photoUri.toString(),
                    latitude = location?.latitude,
                    longitude = location?.longitude
                )
                
                // Reset UI
                binding.entryText.text?.clear()
                showPreCaptureUI()
                photoUri = null
                
                Toast.makeText(requireContext(), "Entry saved successfully", Toast.LENGTH_SHORT).show()
            }
        } else {
            lifecycleScope.launch {
                viewModel.saveEntry(
                    text = text,
                    photoUri = photoUri.toString(),
                    latitude = null,
                    longitude = null
                )
                
                // Reset UI
                binding.entryText.text?.clear()
                showPreCaptureUI()
                photoUri = null
                
                Toast.makeText(requireContext(), "Entry saved without location", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    @Suppress("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        // Try GPS first, then Network provider
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        
        for (provider in providers) {
            if (locationManager.isProviderEnabled(provider)) {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null) {
                    return location
                }
            }
        }
        return null
    }
    
    private fun showPreCaptureUI() {
        binding.viewFinder.visibility = View.VISIBLE
        binding.photoPreview.visibility = View.GONE
        binding.retakeButton.visibility = View.GONE
        binding.captureButton.visibility = View.VISIBLE
        binding.saveButton.visibility = View.VISIBLE
    }

    private fun showPostCaptureUI() {
        binding.viewFinder.visibility = View.GONE
        binding.photoPreview.visibility = View.VISIBLE
        binding.retakeButton.visibility = View.VISIBLE
        binding.captureButton.visibility = View.GONE
        binding.saveButton.visibility = View.VISIBLE
    }

    private fun loadEntryForEditing(entryId: Long) {
        // In edit mode we reuse this screen but disable camera and show existing photo/text
        binding.viewFinder.visibility = View.GONE
        binding.captureButton.visibility = View.GONE
        binding.retakeButton.visibility = View.GONE
        binding.photoPreview.visibility = View.VISIBLE
        binding.saveButton.visibility = View.VISIBLE

        lifecycleScope.launch {
            val entry = viewModel.getEntryById(entryId)
            if (entry != null) {
                photoUri = Uri.parse(entry.photoPath)
                binding.photoPreview.setImageURI(photoUri)
                binding.entryText.setText(entry.text)
            } else {
                Toast.makeText(requireContext(), "Entry not found", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
