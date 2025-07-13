package com.example.stalblet

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.stalblet.databinding.FragmentAddSubletBinding
import com.example.stalblet.model.Sublet
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.location.Geocoder



class AddSubletFragment : Fragment() {
    private var _binding: FragmentAddSubletBinding? = null
    private val binding get() = _binding!!

    private val db      = FirebaseFirestore.getInstance()
    private val auth    = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    @SuppressLint("SetTextI18n")
    private val pickImages =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                binding.previewContainer.removeAllViews()
                uris.forEach { uri ->
                    val iv = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_image_preview, binding.previewContainer, false)
                            as androidx.appcompat.widget.AppCompatImageView
                    iv.setImageURI(uri)
                    binding.previewContainer.addView(iv)
                }
                pendingImages = uris
                binding.tvPhotoCount.text = "${uris.size} photo(s) selected"
            }
        }
    private var pendingImages: List<Uri> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSubletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("AddSublet", "onViewCreated called, btnSubmit=${true}")

        binding.btnSubmit.setOnClickListener {
            Log.d("AddSublet", "Submit clicked!")
            binding.progressBar.isVisible = true}

        setHasOptionsMenu(false)

        binding.btnPickPhotos.setOnClickListener {
            pickImages.launch("image/*")
        }

        binding.btnSubmit.setOnClickListener {
            // Show spinner
            binding.progressBar.isVisible = true
            Log.d("AddSublet", "Submit clicked")

            val title = binding.etTitle.text.toString().trim()
            val desc  = binding.etDescription.text.toString().trim()
            val addressString = binding.etAddress.text.toString().trim()
            // Validation
            if (title.isEmpty() || desc.isEmpty() || addressString.isEmpty()) {
                binding.progressBar.isVisible = false
                Toast.makeText(requireContext(),
                    "Please fill title, description, and address",
                    Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Geocode address → GeoPoint
            val geo: GeoPoint? = try {
                val coder = Geocoder(requireContext())
                val results = coder.getFromLocationName(addressString, 1)
                if (results.isNullOrEmpty()) {
                    null
                } else {
                    val loc = results[0]
                    GeoPoint(loc.latitude, loc.longitude)
                }
            } catch (e: Exception) {
                null
            }

            if (geo == null) {
                binding.progressBar.isVisible = false
                Toast.makeText(requireContext(),
                    "Unable to find location for that address",
                    Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Auth check & upload (same as before, now using geo)
            val ownerId = auth.currentUser?.uid
            if (ownerId == null) {
                binding.progressBar.isVisible = false
                Toast.makeText(requireContext(),
                    "You must be signed in to add a listing",
                    Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            uploadSublet(title, desc, geo, ownerId, pendingImages)
        }
    }

    private fun uploadSublet(
        title: String,
        desc: String,
        geo: GeoPoint,
        ownerId: String,
        images: List<Uri>
    ) {
        val timestamp = Timestamp.now()
        val docRef = db.collection("sublets").document()
        Log.d("AddSublet", "uploadSublet: doc=${docRef.id}, images=${images.size}")

        // Helper to always hide spinner + pop
        fun finish(success: Boolean, msg: String) {
            binding.progressBar.isVisible = false
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            if (success) parentFragmentManager.popBackStack()
        }

        // No images → simple write
        if (images.isEmpty()) {
            Log.d("AddSublet", "No images branch")
            val sub = Sublet(
                id          = docRef.id,
                ownerId     = ownerId,
                title       = title,
                description = desc,
                location    = geo,
                imageUrls   = emptyList(),
                timestamp   = timestamp
            )
            docRef.set(sub)
                .addOnSuccessListener {
                    Log.d("AddSublet", "Firestore write success")
                    finish(true, "Listing added!")
                }
                .addOnFailureListener { e ->
                    Log.e("AddSublet", "Firestore write failed", e)
                    finish(false, "Failed to add listing: ${e.message}")
                }
            return
        }

        // With images → upload then write
        Log.d("AddSublet", "Uploading ${images.size} images")
        val uploaded = mutableListOf<String>()
        images.forEachIndexed { idx, uri ->
            val imgRef = storage.child("sublet_images/${docRef.id}/img_$idx.jpg")
            imgRef.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception!!
                    imgRef.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    uploaded += downloadUri.toString()
                    Log.d("AddSublet", "Image $idx uploaded: $downloadUri")

                    if (uploaded.size == images.size) {
                        Log.d("AddSublet", "All images uploaded, writing doc")
                        val sub = Sublet(
                            id          = docRef.id,
                            ownerId     = ownerId,
                            title       = title,
                            description = desc,
                            location    = geo,
                            imageUrls   = uploaded,
                            timestamp   = timestamp
                        )
                        docRef.set(sub)
                            .addOnSuccessListener {
                                Log.d("AddSublet", "Firestore write success")
                                finish(true, "Listing added!")
                            }
                            .addOnFailureListener { e ->
                                Log.e("AddSublet", "Firestore write failed", e)
                                finish(false, "Failed to add listing: ${e.message}")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AddSublet", "Image upload failed", e)
                    finish(false, "Image upload failed: ${e.message}")
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
