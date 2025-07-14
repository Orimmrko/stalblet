package com.example.stalblet

import MessagesAdapter
import android.os.Bundle
import android.util.Log
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

        // 1) Find your views
        val rvMessages      = view.findViewById<RecyclerView>(R.id.rvMessages)
        val etMessageInput  = view.findViewById<EditText>(R.id.etMessageInput)
        val btnSend         = view.findViewById<Button>(R.id.btnSend)

        // 2) Prepare current user and adapter
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val adapter = MessagesAdapter(emptyList(), currentUserId)
        rvMessages.layoutManager = LinearLayoutManager(requireContext())
        rvMessages.adapter       = adapter

        // 3) Listen for real-time updates
        messagesRef
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Chat listener failed", error)
                    return@addSnapshotListener
                }
                val msgs = snapshots
                    ?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)?.apply {
                            id = doc.id
                        }
                    } ?: emptyList()

                adapter.updateData(msgs)
                if (msgs.isNotEmpty()) {
                    rvMessages.scrollToPosition(msgs.lastIndex)
                }
            }
        var subletTitle = ""
        db.collection("sublets")
            .document(subletId)
            .get()
            .addOnSuccessListener { doc ->
                subletTitle = doc.getString("title") ?: ""
            }
        // 4) Wire up the Send button
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
                    val convRef = db.collection("conversations").document(subletId)
                    convRef.set(
                        mapOf(
                            "subletId"     to subletId,
                            "subletTitle"  to subletTitle,
                            "participants" to listOf(currentUserId, ownerId),
                            "lastMessage"  to message.text,
                            "lastTimestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )

                }

                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to send message", e)
                    Toast.makeText(
                        requireContext(),
                        "Failed to send message",
                        Toast.LENGTH_SHORT
                    ).show()
                }

        }
    }

}
