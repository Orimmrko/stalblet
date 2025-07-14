package com.example.stalblet

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatListFragment : Fragment(R.layout.fragment_chat_list) {
    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "ChatListFragment"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvConversations)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val adapter = ConversationAdapter(emptyList(), currentUserId) { convo ->
            // On click, open the ChatFragment for that sublet
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.auth_container,
                    ChatFragment.newInstance(convo.subletId, /*ownerId=*/convo.participants
                        .first { it != currentUserId })
                )
                .addToBackStack(null)
                .commit()
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // Query conversations where current user is a participant
        db.collection("conversations")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snaps, err ->
                if (err != null) {
                    Log.e(TAG, "Failed loading conversations", err)
                    return@addSnapshotListener
                }
                val items = snaps
                    ?.toObjects(Conversation::class.java)
                    ?: emptyList()
                adapter.updateData(items)
            }
    }
}
