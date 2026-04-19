package com.example.ca2.ui.scan

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ca2.databinding.ItemSelectedImageBinding

class SelectedImagesAdapter(
    private val onImageClick: (Uri) -> Unit
) : RecyclerView.Adapter<SelectedImagesAdapter.ViewHolder>() {

    private var images: List<Uri> = emptyList()
    private var selectedUri: Uri? = null

    fun submitList(items: List<Uri>, selected: Uri?) {
        images = items
        selectedUri = selected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSelectedImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(images[position], images[position] == selectedUri)
    }

    override fun getItemCount(): Int = images.size

    inner class ViewHolder(private val binding: ItemSelectedImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri, isSelected: Boolean) {
            Glide.with(binding.ivThumb.context)
                .load(uri)
                .into(binding.ivThumb)

            binding.selectionOverlay.alpha = if (isSelected) 1f else 0f
            binding.root.setOnClickListener { onImageClick(uri) }
        }
    }
}
