package com.example.proyek

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MyReportAdapter(
    private val reports: MutableList<MyReportItem>,
    private val onEdit: (MyReportItem) -> Unit,
    private val onDelete: (MyReportItem) -> Unit
) : RecyclerView.Adapter<MyReportAdapter.MyReportVH>() {

    inner class MyReportVH(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgAnimal)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvType: TextView = view.findViewById(R.id.tvType)
        val tvDetail: TextView = view.findViewById(R.id.tvDetail)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyReportVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_report, parent, false)
        return MyReportVH(view)
    }

    override fun onBindViewHolder(holder: MyReportVH, position: Int) {
        val r = reports[position]

        holder.tvTitle.text = r.animalName ?: "Unknown Animal"
        holder.tvType.text = r.type
        holder.tvDetail.text =
            "${r.animalType ?: "-"} • ${r.animalColor ?: "-"}"

        Glide.with(holder.itemView)
            .load(r.photoUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_placeholder)
            .into(holder.img)

        holder.btnEdit.setOnClickListener {
            onEdit(r)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(r)
        }
    }

    override fun getItemCount(): Int = reports.size
}
