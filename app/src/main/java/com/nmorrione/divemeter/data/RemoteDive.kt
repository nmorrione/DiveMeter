package com.nmorrione.divemeter.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

/** Mirrors a row already stored in the `dives` table — includes server-generated fields. */
@Serializable
data class RemoteDive(
    val id: Long,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("owner_nickname") val ownerNickname: String,
    @SerialName("spot_name") val spotName: String,
    @SerialName("height_meters") val heightMeters: Double,
    val latitude: Double,
    val longitude: Double,
    val method: String,
    val description: String = "",
    val rating: Int = 0,
    @SerialName("created_at") val createdAt: String
)

/** Body sent when creating a dive — omits id/created_at, which Postgres fills in. */
@Serializable
data class RemoteDiveInsert(
    @SerialName("owner_id") val ownerId: String,
    @SerialName("owner_nickname") val ownerNickname: String,
    @SerialName("spot_name") val spotName: String,
    @SerialName("height_meters") val heightMeters: Double,
    val latitude: Double,
    val longitude: Double,
    val method: String,
    val description: String = "",
    val rating: Int = 0
)

fun RemoteDive.toDive(): Dive = Dive(
    id = id,
    ownerId = ownerId,
    ownerNickname = ownerNickname,
    spotName = spotName,
    heightMeters = heightMeters,
    latitude = latitude,
    longitude = longitude,
    timestampMillis = try {
        OffsetDateTime.parse(createdAt).toInstant().toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    },
    method = try {
        DiveMethod.valueOf(method)
    } catch (e: IllegalArgumentException) {
        DiveMethod.MANUAL
    },
    description = description,
    rating = rating
)
