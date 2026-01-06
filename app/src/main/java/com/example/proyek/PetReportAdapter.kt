package com.example.proyek

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PetReportAdapter(
    private val list: MutableList<PetReport>,
    private val onClick: (PetReport) -> Unit
) : RecyclerView.Adapter<PetReportAdapter.VH>() {

    private val fullList = mutableListOf<PetReport>()

    fun updateData(newList: List<PetReport>) {
        list.clear()
        list.addAll(newList)

        fullList.clear()
        fullList.addAll(newList)

        notifyDataSetChanged()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvEmail: TextView = view.findViewById(R.id.tvEmail)
        val tvBadge: TextView = view.findViewById(R.id.tvBadge)
        val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pet_report, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = list[pos]

        h.tvTitle.text =
            "${item.animalName ?: "Unknown"} - ${item.animalType} - ${item.animalColor}"

        h.tvEmail.text = item.ownerEmail ?: "Found Report"

        if (!item.photoUrl.isNullOrEmpty()) {
            Glide.with(h.itemView.context)
                .load(item.photoUrl)
                .into(h.ivPhoto)
        } else {
            h.ivPhoto.setImageDrawable(null)
        }

        // 🔥 BADGE
        if (item.type == ReportType.LOST) {
            h.tvBadge.text = "LOST"
            h.tvBadge.setBackgroundResource(R.drawable.badge_lost)
        } else {
            h.tvBadge.text = "FOUND"
            h.tvBadge.setBackgroundResource(R.drawable.badge_found)
        }

        h.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = list.size
}

