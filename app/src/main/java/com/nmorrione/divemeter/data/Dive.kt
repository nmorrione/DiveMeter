package com.nmorrione.divemeter.data

enum class DiveMethod {
    MANUAL,
    VIDEO,
    BAROMETER
}

data class Dive(
    val id: Long = 0,
    val ownerId: String = "",
    val ownerNickname: String = "",
    val spotName: String,
    val heightMeters: Double,
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long,
    val method: DiveMethod,
    val description: String = "",
    val rating: Int = 0
)
