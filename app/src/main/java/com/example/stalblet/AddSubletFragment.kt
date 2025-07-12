package com.example.stalblet

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.stalblet.databinding.FragmentAddSubletBinding
import com.example.stalblet.model.Sublet
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage

class AddSubletFragment : Fragment() {

    private var _binding: FragmentAddSubletBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance() = AddSubletFragment()
        private const val PICK_IMAGES = 1001
    }

    private val storage     = FirebaseStorage.getInstance()
    private val db          = FirebaseFirestore.getInstance()
    private val auth        = FirebaseAuth.getInstance()
    private val selectedUris = mutableListOf<Uri>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSubletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Pick photos
        binding.btnPickPhotos.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(intent, PICK_IMAGES)
        }

        // Submit
        binding.btnSubmit.setOnClickListener {
            val uploadTasks = selectedUris.map { uri ->
                storage.reference
                    .child("sublet_images/${auth.uid}/${uri.lastPathSegment}")
                    .putFile(uri)
                    .continueWithTask { it.result.storage.downloadUrl }
            }

            Tasks.whenAllSuccess<Uri>(uploadTasks)
                .addOnSuccessListener { urls ->
                    val lat = binding.etLatitude.text.toString().toDoubleOrNull() ?: 0.0
                    val lng = binding.etLongitude.text.toString().toDoubleOrNull() ?: 0.0
                    val sublet = Sublet(
                        ownerId     = auth.currentUser?.uid ?: "",
                        title       = binding.etTitle.text.toString(),
                        description = binding.etDescription.text.toString(),
                        location    = GeoPoint(lat, lng),
                        imageUrls   = urls.map { it.toString() },
                        timestamp   = Timestamp.now()
                    )
                    db.collection("sublets")
                        .add(sublet)
                        .addOnSuccessListener {
                            parentFragmentManager.popBackStack()
                        }
                }
        }
    }

    @SuppressLint("SetTextI18n")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_IMAGES && resultCode == Activity.RESULT_OK && data != null) {
            selectedUris.clear()
            data.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) {
                    selectedUris.add(clip.getItemAt(i).uri)
                }
            } ?: data.data?.let { selectedUris.add(it) }

            binding.tvPhotoCount.text = "${selectedUris.size} photo(s) selected"
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
