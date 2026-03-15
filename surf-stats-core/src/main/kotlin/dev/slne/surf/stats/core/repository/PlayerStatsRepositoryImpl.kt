package dev.slne.surf.stats.core.repository

import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.core.service.StatsFileService
import dev.slne.surf.surfapi.core.api.util.logger
import java.util.*

/**
 * Implementation of PlayerStatsRepository that delegates to StatsFileService.
 */
object PlayerStatsRepositoryImpl : PlayerStatsRepository {
    private val log = logger()

    override suspend fun loadStats(uuid: UUID): PlayerStats? {
        return StatsFileService.loadStatistics(uuid).getOrNull()
    }

    override suspend fun loadStats(uuid: UUID, name: String): PlayerStats? {
        return StatsFileService.loadStatistics(uuid, name).getOrNull()
    }

    override suspend fun loadAllStats(uuids: Set<UUID>): List<PlayerStats> {
        val results = StatsFileService.loadAllStatistics(uuids)
        return processResults(results)
    }

    override suspend fun loadAllStats(players: Map<UUID, String>): List<PlayerStats> {
        val results = StatsFileService.loadAllStatistics(players)
        return processResults(results)
    }

    override suspend fun statsExist(uuid: UUID): Boolean {
        return StatsFileService.statsExist(uuid)
    }

    private fun processResults(results: Map<UUID, Result<PlayerStats>>): List<PlayerStats> {
        val successful = results.values.mapNotNull { it.getOrNull() }
        val failed = results.filterValues { it.isFailure }.keys

        if (failed.isNotEmpty()) {
            log.atWarning().log("Failed to load stats for {} players: {}", failed.size, failed)
        }

        return successful
    }
}
