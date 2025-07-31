package com.example.stalblet.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Sublet(
    val id: String           = "",
    val ownerId: String      = "",
    val title: String        = "",
    val description: String  = "",
    val location: GeoPoint   = GeoPoint(0.0, 0.0),
    val imageUrls: List<String> = emptyList(),

    // NEW: whether to show on the map
    val visible: Boolean     = true,

    val startDate: Timestamp = Timestamp.now(),
    val endDate:   Timestamp = Timestamp.now(),
    val timestamp: Timestamp = Timestamp.now()
)
