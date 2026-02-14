package dev.slne.surf.stats.api.model

/**
 * Represents a batch of player stats ready for database insertion.
 * Includes server context for the player_stats table.
 */
data class PlayerStatsBatch(
    val player: PlayerStats,
    val serverName: String
)
