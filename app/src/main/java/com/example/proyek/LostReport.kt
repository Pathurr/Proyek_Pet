package com.example.proyek

data class LostReport(
    val id: String = "",
    val animalName: String = "",
    val animalType: String = "",
    val animalColor: String = "",
    val description: String = "",
    val locationText: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val contact: String = "",
    val photoUrl: String? = null,
    val ownerUid: String = "",
    val ownerEmail: String? = null,
    val createdAt: Long = 0
)
