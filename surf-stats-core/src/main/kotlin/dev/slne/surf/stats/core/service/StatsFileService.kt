package dev.slne.surf.stats.core.service

import dev.slne.surf.stats.api.model.PlayerStats
import java.nio.file.Path
import java.util.*

/**
 * Service interface for file-based statistics operations.
 */
interface StatsFileService {
    /**
     * Initializes the service with the base stats directory path.
     *
     * @param statsDirectory The directory containing player stat files
     */
    suspend fun initialize(statsDirectory: Path)

    /**
     * Reads statistics from a player's JSON file.
     *
     * @param playerUuid The player's UUID
     * @return Result containing PlayerStats on success, or failure with exception
     */
    suspend fun loadStatistics(playerUuid: UUID): Result<PlayerStats>

    /**
     * Reads statistics from a player's JSON file with known player name.
     *
     * @param playerUuid The player's UUID
     * @param playerName The player's name
     * @return Result containing PlayerStats on success, or failure with exception
     */
    suspend fun loadStatistics(playerUuid: UUID, playerName: String): Result<PlayerStats>

    /**
     * Reads statistics for multiple players.
     *
     * @param playerUuids Set of player UUIDs
     * @return Map of UUID to Result, allowing partial success
     */
    suspend fun loadAllStatistics(playerUuids: Set<UUID>): Map<UUID, Result<PlayerStats>>

    /**
     * Reads statistics for multiple players with known names.
     *
     * @param players Map of UUID to player name
     * @return Map of UUID to Result, allowing partial success
     */
    suspend fun loadAllStatistics(players: Map<UUID, String>): Map<UUID, Result<PlayerStats>>

    /**
     * Gets the file path for a player's stats file.
     *
     * @param playerUuid The player's UUID
     * @return The path to the stats file
     */
    fun getStatsFilePath(playerUuid: UUID): Path

    /**
     * Checks if a stats file exists for the given player.
     *
     * @param playerUuid The player's UUID
     * @return true if the stats file exists
     */
    suspend fun statsExist(playerUuid: UUID): Boolean

    companion object : StatsFileService by StatsFileServiceImpl
}
