package dev.slne.surf.stats.core.client.repository

import dev.slne.surf.stats.api.model.PlayerAdvancements
import java.util.*

/**
 * Loads player advancement snapshots from the filesystem and attaches the
 * current server identity.
 */
interface PlayerAdvancementsRepository {
    /**
     * Loads one player's snapshot. Read failures yield an empty snapshot, which
     * callers must never send — see [loadAllAdvancements].
     */
    suspend fun loadAdvancements(uuid: UUID): PlayerAdvancements

    /**
     * Loads snapshots for multiple players, dropping the ones that failed to read.
     */
    suspend fun loadAllAdvancements(uuids: Set<UUID>): List<PlayerAdvancements>

    companion object : PlayerAdvancementsRepository by PlayerAdvancementsRepositoryImpl
}
