package com.example.stalblet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class SubletDetailFragment : Fragment() {

    private var ownerId: String? = null

    companion object {
        private const val ARG_SUBLET_ID = "subletId"

        /** Instantiate with the ID of the sublet to display */
        fun newInstance(subletId: String): SubletDetailFragment =
            SubletDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SUBLET_ID, subletId)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sublet_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val subletId = arguments?.getString(ARG_SUBLET_ID)
        if (subletId.isNullOrEmpty()) {
            Log.e("SubletDetail", "No subletId provided")
            return
        }

        FirebaseFirestore.getInstance()
            .collection("sublets")
            .document(subletId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val title     = doc.getString("title") ?: ""
                    val desc      = doc.getString("description") ?: ""
                    val imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList()
                    ownerId       = doc.getString("ownerId")

                    // Title with fallback
                    view.findViewById<TextView>(R.id.tvTitle).text =
                        if (title.isBlank()) "Untitled" else title

                    // Description with placeholder
                    view.findViewById<TextView>(R.id.tvDescription).text =
                        if (desc.isBlank()) "No description provided" else desc

                    // Load main photo if available
                    if (imageUrls.isNotEmpty()) {
                        Picasso.get()
                            .load(imageUrls[0])
                            .into(view.findViewById<ImageView>(R.id.ivMainPhoto))
                    }
                } else {
                    Log.e("SubletDetail", "Document $subletId does not exist")
                }
            }
            .addOnFailureListener { e ->
                Log.e("SubletDetail", "Error loading $subletId", e)
            }

        // Chat button: only navigates once we have both IDs
        view.findViewById<Button>(R.id.btnChatOwner)
            .setOnClickListener {
                if (subletId.isNotEmpty() && !ownerId.isNullOrEmpty()) {
                    parentFragmentManager.beginTransaction()
                        .replace(
                            R.id.auth_container,
                            ChatFragment.newInstance(subletId, ownerId!!)
                        )
                        .addToBackStack(null)
                        .commit()
                } else {
                    Log.e("SubletDetail", "Missing subletId or ownerId, cannot start chat")
                }
            }
    }
}
