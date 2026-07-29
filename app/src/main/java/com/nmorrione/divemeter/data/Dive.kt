package com.nmorrione.divemeter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DiveMethod {
    MANUAL,
    VIDEO,
    BAROMETER
}

@Entity(tableName = "dives")
data class Dive(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spotName: String,
    val heightMeters: Double,
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long,
    val method: DiveMethod,
    val videoUri: String? = null
)
