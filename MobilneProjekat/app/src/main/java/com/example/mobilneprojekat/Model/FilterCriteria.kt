package com.example.mobilneprojekat.Model

import com.google.firebase.Timestamp

data class FilterCriteria(
    val userEmail: String? = null,
    val name: String? = null,
    val dateRange: Pair<Timestamp?, Timestamp?> = Pair(null, null),
    val type: String? = null,
    val radius: Float? = null // Radius in meters
)