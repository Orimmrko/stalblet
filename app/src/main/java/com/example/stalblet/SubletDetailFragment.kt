package com.example.stalblet

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import com.example.stalblet.model.Sublet
import com.google.firebase.auth.FirebaseAuth

class SubletDetailFragment : Fragment(R.layout.fragment_sublet_detail) {
    companion object {
        private const val ARG_SUBLET_ID = "subletId"
        fun newInstance(subletId: String) = SubletDetailFragment().apply {
            arguments = Bundle().apply { putString(ARG_SUBLET_ID, subletId) }
        }
    }

    private val db = FirebaseFirestore.getInstance()
    private lateinit var subletId: String
    private var ownerId: String? = null

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subletId = arguments?.getString(ARG_SUBLET_ID)
            ?: throw IllegalStateException("Missing subletId")

        // Find our views
        val tvTitle      = view.findViewById<TextView>(R.id.tvTitle)
        val tvDesc       = view.findViewById<TextView>(R.id.tvDescription)
        val ivPhoto      = view.findViewById<ImageView>(R.id.ivMainPhoto)
        val btnChat      = view.findViewById<Button>(R.id.btnChatOwner)
        val switchVis    = view.findViewById<Switch>(R.id.switchVisibility)

        // Load the document
        db.collection("sublets")
            .document(subletId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(requireContext(),
                        "This listing no longer exists", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                    return@addOnSuccessListener
                }
                val sublet = doc.toObject(Sublet::class.java)!!
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val isOwner = currentUserId == sublet.ownerId
                switchVis.isVisible = isOwner
                if (isOwner) {
                    // Owner can toggle
                    switchVis.isChecked = sublet.visible
                    switchVis.setOnCheckedChangeListener { _, isChecked ->
                        db.collection("sublets")
                            .document(subletId)
                            .update("visible", isChecked)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    requireContext(),
                                    if (isChecked) "Listing visible" else "Listing hidden",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e("SubletDetail", "Visibility update failed", e)
                                Toast.makeText(
                                    requireContext(),
                                    "Could not update visibility",
                                    Toast.LENGTH_SHORT
                                ).show()
                                switchVis.isChecked = !isChecked
                            }
                    }
                } else {
                    // Not owner: always reflect the stored value, but disable interaction
                    switchVis.isChecked = sublet.visible
                    switchVis.isEnabled = false
                }

                // Populate UI
                tvTitle.text = sublet.title.ifBlank { "Untitled" }
                tvDesc.text  = sublet.description.ifBlank { "No description provided" }
                ownerId      = sublet.ownerId

                sublet.imageUrls.firstOrNull()?.let {
                    Picasso.get().load(it).into(ivPhoto)
                }


                switchVis.isChecked = sublet.visible
                switchVis.setOnCheckedChangeListener { _, isChecked ->
                    db.collection("sublets")
                        .document(subletId)
                        .update("visible", isChecked)
                        .addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                if (isChecked) "Listing visible" else "Listing hidden",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("SubletDetail", "Visibility update failed", e)
                            Toast.makeText(
                                requireContext(),
                                "Could not update visibility",
                                Toast.LENGTH_SHORT
                            ).show()
                            // revert switch
                            switchVis.isChecked = !isChecked
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("SubletDetail", "Failed to load listing", e)
                Toast.makeText(requireContext(),
                    "Error loading listing", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }

        // Chat button unchanged…
        btnChat.setOnClickListener {
            ownerId?.let { oid ->
                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.auth_container,
                        ChatFragment.newInstance(subletId, oid)
                    )
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}
