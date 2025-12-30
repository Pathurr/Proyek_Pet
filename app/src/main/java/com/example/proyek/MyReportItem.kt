package com.example.proyek

data class MyReportItem(
    val id: String = "",
    val type: String = "", // LOST / FOUND
    val animalName: String? = null,
    val animalType: String? = null,
    val animalColor: String? = null,
    val photoUrl: String? = null
)
