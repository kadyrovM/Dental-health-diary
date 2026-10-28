package com.example.lab3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class TimerHistoryAdapter(
    private val items: List<Int>,
    private val onRepeatClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<TimerHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvHistoryTime)
        val btnRepeat: Button = view.findViewById(R.id.btnRepeat)
        val btnDelete: Button = view.findViewById(R.id.btnDeleteHistory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_timer_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val totalSeconds = items[position]
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        holder.tvTime.text = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

        holder.btnRepeat.setOnClickListener { onRepeatClick(totalSeconds) }
        holder.btnDelete.setOnClickListener { onDeleteClick(totalSeconds) }
    }

    override fun getItemCount() = items.size
}