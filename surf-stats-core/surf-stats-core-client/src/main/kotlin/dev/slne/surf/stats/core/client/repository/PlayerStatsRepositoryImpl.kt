package dev.slne.surf.stats.core.client.repository

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.core.client.json.StatsFileService
import java.util.*

/**
 * Implementation of PlayerStatsRepository that delegates to StatsFileService.
 */
object PlayerStatsRepositoryImpl : PlayerStatsRepository {
    private val log = logger()

    override suspend fun loadStats(uuid: UUID): PlayerStats {
        return StatsFileService.loadStatistics(uuid)
            .getOrElse {
                PlayerStats(
                    playerUuid = uuid,
                    serverName = SurfCoreApi.getCurrentServerName()
                )
            }
    }

    override suspend fun loadAllStats(uuids: Set<UUID>): List<PlayerStats> {
        val results = StatsFileService.loadAllStatistics(uuids)
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