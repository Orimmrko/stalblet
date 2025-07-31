package com.example.stalblet

import MessagesAdapter
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stalblet.model.ChatMessage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatFragment : Fragment(R.layout.fragment_chat) {
    private lateinit var subletId: String
    private lateinit var ownerId: String

    // Firestore references
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var messagesRef: CollectionReference

    companion object {
        private const val TAG = "ChatFragment"
        private const val ARG_SUBLET_ID = "arg_sublet_id"
        private const val ARG_OWNER_ID  = "arg_owner_id"

        /** Use this to instantiate the fragment */
        fun newInstance(subletId: String, ownerId: String) = ChatFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_SUBLET_ID, subletId)
                putString(ARG_OWNER_ID, ownerId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            subletId = it.getString(ARG_SUBLET_ID)
                ?: throw IllegalStateException("ChatFragment missing subletId")
            ownerId = it.getString(ARG_OWNER_ID)
                ?: throw IllegalStateException("ChatFragment missing ownerId")
        } ?: throw IllegalStateException("ChatFragment requires arguments")

        // Point at the right Firestore location
        messagesRef = db.collection("sublets")
            .document(subletId)
            .collection("chat")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ─── 0) preload the sublet title ──────────────────────────────
        var subletTitle = ""
        val subletRef = db.collection("sublets").document(subletId)
        subletRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) subletTitle = doc.getString("title") ?: ""
        }

        // ─── 1) find views & adapter ─────────────────────────────────
        val rvMessages     = view.findViewById<RecyclerView>(R.id.rvMessages)
        val etMessageInput = view.findViewById<EditText>(R.id.etMessageInput)
        val btnSend        = view.findViewById<Button>(R.id.btnSend)

        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
        val adapter = MessagesAdapter(emptyList(), currentUserId)
        rvMessages.layoutManager = LinearLayoutManager(requireContext())
        rvMessages.adapter       = adapter

        // ─── 2) real-time chat listener ───────────────────────────────
        messagesRef
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snaps, err ->
                if (err != null) return@addSnapshotListener
                val msgs = snaps?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)?.apply { id = doc.id }
                    } ?: emptyList()
                adapter.updateData(msgs)
                if (msgs.isNotEmpty()) rvMessages.scrollToPosition(msgs.lastIndex)
            }

        // ─── 3) send button ────────────────────────────────────────────
        btnSend.setOnClickListener {
            val text = etMessageInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            val message = ChatMessage(
                senderId  = currentUserId,
                text      = text,
                timestamp = Timestamp.now()
            )
            messagesRef.add(message)
                .addOnSuccessListener {
                    etMessageInput.text.clear()

                    // ─── Upsert the conversation summary ────────────────────
                    val convRef = db.collection("conversations").document(subletId)
                    convRef.set(
                        mapOf(
                            "subletId"      to subletId,
                            "subletTitle"   to subletTitle,
                            "participants"  to listOf(currentUserId, ownerId),
                            "lastMessage"   to message.text,
                            "lastTimestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Send failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

}
