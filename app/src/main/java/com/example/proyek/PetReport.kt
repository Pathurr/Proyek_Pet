package com.example.proyek

data class PetReport(
    val id: String,
    val type: ReportType, // LOST / FOUND
    val animalName: String?,
    val animalType: String,
    val animalColor: String,
    val ownerEmail: String?,
    val photoUrl: String?
)
enum class ReportType {
    LOST, FOUND
}

