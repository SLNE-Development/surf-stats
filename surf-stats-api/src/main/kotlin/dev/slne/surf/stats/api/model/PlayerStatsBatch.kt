package dev.slne.surf.stats.api.model

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import kotlinx.serialization.Serializable

/**
 * Represents a batch of player stats ready for database insertion.
 * Includes server context and clan assignment for the player_stats table.
 */
@Serializable
data class PlayerStatsBatch(
    val playerUuid: SerializableUUID,
    val stats: PlayerStats,
    val serverName: String,
    val clanUuid: SerializableUUID? = null,
    val dataVersion: Int = 0
)
