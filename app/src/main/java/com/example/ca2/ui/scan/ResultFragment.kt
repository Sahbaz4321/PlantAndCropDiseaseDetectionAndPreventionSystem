package com.example.ca2.ui.scan

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.PopupMenu
import android.graphics.Color
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.ca2.R
import com.example.ca2.data.api.GeminiDiseaseService
import com.example.ca2.data.firebase.FirebaseManager
import com.example.ca2.data.model.Prediction
import com.example.ca2.databinding.FragmentResultBinding
import com.example.ca2.ui.reports.ReportExportHelper

class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!
    private val firebaseManager = FirebaseManager()
    private val geminiDiseaseService = GeminiDiseaseService()
    private var currentPrediction: Prediction? = null
    private var isSaved = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnReportMenu.setOnClickListener { showReportMenu(it) }

        val initialPrediction = predictionFromArgs()
        currentPrediction = initialPrediction
        
        if (initialPrediction.description.isBlank()) {
            // New scan: Show loading and fetch EVERYTHING before binding
            generateDiseaseReport(initialPrediction)
        } else {
            // Opened from history: Everything is already there
            bindPrediction(initialPrediction)
            showLoadingState(false, "Report loaded")
            isSaved = initialPrediction.predictionId.isNotBlank()
        }
    }

    private fun predictionFromArgs(): Prediction {
        val args = requireArguments()
        return Prediction(
            predictionId = args.getString(ARG_PREDICTION_ID).orEmpty(),
            userId = args.getString(ARG_USER_ID).orEmpty(),
            imageUrl = args.getString(ARG_IMAGE_URI).orEmpty(),
            diseaseName = args.getString(ARG_DISEASE_NAME).orEmpty()
                .replace("___", " ")
                .replace("_", " "),
            confidence = args.getFloat(ARG_CONFIDENCE, 0f).toDouble(),
            description = args.getString(ARG_DESCRIPTION).orEmpty(),
            causes = args.getString(ARG_CAUSES).orEmpty(),
            prevention = args.getString(ARG_PREVENTION).orEmpty(),
            fertilizer = args.getString(ARG_FERTILIZER).orEmpty(),
            pesticide = args.getString(ARG_PESTICIDE).orEmpty(),
            recoveryTime = args.getString(ARG_RECOVERY_TIME).orEmpty(),
            extraCareTips = args.getString(ARG_EXTRA_CARE).orEmpty(),
            createdAt = args.getLong(ARG_CREATED_AT, System.currentTimeMillis())
        )
    }

    private fun generateDiseaseReport(basePrediction: Prediction) {
        showLoadingState(true, "Preparing your AI crop care report...")
        Thread {
            val aiPrediction = geminiDiseaseService.buildDetailedPrediction(
                diseaseName = basePrediction.diseaseName,
                confidence = basePrediction.confidence
            )
            val finalPrediction = aiPrediction.copy(
                predictionId = basePrediction.predictionId,
                userId = firebaseManager.getCurrentUserId().orEmpty(),
                imageUrl = basePrediction.imageUrl,
                createdAt = basePrediction.createdAt
            )

            activity?.runOnUiThread {
                if (!isAdded || _binding == null) return@runOnUiThread
                currentPrediction = finalPrediction
                bindPrediction(finalPrediction)
                showLoadingState(false, "AI report ready and synced for later use")
                autoSavePrediction()
            }
        }.start()
    }

    private fun bindPrediction(prediction: Prediction) {
        binding.tvDiseaseTitle.text = prediction.diseaseName.ifBlank { "Plant analysis" }
        binding.tvConfidencePercent.text = "${(prediction.confidence * 100).toInt()}%"
        binding.confidenceProgressCircular.progress = (prediction.confidence * 100).toInt()
        binding.tvConfidenceStatus.text = if (prediction.confidence > 0.7) "High Accuracy" else "Moderate Accuracy"
        
        binding.tvDescription.text = prediction.description.ifBlank { "Generating AI summary..." }
        binding.tvCauses.text = prediction.causes.ifBlank { "Waiting for disease cause analysis..." }
        binding.tvFertilizer.text = prediction.fertilizer.ifBlank { "Balanced fertilizer guidance will appear here." }
        binding.tvTreatment.text = prediction.pesticide.ifBlank { "Treatment advice will appear here." }
        binding.tvRecovery.text = prediction.recoveryTime.ifBlank { "Recovery timeline is being calculated." }

        // Ensure justification programmatically for better compatibility
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val justify = android.graphics.text.LineBreaker.JUSTIFICATION_MODE_INTER_WORD
            binding.tvDescription.justificationMode = justify
            binding.tvCauses.justificationMode = justify
            binding.tvFertilizer.justificationMode = justify
            binding.tvTreatment.justificationMode = justify
            binding.tvRecovery.justificationMode = justify
        }

        // Handle Prevention Tips as bullet points
        binding.preventionContainer.removeAllViews()
        val tips = prediction.prevention.split(". ", "\n", "- ").filter { it.isNotBlank() }
        if (tips.isEmpty()) {
            addPreventionPoint("Prevention guidance will appear here.")
        } else {
            tips.forEach { tip ->
                addPreventionPoint(tip.trim().removeSuffix("."))
            }
        }

        if (prediction.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(prediction.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivResultPlant)
        }
    }

    private fun addPreventionPoint(text: String) {
        val textView = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 12)
            }
            
            // Use BulletSpan for proper indentation - smaller bullet radius
            val bulletPoint = "  $text"
            val spannableString = android.text.SpannableString(bulletPoint)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                // radius reduced from 12 to 6 for smaller bullets
                spannableString.setSpan(
                    android.text.style.BulletSpan(24, Color.parseColor("#DB2777"), 6),
                    0, bulletPoint.length,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else {
                @Suppress("DEPRECATION")
                spannableString.setSpan(
                    android.text.style.BulletSpan(24),
                    0, bulletPoint.length,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            
            this.text = spannableString
            setTextColor(Color.parseColor("#831843"))
            textSize = 14f
            setLineSpacing(4f, 1.1f)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                justificationMode = android.graphics.text.LineBreaker.JUSTIFICATION_MODE_INTER_WORD
            }
        }
        binding.preventionContainer.addView(textView)
    }

    private fun showLoadingState(isLoading: Boolean, message: String) {
        binding.layoutAiStatus.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.progressAi.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.tvAiStatus.text = message
    }

    private fun showReportMenu(anchor: View) {
        val prediction = currentPrediction ?: return
        // Use a Wrapper context or check gravity to prevent clipping
        PopupMenu(requireContext(), anchor, android.view.Gravity.END).apply {
            inflate(R.menu.report_actions_menu)
            menu.findItem(R.id.action_delete).isVisible = false
            
            // Try to force show icons if supported, but mainly fix visibility
            try {
                val fields = javaClass.declaredFields
                for (field in fields) {
                    if ("mPopup" == field.name) {
                        field.isAccessible = true
                        val menuPopupHelper = field.get(this)
                        val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                        val setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                        setForceShowIcon.invoke(menuPopupHelper, true)
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_rename -> promptRename(prediction)
                    R.id.action_share -> ReportExportHelper.shareSummary(requireContext(), prediction)
                    R.id.action_download -> {
                        ReportExportHelper.downloadPdf(requireContext(), prediction)
                        Toast.makeText(requireContext(), "PDF downloaded.", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
        }.show()
    }

    private fun promptRename(prediction: Prediction) {
        val input = EditText(requireContext()).apply {
            setText(prediction.diseaseName)
            setSelection(text.length)
            setPadding(36, 28, 36, 28)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Rename report")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotBlank()) {
                    firebaseManager.saveOrUpdatePrediction(prediction.copy(diseaseName = newName)) { success, savedPrediction ->
                        if (success && isAdded) {
                            currentPrediction = savedPrediction
                            bindPrediction(savedPrediction)
                            Toast.makeText(requireContext(), "Report renamed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun autoSavePrediction() {
        val prediction = currentPrediction ?: return
        val userId = firebaseManager.getCurrentUserId()
        if (userId.isNullOrBlank()) return

        firebaseManager.saveOrUpdatePrediction(prediction.copy(userId = userId)) { success, savedPrediction ->
            if (success && isAdded) {
                currentPrediction = savedPrediction
                isSaved = true
                // Removed the status message as per user request
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PREDICTION_ID = "prediction_id"
        private const val ARG_USER_ID = "user_id"
        private const val ARG_IMAGE_URI = "image_uri"
        private const val ARG_DISEASE_NAME = "disease_name"
        private const val ARG_CONFIDENCE = "confidence"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_CAUSES = "causes"
        private const val ARG_PREVENTION = "prevention"
        private const val ARG_FERTILIZER = "fertilizer"
        private const val ARG_PESTICIDE = "pesticide"
        private const val ARG_RECOVERY_TIME = "recovery_time"
        private const val ARG_EXTRA_CARE = "extra_care"
        private const val ARG_CREATED_AT = "created_at"

        fun createArgs(prediction: Prediction): Bundle {
            return Bundle().apply {
                putString(ARG_PREDICTION_ID, prediction.predictionId)
                putString(ARG_USER_ID, prediction.userId)
                putString(ARG_IMAGE_URI, prediction.imageUrl)
                putString(ARG_DISEASE_NAME, prediction.diseaseName)
                putFloat(ARG_CONFIDENCE, prediction.confidence.toFloat())
                putString(ARG_DESCRIPTION, prediction.description)
                putString(ARG_CAUSES, prediction.causes)
                putString(ARG_PREVENTION, prediction.prevention)
                putString(ARG_FERTILIZER, prediction.fertilizer)
                putString(ARG_PESTICIDE, prediction.pesticide)
                putString(ARG_RECOVERY_TIME, prediction.recoveryTime)
                putString(ARG_EXTRA_CARE, prediction.extraCareTips)
                putLong(ARG_CREATED_AT, prediction.createdAt)
            }
        }
    }
}
