package com.example.stalblet

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.stalblet.model.Sublet
import java.util.Date
import androidx.core.util.Pair as AndroidPair

class MyListingsFragment : Fragment(R.layout.fragment_my_listings) {
    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rv = view.findViewById<RecyclerView>(R.id.rvMyListings)
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val adapter = MyListingsAdapter(
            emptyList(),
            onToggle = { sublet, isVisible ->
                db.collection("sublets").document(sublet.id)
                    .update("visible", isVisible)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(),
                            if (isVisible) "Listing shown" else "Listing hidden",
                            Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(),
                            "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            },
            onEditDates = { sublet ->
                showDateRangePicker(sublet)
            }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        db.collection("sublets")
            .whereEqualTo("ownerId", me)
            .addSnapshotListener { snaps, err ->
                if (err != null) return@addSnapshotListener
                val items = snaps?.toObjects(Sublet::class.java) ?: emptyList()
                adapter.updateData(items)
            }
    }

    private fun showDateRangePicker(sublet: Sublet) {
        // Use AndroidX Pair, not Kotlin Pair
        val initial = AndroidPair(
            sublet.startDate.toDate().time,
            sublet.endDate.toDate().time
        )
        val picker = MaterialDatePicker.Builder
            .dateRangePicker()
            .setTitleText("Select availability period")
            .setSelection(initial)
            .build()

        picker.show(parentFragmentManager, "DATE_RANGE_PICKER")
        picker.addOnPositiveButtonClickListener { selection ->
            val startMs = selection.first
            val endMs   = selection.second
            val startTs = Timestamp(Date(startMs))
            val endTs   = Timestamp(Date(endMs))

            db.collection("sublets").document(sublet.id)
                .update(
                    mapOf(
                        "startDate" to startTs,
                        "endDate"   to endTs
                    )
                )
                .addOnSuccessListener {
                    Toast.makeText(requireContext(),
                        "Dates updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(),
                        "Date update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
