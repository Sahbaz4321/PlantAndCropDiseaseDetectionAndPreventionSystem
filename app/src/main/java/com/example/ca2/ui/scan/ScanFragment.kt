package com.example.ca2.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.ca2.R
import com.example.ca2.databinding.FragmentScanBinding
import java.io.File
import java.io.FileOutputStream

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!
    private val selectedImages = mutableListOf<Uri>()
    private var selectedImageUri: Uri? = null
    private var isMultiMode = false
    private lateinit var selectedImagesAdapter: SelectedImagesAdapter

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (cameraGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    private val singleImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages.clear()
            selectedImages.addAll(uris.distinct())
            selectedImageUri = selectedImages.firstOrNull()
            refreshSelectionUi()
        }
    }

    private val multipleImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages.addAll(uris.distinct())
            // Keep current selection if valid, else pick first from new
            if (selectedImageUri == null) {
                selectedImageUri = selectedImages.firstOrNull()
            }
            refreshSelectionUi()
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            saveBitmapToUri(it)?.let { uri ->
                selectedImages.add(uri)
                selectedImageUri = uri
                refreshSelectionUi()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        // Removed setupModeToggle call as it's no longer needed
        refreshSelectionUi()

        binding.btnGallery.setOnClickListener {
            // Now both behave as "Add to batch" but we can keep them distinct if needed
            // For simplicity, let's use the multiple launcher for everything now
            multipleImagesLauncher.launch("image/*")
        }

        binding.btnCamera.setOnClickListener {
            checkPermissionsAndOpenCamera()
        }

        binding.btnClearAll.setOnClickListener {
            selectedImages.clear()
            selectedImageUri = null
            refreshSelectionUi()
        }

        binding.btnAnalyze.setOnClickListener {
            selectedImageUri?.let { uri ->
                val bundle = Bundle().apply {
                    putString("image_uri", uri.toString())
                }
                findNavController().navigate(R.id.action_scanFragment_to_analysisFragment, bundle)
            }
        }
    }

    private fun setupRecyclerView() {
        selectedImagesAdapter = SelectedImagesAdapter { uri ->
            selectedImageUri = uri
            refreshSelectionUi()
        }
        binding.rvSelectedImages.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvSelectedImages.adapter = selectedImagesAdapter
    }

    private fun refreshSelectionUi() {
        if (_binding == null) return

        selectedImagesAdapter.submitList(selectedImages.toList(), selectedImageUri)
        val activeUri = selectedImageUri

        if (activeUri != null) {
            // Big preview removed as requested, only showing the Analyze button
            binding.btnAnalyze.visibility = View.VISIBLE
        } else {
            binding.btnAnalyze.visibility = View.GONE
        }

        binding.tvSelectionCount.text = when {
            selectedImages.isEmpty() -> "No images selected yet"
            else -> "${selectedImages.size} images selected. Tap a thumbnail to choose for analysis."
        }
    }

    private fun checkPermissionsAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }

    private fun openCamera() {
        cameraLauncher.launch(null)
    }

    private fun saveBitmapToUri(bitmap: Bitmap): Uri? {
        val file = File(requireContext().cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        return try {
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not save captured image.", Toast.LENGTH_SHORT).show()
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
