package com.example.stalblet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormat

class ConversationAdapter(
    private var items: List<Conversation>,
    private val currentUserId: String,
    private val onClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(android.R.id.text1)
        val tvSubtitle: TextView = itemView.findViewById(android.R.id.text2)

        init {
            itemView.setOnClickListener {
                // Safely get adapterPosition and ensure it's valid
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onClick(items[pos])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // Inflate Android's two-line list item
        val v = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val convo = items[position]
        holder.tvTitle.text = convo.subletTitle

        // Display who sent last message and when
        val who = if (convo.participants.firstOrNull() == currentUserId) "You" else "Owner"
        val time = convo.lastTimestamp
            ?.toDate()
            ?.let { DateFormat.getDateTimeInstance().format(it) }
            ?: ""
        holder.tvSubtitle.text = "$who: ${convo.lastMessage}  •  $time"
    }

    override fun getItemCount() = items.size

    /** Replace the current list and refresh */
    fun updateData(newItems: List<Conversation>) {
        items = newItems
        notifyDataSetChanged()
    }
}
