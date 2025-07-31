package com.example.stalblet

import com.google.firebase.Timestamp

/**
 * Mirrors a document in Firestore under “conversations/{subletId}”
 */
data class Conversation(
    val subletId: String            = "",
    val subletTitle: String         = "",
    val participants: List<String>  = emptyList(),
    val lastMessage: String         = "",
    val lastTimestamp: Timestamp?   = null
)
