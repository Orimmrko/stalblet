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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvConversations)
        val me = FirebaseAuth.getInstance().currentUser!!.uid

        val adapter = ConversationAdapter(emptyList(), me) { convo ->
            // open the ChatFragment for that sublet
            val other = convo.participants.first { it != me }
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.auth_container,
                    ChatFragment.newInstance(convo.subletId, other)
                )
                .addToBackStack(null)
                .commit()
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter        = adapter

        // query all conversations where I participate
        db.collection("conversations")
            .whereArrayContains("participants", me)
            .orderBy("lastTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snaps, err ->
                if (err != null) {
                    Log.e("ChatListFragment","Load failed",err)
                    return@addSnapshotListener
                }
                val items = snaps?.toObjects(Conversation::class.java) ?: emptyList()
                adapter.updateData(items)
            }
    }
}
