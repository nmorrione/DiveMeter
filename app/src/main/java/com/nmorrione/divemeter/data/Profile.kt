package com.nmorrione.divemeter.data

import kotlinx.serialization.Serializable

@Serializable
data class ProfileInsert(
    val id: String,
    val nickname: String
)
