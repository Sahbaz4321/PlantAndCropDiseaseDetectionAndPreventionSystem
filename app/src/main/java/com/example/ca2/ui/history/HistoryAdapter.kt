package com.example.ca2.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ca2.R
import com.example.ca2.data.model.Prediction
import com.example.ca2.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onItemClick: (Prediction) -> Unit,
    private val onActionClick: (Prediction, ReportAction) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    enum class ReportAction { RENAME, SHARE, DOWNLOAD, DELETE }

    private var predictions: List<Prediction> = emptyList()

    fun submitList(list: List<Prediction>) {
        predictions = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(predictions[position])
    }

    override fun getItemCount(): Int = predictions.size

    inner class ViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(prediction: Prediction) {
            binding.tvDiseaseName.text = prediction.diseaseName
            binding.tvConfidence.text = "${(prediction.confidence * 100).toInt()}%"
            binding.tvSummary.text = prediction.description.ifBlank {
                "Tap to open the full disease report and recommendations."
            }

            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.tvDate.text = sdf.format(Date(prediction.createdAt))

            Glide.with(binding.ivPlant.context)
                .load(prediction.imageUrl)
                .into(binding.ivPlant)

            binding.root.setOnClickListener { onItemClick(prediction) }
            binding.btnItemMenu.setOnClickListener { anchor ->
                showMenu(anchor, prediction)
            }
        }

        private fun showMenu(anchor: View, prediction: Prediction) {
            PopupMenu(anchor.context, anchor).apply {
                inflate(R.menu.report_actions_menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_rename -> onActionClick(prediction, ReportAction.RENAME)
                        R.id.action_share -> onActionClick(prediction, ReportAction.SHARE)
                        R.id.action_download -> onActionClick(prediction, ReportAction.DOWNLOAD)
                        R.id.action_delete -> onActionClick(prediction, ReportAction.DELETE)
                    }
                    true
                }
            }.show()
        }
    }
}
