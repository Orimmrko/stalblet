package com.example.stalblet

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.stalblet.model.Sublet
import java.text.DateFormat

class MyListingsAdapter(
    private var items: List<Sublet>,
    private val onToggle: (Sublet, Boolean) -> Unit,
    private val onEditDates: (Sublet) -> Unit
) : RecyclerView.Adapter<MyListingsAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView? = itemView.findViewById<TextView>(R.id.tvItemTitle)
        val tvDates: TextView? = itemView.findViewById<TextView>(R.id.tvItemDates)
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        val switchVis: Switch? = itemView.findViewById<Switch>(R.id.switchItemVisible)
        val btnEditDates: ImageButton? = itemView.findViewById<ImageButton>(R.id.btnEditDates)

        init {
            switchVis?.setOnCheckedChangeListener { _, isChecked ->
                adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let {
                    onToggle(items[it], isChecked)
                }
            }
            btnEditDates?.setOnClickListener {
                adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let {
                    onEditDates(items[it])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_listing, parent, false)
        return VH(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        holder.tvTitle?.text = s.title
        val fmt = DateFormat.getDateInstance()
        holder.tvDates?.text = "${fmt.format(s.startDate.toDate())} → ${fmt.format(s.endDate.toDate())}"
        holder.switchVis?.isChecked = s.visible
    }

    override fun getItemCount() = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: List<Sublet>) {
        items = newItems
        notifyDataSetChanged()
    }
}
