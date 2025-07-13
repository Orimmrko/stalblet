package com.example.stalblet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.firestore.FirebaseFirestore

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("MapFragment", "++++ onViewCreated ++++")

        // 1) Initialize the SupportMapFragment
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.google_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 2) Initialize the AutocompleteSupportFragment
        val autocompleteFragment = childFragmentManager
            .findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                place.latLng?.let { latLng ->
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
            override fun onError(status: Status) {
                Log.e("MapFragment", "Autocomplete error: $status")
            }
        })
    }
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Optional: set an initial camera position
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(32.0, 35.0), 12f))

        // Listen for real-time updates
        db.collection("sublets")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("MapFragment", "Listen failed", error)
                    return@addSnapshotListener
                }

                map.clear()

                snapshots?.forEach { doc ->
                    val gp = doc.getGeoPoint("location") ?: return@forEach
                    val title = doc.getString("title") ?: "No title"
                    val pos = LatLng(gp.latitude, gp.longitude)

                    // 1) Add the marker
                    val marker = map.addMarker(
                        MarkerOptions()
                            .position(pos)
                            .title(title)
                    )
                    // 2) Tag it with the Firestore document ID
                    marker?.tag = doc.id
                }
            }

        // Handle marker taps by reading the doc ID from tag
        map.setOnMarkerClickListener { marker ->
            val docId = marker.tag as? String
            if (docId != null) {
                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.auth_container,
                        SubletDetailFragment().apply {
                            arguments = Bundle().apply {
                                putString("subletId", docId)
                            }
                        }
                    )
                    .addToBackStack(null)
                    .commit()
            }
            true
        }
    }


}
