package com.example.stalblet

import android.app.DatePickerDialog
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.stalblet.databinding.FragmentAddSubletBinding
import com.example.stalblet.model.Sublet
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class AddSubletFragment : Fragment() {

    private var _binding: FragmentAddSubletBinding? = null
    private val binding get() = _binding!!

    private val db      = FirebaseFirestore.getInstance()
    private val auth    = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    // Hold the picked images
    private var pendingImages: List<Uri> = emptyList()

    // Hold the selected dates (millis)
    private var startDateMillis: Long? = null
    private var endDateMillis:   Long? = null

    // ---------- ActivityResults ----------
    @SuppressLint("SetTextI18n")
    private val pickImages =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                pendingImages = uris
                binding.previewContainer.removeAllViews()
                uris.forEach { uri ->
                    val iv = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_image_preview, binding.previewContainer, false)
                            as androidx.appcompat.widget.AppCompatImageView
                    iv.setImageURI(uri)
                    binding.previewContainer.addView(iv)
                }
                binding.tvPhotoCount.text = "${uris.size} photo(s) selected"
            }
        }

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchImagePicker()
            else Toast.makeText(requireContext(),
                "Permission required to pick images", Toast.LENGTH_SHORT).show()
        }

    // ---------- Helpers ----------

    private fun launchImagePicker() {
        pickImages.launch("image/*")
    }

    private fun pickDate(onDate: (Long) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                c.set(year, month, day)
                onDate(c.timeInMillis)
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = FragmentAddSubletBinding.inflate(inflater, container, false)
        .also { _binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pick photos button
        binding.btnPickPhotos.setOnClickListener {
            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_IMAGES
            else
                Manifest.permission.READ_EXTERNAL_STORAGE

            if (ContextCompat.checkSelfPermission(requireContext(), perm)
                == PackageManager.PERMISSION_GRANTED
            ) {
                launchImagePicker()
            } else {
                requestPermission.launch(perm)
            }
        }

        // Start date field
        binding.etStartDate.setOnClickListener {
            pickDate { millis ->
                startDateMillis = millis
                binding.etStartDate.setText(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Date(millis))
                )
            }
        }

        // End date field
        binding.etEndDate.setOnClickListener {
            pickDate { millis ->
                endDateMillis = millis
                binding.etEndDate.setText(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Date(millis))
                )
            }
        }

        // Submit button
        binding.btnSubmit.setOnClickListener {
            val title   = binding.etTitle.text.toString().trim()
            val desc    = binding.etDescription.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()

            // Basic validations
            if (title.isEmpty()) {
                binding.etTitle.error = "Required"; return@setOnClickListener
            }
            if (desc.isEmpty()) {
                binding.etDescription.error = "Required"; return@setOnClickListener
            }
            if (address.isEmpty()) {
                binding.etAddress.error = "Required"; return@setOnClickListener
            }
            if (startDateMillis == null) {
                Toast.makeText(requireContext(),"Pick a start date",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (endDateMillis == null) {
                Toast.makeText(requireContext(),"Pick an end date",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (startDateMillis!! > endDateMillis!!) {
                Toast.makeText(requireContext(),"Start must be ≤ end",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pendingImages.isEmpty()) {
                Toast.makeText(requireContext(),"Pick at least one photo",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.isVisible = true

            // Geocode address
            val geo = try {
                val locs = Geocoder(requireContext()).getFromLocationName(address, 1)
                locs?.firstOrNull()?.let { GeoPoint(it.latitude, it.longitude) }
            } catch (_: Exception) { null }

            if (geo == null) {
                binding.progressBar.isVisible = false
                Toast.makeText(requireContext(),
                    "Cannot find that address",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ownerId = auth.currentUser?.uid
            if (ownerId == null) {
                binding.progressBar.isVisible = false
                Toast.makeText(requireContext(),
                    "Must be signed in",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Ready: upload
            uploadSublet(
                title, desc, geo, ownerId,
                pendingImages,
                Timestamp(Date(startDateMillis!!)),
                Timestamp(Date(endDateMillis!!))
            )
        }
    }

    private fun uploadSublet(
        title: String,
        desc: String,
        geo: GeoPoint,
        ownerId: String,
        images: List<Uri>,
        startTs: Timestamp,
        endTs:   Timestamp
    ) {
        val docRef   = db.collection("sublets").document()
        val subletId = docRef.id
        val now      = Timestamp.now()
        val uploaded = mutableListOf<String>()

        fun finish(success: Boolean, msg: String) {
            binding.progressBar.isVisible = false
            Toast.makeText(requireContext(),msg,Toast.LENGTH_SHORT).show()
            if (success) parentFragmentManager.popBackStack()
        }

        images.forEachIndexed { idx, uri ->
            val imgRef = storage.child("sublet_images/$subletId/img_$idx.jpg")
            imgRef.putFile(uri)
                .addOnSuccessListener { snap ->
                    snap.storage.downloadUrl
                        .addOnSuccessListener { url ->
                            uploaded += url.toString()
                            if (uploaded.size == images.size) {
                                // All done: write Firestore
                                val sub = Sublet(
                                    id          = subletId,
                                    ownerId     = ownerId,
                                    title       = title,
                                    description = desc,
                                    location    = geo,
                                    imageUrls   = uploaded,
                                    startDate   = startTs,
                                    endDate     = endTs,
                                    timestamp   = now
                                )
                                docRef.set(sub)
                                    .addOnSuccessListener { finish(true,"Listing added!") }
                                    .addOnFailureListener { e -> finish(false,"Error: ${e.message}") }
                            }
                        }
                        .addOnFailureListener { e -> finish(false,"URL fetch failed: ${e.message}") }
                }
                .addOnFailureListener { e -> finish(false,"Upload failed: ${e.message}") }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
