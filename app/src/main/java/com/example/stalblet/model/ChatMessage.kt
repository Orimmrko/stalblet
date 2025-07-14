package com.example.stalblet.model

import com.google.firebase.Timestamp

/** Mirrors documents in Firestore under sublets/{subletId}/chat */
data class ChatMessage(
    var id: String               = "",                // will store the document ID
    val senderId: String         = "",                // UID of who sent it
    val text: String             = "",                // the chat text
    val timestamp: Timestamp     = Timestamp.now()    // client-side timestamp
)
