// app/src/main/java/com/example/stalblet/model/Sublet.kt
package com.example.stalblet.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Sublet(
    val id: String = "",          // Firestore document ID
    val ownerId: String = "",     // UID of the user who posted
    val title: String = "",
    val description: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val imageUrls: List<String> = emptyList(),
    val timestamp: Timestamp = Timestamp.now()
)
