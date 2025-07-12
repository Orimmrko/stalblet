package com.example.stalblet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.stalblet.model.Sublet
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.example.stalblet.SubletDetailFragment


class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap

    companion object {
        fun newInstance() = MapFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate your fragment_map.xml, which must contain:
        // <fragment
        //     android:id="@+id/map"
        //     android:name="com.google.android.gms.maps.SupportMapFragment"
        //     ... />
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Find the SupportMapFragment inside this fragment and request the map
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        // … your existing setup …

        //  Add this block to load sublets:
        val db = FirebaseFirestore.getInstance()
        db.collection("sublets")
            .addSnapshotListener { snaps, error ->
                if (error != null || snaps == null) return@addSnapshotListener
                map.clear()
                for (doc in snaps.documents) {
                    val s = doc.toObject(Sublet::class.java)!!.copy(id = doc.id)
                    val latLng = LatLng(s.location.latitude, s.location.longitude)
                    map.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(s.title)
                    )?.tag = s   // store Sublet on the marker
                }
            }

        // Handle marker taps:
        map.setOnMarkerClickListener { marker ->
            val sublet = marker.tag as? Sublet ?: return@setOnMarkerClickListener false
            parentFragmentManager.beginTransaction()
                .replace(R.id.auth_container, SubletDetailFragment.newInstance(sublet.id))
                .addToBackStack("sublet_detail")
                .commit()
            true
        }
    }
}
