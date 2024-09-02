package com.example.mobilneprojekat.Model

import com.google.firebase.Timestamp

data class MyObject(
    val id: String = "",
    val name: String? = null,
    val description: String? = null,
    val type: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrls: List<String> = emptyList(),
    val userEmail: String? = null,
    val dateCreated: Timestamp? = null,
    val rating: Float? = 0f,
)