package com.example.ca2.ui.dashboard

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
import com.example.ca2.databinding.FragmentHomeBinding
import com.example.ca2.ui.history.HistoryAdapter
import com.example.ca2.ui.reports.ReportExportHelper
import com.example.ca2.ui.scan.ResultFragment

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val firebaseManager = FirebaseManager()
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadUserData()
        loadRecentPredictions()

        binding.cardScan.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_scanFragment)
        }

        binding.cardHistory.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_historyFragment)
        }

        binding.cardReports.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_historyFragment)
        }

        binding.tvSeeAll.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_historyFragment)
        }
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
        binding.rvRecent.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecent.adapter = adapter
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
                        if (success) loadRecentPredictions()
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
                loadRecentPredictions()
            }
        }
    }

    private fun loadUserData() {
        val userId = firebaseManager.getCurrentUserId() ?: return
        firebaseManager.getUser(userId) { user ->
            if (isAdded && _binding != null) {
                user?.let {
                    binding.tvUserName.text = it.name
                }
            }
        }
    }

    private fun loadRecentPredictions() {
        val userId = firebaseManager.getCurrentUserId() ?: return
        firebaseManager.getPredictions(userId) { predictions ->
            if (isAdded && _binding != null) {
                // Show only max 3 recent activities as requested
                adapter.submitList(predictions.take(3))
                binding.tvReportsValue.text = predictions.size.toString()
                binding.tvLatestDisease.text = predictions.firstOrNull()?.diseaseName ?: "No scans yet"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
