package dev.slne.surf.stats.api

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.stats.api.model.PlayerStats
import java.util.*

private val api = requiredService<SurfStatsApi>()

interface SurfStatsApi {

    suspend fun processPlayerStats(playerUuid: UUID)

    suspend fun processAllPlayerStats(uuids: Set<UUID>)

    suspend fun getPlayerStats(playerUuid: UUID): PlayerStats

    suspend fun saveStats(playerUuid: UUID, stats: PlayerStats)

    suspend fun saveDiffStats(playerUuid: UUID, diffs: PlayerStats)

    companion object : SurfStatsApi by api {
        val INSTANCE get() = api
    }
}
