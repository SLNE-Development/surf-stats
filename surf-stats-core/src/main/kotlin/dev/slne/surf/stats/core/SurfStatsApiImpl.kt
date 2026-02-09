package dev.slne.surf.stats.core

import dev.slne.surf.stats.api.SurfStatsApi
import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.api.model.PlayerStatsBatch
import dev.slne.surf.stats.api.repository.PlayerStatsRepository
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Implementation of the main SurfStats API.
 */
class SurfStatsApiImpl(
    private val repository: PlayerStatsRepository,
    override val serverName: String
) : SurfStatsApi {

    private val logger = LoggerFactory.getLogger(SurfStatsApiImpl::class.java)

    override suspend fun processPlayerStats(playerUuid: UUID, playerName: String): Result<PlayerStatsBatch> {
        logger.debug("Processing stats for player: {} ({})", playerName, playerUuid)

        return runCatching {
            val stats = repository.loadStats(playerUuid, playerName)
                ?: throw NoSuchElementException("No stats found for player: $playerName ($playerUuid)")

            val batch = PlayerStatsBatch(
                player = stats,
                serverName = serverName
            )

            logger.info(
                "Processed stats for player {} ({}): {} entries, {} categories, dataVersion={}",
                playerName, playerUuid, stats.stats.size, stats.categories().size, stats.dataVersion
            )

            batch
        }.onFailure { error ->
            logger.error("Failed to process stats for player {} ({}): {}", playerName, playerUuid, error.message)
        }
    }

    override suspend fun processAllPlayerStats(players: Map<UUID, String>): List<PlayerStatsBatch> {
        logger.info("Processing stats for {} players on server '{}'", players.size, serverName)

        val statsList = repository.loadAllStats(players)
        val batches = statsList.map { stats ->
            PlayerStatsBatch(
                player = stats,
                serverName = serverName
            )
        }

        val totalEntries = batches.sumOf { it.player.stats.size }
        logger.info(
            "Processed stats for {}/{} players: {} total entries",
            batches.size, players.size, totalEntries
        )

        return batches
    }

    override suspend fun getPlayerStats(playerUuid: UUID, playerName: String): PlayerStats? {
        return repository.loadStats(playerUuid, playerName)
    }
}
