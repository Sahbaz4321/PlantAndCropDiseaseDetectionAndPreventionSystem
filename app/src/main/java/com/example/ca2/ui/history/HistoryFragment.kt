package com.example.ca2.ui.history

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ca2.R
import com.example.ca2.data.firebase.FirebaseManager
import com.example.ca2.data.model.Prediction
import com.example.ca2.databinding.FragmentHistoryBinding
import com.example.ca2.ui.reports.ReportExportHelper
import com.example.ca2.ui.scan.ResultFragment

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val firebaseManager = FirebaseManager()
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadHistory()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            onItemClick = { prediction ->
                findNavController().navigate(R.id.resultFragment, ResultFragment.createArgs(prediction))
            },
            onActionClick = { prediction, action ->
                handleReportAction(prediction, action)
            }
        )
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter
    }

    private fun handleReportAction(prediction: Prediction, action: HistoryAdapter.ReportAction) {
        when (action) {
            HistoryAdapter.ReportAction.RENAME -> promptRename(prediction)
            HistoryAdapter.ReportAction.SHARE -> ReportExportHelper.shareSummary(requireContext(), prediction)
            HistoryAdapter.ReportAction.DOWNLOAD -> {
                ReportExportHelper.downloadPdf(requireContext(), prediction)
                Toast.makeText(requireContext(), "PDF downloaded.", Toast.LENGTH_SHORT).show()
            }
            HistoryAdapter.ReportAction.DELETE -> deletePrediction(prediction)
        }
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
                    firebaseManager.saveOrUpdatePrediction(prediction.copy(diseaseName = newName)) { success, _ ->
                        if (success) loadHistory()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePrediction(prediction: Prediction) {
        if (prediction.userId.isBlank() || prediction.predictionId.isBlank()) return
        firebaseManager.deletePrediction(prediction.userId, prediction.predictionId) { success ->
            if (success && isAdded) {
                loadHistory()
            }
        }
    }

    private fun loadHistory() {
        val userId = firebaseManager.getCurrentUserId() ?: return
        firebaseManager.getPredictions(userId) { predictions ->
            if (!isAdded || _binding == null) return@getPredictions
            if (predictions.isEmpty()) {
                binding.llEmptyState.visibility = View.VISIBLE
                binding.rvHistory.visibility = View.GONE
            } else {
                binding.llEmptyState.visibility = View.GONE
                binding.rvHistory.visibility = View.VISIBLE
                adapter.submitList(predictions)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
