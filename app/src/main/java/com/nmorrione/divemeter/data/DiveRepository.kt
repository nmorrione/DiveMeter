package com.nmorrione.divemeter.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.exception.PostgrestRestException

sealed class NicknameResult {
    object Success : NicknameResult()
    object AlreadyTaken : NicknameResult()
    data class Error(val message: String) : NicknameResult()
}

private const val UNIQUE_VIOLATION_CODE = "23505"

/** Talks to the shared Supabase backend: anonymous device identity, nickname reservation, dives. */
object DiveRepository {
    private val client get() = SupabaseClientProvider.client

    /** Ensures this device has a stable (persisted) anonymous identity, and returns its uid. */
    suspend fun ensureSignedIn(): String {
        client.auth.awaitInitialization()
        client.auth.currentUserOrNull()?.let { return it.id }
        client.auth.signInAnonymously()
        return client.auth.currentUserOrNull()?.id ?: error("Sign-in failed")
    }

    fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun claimNickname(nickname: String): NicknameResult {
        val uid = ensureSignedIn()
        return try {
            client.postgrest.from("profiles").insert(ProfileInsert(id = uid, nickname = nickname))
            NicknameResult.Success
        } catch (e: PostgrestRestException) {
            if (e.code == UNIQUE_VIOLATION_CODE) NicknameResult.AlreadyTaken else NicknameResult.Error(e.message ?: "Unknown error")
        } catch (e: Exception) {
            NicknameResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun updateNickname(nickname: String): NicknameResult {
        val uid = ensureSignedIn()
        return try {
            client.postgrest.from("profiles").update({
                set("nickname", nickname)
            }) {
                filter { eq("id", uid) }
            }
            NicknameResult.Success
        } catch (e: PostgrestRestException) {
            if (e.code == UNIQUE_VIOLATION_CODE) NicknameResult.AlreadyTaken else NicknameResult.Error(e.message ?: "Unknown error")
        } catch (e: Exception) {
            NicknameResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun fetchDives(): List<Dive> {
        val result = client.postgrest.from("dives").select {
            order(column = "created_at", order = Order.DESCENDING)
        }
        return result.decodeList<RemoteDive>().map { it.toDive() }
    }

    suspend fun insertDive(
        spotName: String,
        heightMeters: Double,
        latitude: Double,
        longitude: Double,
        method: DiveMethod,
        description: String,
        rating: Int,
        ownerNickname: String
    ) {
        val uid = ensureSignedIn()
        client.postgrest.from("dives").insert(
            RemoteDiveInsert(
                ownerId = uid,
                ownerNickname = ownerNickname,
                spotName = spotName,
                heightMeters = heightMeters,
                latitude = latitude,
                longitude = longitude,
                method = method.name,
                description = description,
                rating = rating
            )
        )
    }

    suspend fun deleteDive(id: Long) {
        ensureSignedIn()
        client.postgrest.from("dives").delete {
            filter { eq("id", id) }
        }
    }
}
