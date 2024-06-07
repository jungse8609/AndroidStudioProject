package com.example.termproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ProfileAdapter(
    private val context: Context,
    private val imageNames: List<String>, // Change to list of image names
    private val onItemClick: (String) -> Unit // Pass image name on item click
) : RecyclerView.Adapter<ProfileAdapter.ViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_profile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageName = "profile${position + 1}" // Construct image name like profile1, profile2, ...
        // Load image using resource ID
        val resourceId = context.resources.getIdentifier(imageName, "drawable", context.packageName)
        holder.imageView.setImageResource(resourceId)

        holder.itemView.setOnClickListener {
            onItemClick(imageName) // Pass image name on item click
            selectedPosition = position
            notifyDataSetChanged()
        }

        holder.itemView.alpha = if (selectedPosition == position) 1.0f else 0.5f
    }

    override fun getItemCount(): Int {
        return imageNames.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }
}
