package com.example.stalblet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stalblet.databinding.FragmentChatBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

data class ChatMessage(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

class ChatFragment : Fragment() {

    companion object {
        private const val ARG_SUBLET_ID = "sublet_id"
        fun newInstance(subletId: String): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SUBLET_ID, subletId)
                }
            }
        }
    }

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var subletId: String = ""

    private lateinit var adapter: MessagesAdapter
    private lateinit var messagesCol: CollectionReference
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        subletId = requireArguments().getString(ARG_SUBLET_ID)!!
        messagesCol = db.collection("chats")
            .document(subletId)
            .collection("messages")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // RecyclerView setup
        adapter = MessagesAdapter(auth.currentUser?.uid ?: "")
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMessages.adapter = adapter

        // Listen for new messages
        listener = messagesCol
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snaps, err ->
                if (err != null || snaps == null) return@addSnapshotListener
                val msgs = snaps.toObjects(ChatMessage::class.java)
                adapter.submitList(msgs)
                binding.rvMessages.scrollToPosition(msgs.size - 1)
            }

        // Send button
        binding.btnSend.setOnClickListener {
            val text = binding.etMessageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                val msg = ChatMessage(
                    senderId = auth.currentUser?.uid ?: "",
                    text     = text,
                    timestamp= Timestamp.now()
                )
                messagesCol.add(msg)
                binding.etMessageInput.text?.clear()
            }
        }
    }

    // Simple ListAdapter for ChatMessage
    private class MessagesAdapter(
        private val myUid: String
    ) : androidx.recyclerview.widget.ListAdapter<ChatMessage, MessagesAdapter.VH>(
        object : androidx.recyclerview.widget.DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(a: ChatMessage, b: ChatMessage) =
                a.timestamp == b.timestamp && a.senderId == b.senderId

            override fun areContentsTheSame(a: ChatMessage, b: ChatMessage) =
                a == b
        }
    ) {
        inner class VH(val binding: com.example.stalblet.databinding.ItemMessageBinding)
            : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = com.example.stalblet.databinding.ItemMessageBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val msg = getItem(position)
            holder.binding.tvMessage.text = msg.text
            // Align left/right based on sender
            val isMe = msg.senderId == myUid
            holder.binding.tvMessageContainer
                .setBackgroundResource(
                    if (isMe) R.drawable.bg_message_outgoing else R.drawable.bg_message_incoming
                )
            val params = holder.binding.tvMessage.layoutParams as ViewGroup.MarginLayoutParams
            if (isMe) {
                params.marginStart = 64
                params.marginEnd = 0
            } else {
                params.marginStart = 0
                params.marginEnd = 64
            }
            holder.binding.tvMessage.layoutParams = params
        }
    }
}
