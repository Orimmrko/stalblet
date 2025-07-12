import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.stalblet.ChatMessage
import com.example.stalblet.R

class MessagesAdapter(
    private val currentUserId: String,
    private val items: List<ChatMessage>
) : RecyclerView.Adapter<MessagesAdapter.VH>() {

    class VH(val view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(R.id.tvText)
        val container: View = view.findViewById(R.id.messageContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layout = if (viewType == 0) R.layout.item_message_outgoing else R.layout.item_message_incoming
        val v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return if (items[position].senderId == currentUserId) 0 else 1
    }

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val msg = items[pos]
        holder.tvText.text = msg.text
    }
}
