package com.qq7te.totalrecall.ui.detail

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.qq7te.totalrecall.R
import com.qq7te.totalrecall.databinding.FragmentPhotoViewerBinding

/**
 * Full-screen photo viewer dialog that supports zoom and pan gestures.
 */
class PhotoViewerDialogFragment : DialogFragment() {

    private var _binding: FragmentPhotoViewerBinding? = null
    private val binding get() = _binding!!
    
    private var photoUri: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make the dialog full screen
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        
        photoUri = arguments?.getString(ARG_PHOTO_URI)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoViewerBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        photoUri?.let { uri ->
            Glide.with(this)
                .load(Uri.parse(uri))
                .fitCenter()
                .into(binding.photoViewer)
        }
        
        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        private const val ARG_PHOTO_URI = "photo_uri"
        
        /**
         * Create a new instance of PhotoViewerDialogFragment
         * @param photoUri The URI string of the photo to display
         */
        fun newInstance(photoUri: String): PhotoViewerDialogFragment {
            val fragment = PhotoViewerDialogFragment()
            val args = Bundle()
            args.putString(ARG_PHOTO_URI, photoUri)
            fragment.arguments = args
            return fragment
        }
    }
}
