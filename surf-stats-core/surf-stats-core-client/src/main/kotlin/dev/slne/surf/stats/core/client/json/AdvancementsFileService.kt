package dev.slne.surf.stats.core.client.json

import dev.slne.surf.stats.api.model.AdvancementEntry
import java.nio.file.Path
import java.util.*

/**
 * Reads player advancement files from `<world>/advancements/`.
 *
 * Returns bare entry lists rather than [dev.slne.surf.stats.api.model.PlayerAdvancements];
 * attaching the player and server identity is the repository's job.
 */
interface AdvancementsFileService {
    /**
     * Initializes the service with the base advancements directory.
     *
     * @param advancementsDirectory the directory containing player advancement files
     */
    suspend fun initialize(advancementsDirectory: Path)

    /**
     * Reads and parses a player's advancement file.
     *
     * @return a successful [Result] with an empty list when the file does not
     *   exist, or a failure when it exists but cannot be read or parsed
     */
    suspend fun loadAdvancements(playerUuid: UUID): Result<List<AdvancementEntry>>

    /**
     * Reads advancements for multiple players, allowing partial success.
     */
    suspend fun loadAllAdvancements(playerUuids: Set<UUID>): Map<UUID, Result<List<AdvancementEntry>>>

    /**
     * Gets the file path for a player's advancement file.
     */
    fun getAdvancementsFilePath(playerUuid: UUID): Path

    /**
     * Checks whether an advancement file exists for the given player.
     */
    suspend fun advancementsExist(playerUuid: UUID): Boolean

    companion object : AdvancementsFileService by AdvancementsFileServiceImpl
}
