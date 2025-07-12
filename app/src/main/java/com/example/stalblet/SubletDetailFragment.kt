package com.example.stalblet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.stalblet.databinding.FragmentSubletDetailBinding
import com.example.stalblet.model.Sublet
import com.google.firebase.firestore.FirebaseFirestore

class SubletDetailFragment : Fragment() {

    companion object {
        private const val ARG_SUBLET_ID = "sublet_id"

        /** Use this to create a detail fragment for a given sublet ID */
        @JvmStatic
        fun newInstance(subletId: String): SubletDetailFragment {
            return SubletDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SUBLET_ID, subletId)
                }
            }
        }
    }

    private var _binding: FragmentSubletDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var subletId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Grab the argument
        subletId = requireArguments().getString(ARG_SUBLET_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubletDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load the Sublet from Firestore
        subletId?.let { id ->
            db.collection("sublets")
                .document(id)
                .get()
                .addOnSuccessListener { doc ->
                    val s = doc.toObject(Sublet::class.java)
                    if (s != null) {
                        binding.tvTitle.text       = s.title
                        binding.tvDescription.text = s.description
                        // TODO: load s.imageUrls into an image carousel / RecyclerView
                    }
                }
        }

        // Wire up the Chat button (you'll need to implement ChatFragment.newInstance)
        binding.btnChat.setOnClickListener {
            subletId?.let { id ->
                val chatFrag = ChatFragment.newInstance(id)  // ← Make sure ChatFragment has this method
                parentFragmentManager.beginTransaction()
                    .replace(R.id.auth_container, chatFrag)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}
