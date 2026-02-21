package dev.slne.surf.stats.paper.api

import com.google.auto.service.AutoService
import dev.slne.surf.stats.api.SurfStatsApi
import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.api.model.PlayerStatsBatch
import dev.slne.surf.stats.api.repository.playerStatsRepository
import dev.slne.surf.stats.paper.plugin
import net.kyori.adventure.util.Services
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Implementation of the main SurfStats API.
 */
@AutoService(SurfStatsApi::class)
class SurfStatsApiImpl() : SurfStatsApi, Services.Fallback {

    private val logger = LoggerFactory.getLogger(SurfStatsApiImpl::class.java)

    override suspend fun processPlayerStats(playerUuid: UUID, playerName: String): Result<PlayerStatsBatch> {
        return runCatching {
            val stats = playerStatsRepository.loadStats(playerUuid, playerName)
                ?: throw NoSuchElementException("No stats found for player: $playerName ($playerUuid)")

            val batch = PlayerStatsBatch(
                player = stats,
                serverName = plugin.serverName
            )

            plugin.databaseService.saveBatch(batch)

            batch
        }.onFailure { error ->
            logger.error("Failed to process stats for player {} ({}): {}", playerName, playerUuid, error.message)
        }
    }

    override suspend fun processAllPlayerStats(players: Map<UUID, String>): List<PlayerStatsBatch> {
        val statsList = playerStatsRepository.loadAllStats(players)
        val batches = statsList.map { stats ->
            PlayerStatsBatch(
                player = stats,
                serverName = plugin.serverName
            )
        }

        val failedCount = plugin.databaseService.saveBatches(batches)
        if (failedCount > 0) {
            logger.warn("Failed to save stats for {}/{} players to database", failedCount, batches.size)
        }

        return batches
    }

    override suspend fun getPlayerStats(playerUuid: UUID, playerName: String): PlayerStats? {
        return playerStatsRepository.loadStats(playerUuid, playerName)
    }

    override suspend fun saveCustomStat(playerUuid: UUID, playerName: String, key: String, value: Long) {
        saveCustomStats(playerUuid, playerName, mapOf(key to value))
    }

    override suspend fun saveCustomStats(playerUuid: UUID, playerName: String, stats: Map<String, Long>) {
        plugin.databaseService.saveCustomStats(playerUuid, playerName, stats)
    }
}