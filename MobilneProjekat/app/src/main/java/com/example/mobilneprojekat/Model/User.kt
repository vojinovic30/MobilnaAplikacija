package com.example.mobilneprojekat.Model

data class User(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val phoneNumber: String = "",
    val imageUrl: String? = null,
    val points: Int = 0
)