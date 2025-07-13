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
import com.squareup.picasso.Picasso
import com.google.firebase.firestore.FirebaseFirestore

class SubletDetailFragment : Fragment() {

    private var ownerId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sublet_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val subletId = arguments?.getString("subletId")
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
                    // 1) Parse fields
                    val title     = doc.getString("title") ?: ""
                    val desc      = doc.getString("description") ?: ""
                    val imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList()
                    ownerId       = doc.getString("ownerId")

                    // 2) Populate your views
                    view.findViewById<TextView>(R.id.tvTitle).text       = title
                    view.findViewById<TextView>(R.id.tvDescription).text = desc

                    // 3) Load the main image with Glide (if any)
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

        // 4) Hook up the "Chat with Owner" button
        view.findViewById<Button>(R.id.btnChatOwner)
            .setOnClickListener {
                ownerId?.let { oid ->
                    parentFragmentManager.beginTransaction()
                        .replace(
                            R.id.auth_container,
                            ChatFragment.newInstance(oid)
                        )
                        .addToBackStack(null)
                        .commit()
                } ?: run {
                    Log.e("SubletDetail", "ownerId is null, cannot start chat")
                }
            }
    }
}
