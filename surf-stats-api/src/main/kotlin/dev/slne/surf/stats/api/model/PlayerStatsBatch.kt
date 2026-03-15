package dev.slne.surf.stats.api.model

import java.util.UUID

/**
 * Represents a batch of player stats ready for database insertion.
 * Includes server context and clan assignment for the player_stats table.
 */
data class PlayerStatsBatch(
    val player: PlayerStats,
    val serverName: String,
    val clanUuid: UUID? = null
)
