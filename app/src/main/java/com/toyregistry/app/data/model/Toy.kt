package com.toyregistry.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Toy(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val storageLocation: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val imageUrls: List<String> = emptyList(),
    val userId: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)
