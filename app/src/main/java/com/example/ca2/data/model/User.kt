package com.example.ca2.data.model

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profileImage: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
