package com.example.lab3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lab3.data.DentalRecord

class DentalAdapter : RecyclerView.Adapter<DentalAdapter.ViewHolder>() {

    private var records: List<DentalRecord> = emptyList()

    var onLongClickListener: ((DentalRecord) -> Unit)? = null

    fun submitList(newRecords: List<DentalRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvNotes: TextView = view.findViewById(R.id.tvNotes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dental_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        holder.tvDate.text = record.date

        val statusParts = mutableListOf<String>()
        if (record.brushedMorning) statusParts.add("Утро ✅")
        if (record.brushedEvening) statusParts.add("Вечер ✅")
        if (record.flossed) statusParts.add("Нить ✅")
        holder.tvStatus.text =
            if (statusParts.isEmpty()) "Пропуск ❌" else statusParts.joinToString("  ")

        holder.tvNotes.text = record.notes ?: ""
        holder.tvNotes.visibility =
            if (record.notes.isNullOrBlank()) View.GONE else View.VISIBLE

        holder.itemView.setOnLongClickListener {
            onLongClickListener?.invoke(record)
            true
        }
    }

    override fun getItemCount() = records.size
}