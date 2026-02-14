package dev.slne.surf.stats.api

import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.api.model.PlayerStatsBatch
import java.util.UUID

/**
 * Main API interface for the Surf Stats system.
 * Provides high-level operations for statistics management.
 *
 * Obtain an instance via Bukkit's ServicesManager:
 * ```kotlin
 * val api = server.servicesManager.getRegistration(SurfStatsApi::class.java)?.provider
 * ```
 */
interface SurfStatsApi {
    /**
     * The server name used for stats attribution.
     */
    val serverName: String

    /**
     * Processes statistics for a player (e.g., on logout).
     * Loads stats from filesystem and prepares for database migration.
     *
     * @param playerUuid The player's UUID
     * @param playerName The player's name
     * @return Result containing the batch ready for database insertion
     */
    suspend fun processPlayerStats(playerUuid: UUID, playerName: String): Result<PlayerStatsBatch>

    /**
     * Processes statistics for multiple players (e.g., on server shutdown).
     *
     * @param players Map of UUID to player name
     * @return List of batches ready for database insertion
     */
    suspend fun processAllPlayerStats(players: Map<UUID, String>): List<PlayerStatsBatch>

    /**
     * Loads player statistics without processing.
     *
     * @param playerUuid The player's UUID
     * @param playerName The player's name
     * @return The player's statistics, or null if not found
     */
    suspend fun getPlayerStats(playerUuid: UUID, playerName: String): PlayerStats?

    /**
     * Extracts all unique category names from a player's stats.
     * Useful for populating the stat_categories table.
     */
    fun extractCategories(stats: PlayerStats): Set<String> = stats.categories()

    /**
     * Extracts all unique stat key names from a player's stats.
     * Useful for populating the stat_keys table.
     */
    fun extractStatKeys(stats: PlayerStats): Set<String> = stats.statKeys()
}
