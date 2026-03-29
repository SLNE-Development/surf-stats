package dev.slne.surf.stats.core.client.repository

import dev.slne.surf.stats.api.model.PlayerStats
import java.util.*

/**
 * Repository interface for loading player statistics from the filesystem.
 */
interface PlayerStatsRepository {
    /**
     * Loads statistics for a single player.
     *
     * @param uuid The player's UUID
     * @return The player's statistics, or null if not found
     */
    suspend fun loadStats(uuid: UUID): PlayerStats

    /**
     * Loads statistics for multiple players.
     *
     * @param uuids Set of player UUIDs to load
     * @return List of successfully loaded player statistics
     */
    suspend fun loadAllStats(uuids: Set<UUID>): List<PlayerStats>

    /**
     * Checks if statistics exist for a player.
     *
     * @param uuid The player's UUID
     * @return true if stats file exists
     */
    suspend fun statsExist(uuid: UUID): Boolean

    companion object : PlayerStatsRepository by PlayerStatsRepositoryImpl
}
