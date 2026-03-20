package dev.slne.surf.stats.api

import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.surfapi.core.api.util.requiredService
import java.util.*

private val api = requiredService<SurfStatsApi>()


interface SurfStatsApi {

    suspend fun processPlayerStats(playerUuid: UUID)

    suspend fun processAllPlayerStats(uuids: Set<UUID>)

    suspend fun getPlayerStats(playerUuid: UUID): PlayerStats

    suspend fun saveStats(playerUuid: UUID, stats: PlayerStats)

    companion object : SurfStatsApi by api {
        val INSTANCE get() = api
    }
}
