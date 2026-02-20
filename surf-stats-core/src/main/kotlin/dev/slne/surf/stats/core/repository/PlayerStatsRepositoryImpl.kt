package dev.slne.surf.stats.core.repository

import com.google.auto.service.AutoService
import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.api.repository.PlayerStatsRepository
import dev.slne.surf.stats.api.service.StatsFileService
import dev.slne.surf.stats.api.service.fileService
import net.kyori.adventure.util.Services
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Implementation of PlayerStatsRepository that delegates to StatsFileService.
 */
@AutoService(PlayerStatsRepository::class)
class PlayerStatsRepositoryImpl() : PlayerStatsRepository, Services.Fallback {

    private val logger = LoggerFactory.getLogger(PlayerStatsRepositoryImpl::class.java)

    override suspend fun loadStats(uuid: UUID): PlayerStats? {
        return fileService.loadStatistics(uuid).getOrNull()
    }

    override suspend fun loadStats(uuid: UUID, name: String): PlayerStats? {
        return fileService.loadStatistics(uuid, name).getOrNull()
    }

    override suspend fun loadAllStats(uuids: Set<UUID>): List<PlayerStats> {
        val results = fileService.loadAllStatistics(uuids)
        return processResults(results)
    }

    override suspend fun loadAllStats(players: Map<UUID, String>): List<PlayerStats> {
        val results = fileService.loadAllStatistics(players)
        return processResults(results)
    }

    override suspend fun statsExist(uuid: UUID): Boolean {
        return fileService.statsExist(uuid)
    }

    private fun processResults(results: Map<UUID, Result<PlayerStats>>): List<PlayerStats> {
        val successful = results.values.mapNotNull { it.getOrNull() }
        val failed = results.filterValues { it.isFailure }.keys

        if (failed.isNotEmpty()) {
            logger.warn("Failed to load stats for {} players: {}", failed.size, failed)
        }

        logger.info("Loaded stats for {}/{} players", successful.size, results.size)
        return successful
    }
}
