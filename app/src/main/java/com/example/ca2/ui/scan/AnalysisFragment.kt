package com.example.ca2.ui.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.ca2.R
import com.example.ca2.databinding.FragmentAnalysisBinding
import com.example.ca2.ml.Classifier

class AnalysisFragment : Fragment() {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!
    private lateinit var classifier: Classifier
    private val tipHandler = Handler(Looper.getMainLooper())
    private var tipRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            classifier = Classifier(requireContext())
        } catch (e: Exception) {
            Log.e("AnalysisFragment", "Classifier init failed", e)
            showError("Model initialization failed", e)
            return
        }

        val imageUriString = arguments?.getString("image_uri")
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            performAnalysis(imageUri)
        } else {
            showPlainError("Image not found. Please select the image again.")
        }
        
        startTipRotation()
    }

    private fun performAnalysis(uri: Uri) {
        Thread {
            try {
                val contentResolver = requireContext().contentResolver
                
                // Optimized Loading to prevent OOM on real devices
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                contentResolver.openInputStream(uri)?.use { 
                    BitmapFactory.decodeStream(it, null, options) 
                }

                // Target size for EfficientNet is usually 224, but we load slightly larger for quality
                val targetSize = 512
                var inSampleSize = 1
                if (options.outHeight > targetSize || options.outWidth > targetSize) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while (halfHeight / inSampleSize >= targetSize && halfWidth / inSampleSize >= targetSize) {
                        inSampleSize *= 2
                    }
                }

                options.inJustDecodeBounds = false
                options.inSampleSize = inSampleSize
                
                val bitmap = contentResolver.openInputStream(uri)?.use { 
                    BitmapFactory.decodeStream(it, null, options) 
                }?.copy(Bitmap.Config.ARGB_8888, true)

                if (bitmap == null) {
                    throw Exception("Could not load image bitmap")
                }

                Log.d("AnalysisFragment", "Bitmap loaded: ${bitmap.width}x${bitmap.height}")

                val result = classifier.classify(bitmap)
                
                // Artificial delay to show processing animation (optional)
                Thread.sleep(1500)

                activity?.runOnUiThread {
                    if (isAdded && _binding != null) {
                        val bundle = Bundle().apply {
                            putString("disease_name", result.first)
                            putFloat("confidence", result.second)
                            putString("image_uri", uri.toString())
                        }
                        findNavController().navigate(R.id.action_analysisFragment_to_resultFragment, bundle)
                    }
                }
            } catch (e: Exception) {
                Log.e("AnalysisFragment", "Analysis error", e)
                activity?.runOnUiThread {
                    if (isAdded) {
                        showError("Analysis failed", e)
                    }
                }
            }
        }.start()
    }

    private fun startTipRotation() {
        val tips = listOf(
            "Analyzing leaf pattern...",
            "Checking for fungal infections...",
            "Comparing with database...",
            "Generating recommendations..."
        )
        var index = 0
        tipRunnable = object : Runnable {
            override fun run() {
                if (isAdded && _binding != null) {
                    binding.tvStatus.text = tips[index]
                    index = (index + 1) % tips.size
                    tipHandler.postDelayed(this, 1000)
                }
            }
        }
        tipHandler.post(tipRunnable!!)
    }

    private fun showError(title: String, throwable: Throwable) {
        val detailedMessage = buildString {
            append(title)
            append("\n\n")
            append(throwable.localizedMessage ?: throwable.message ?: "Unknown error")
            append("\n\n")
            append(throwable.stackTraceToString())
        }
        showPlainError(detailedMessage)
    }

    private fun showPlainError(message: String) {
        if (_binding == null) return

        tipRunnable?.let { tipHandler.removeCallbacks(it) }
        binding.animationView.cancelAnimation()
        binding.animationView.visibility = View.GONE
        binding.tvStatus.text = getString(R.string.analysis_error_title)
        binding.tvTip.text = getString(R.string.analysis_error_hint)
        binding.tvErrorDetails.visibility = View.VISIBLE
        binding.tvErrorDetails.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tipRunnable?.let { tipHandler.removeCallbacks(it) }
        _binding = null
        if (::classifier.isInitialized) {
            classifier.close()
        }
    }
}
