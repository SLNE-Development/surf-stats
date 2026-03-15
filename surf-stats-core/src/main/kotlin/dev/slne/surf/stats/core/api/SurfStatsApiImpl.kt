package dev.slne.surf.stats.core.api

import com.google.auto.service.AutoService
import dev.slne.clan.api.clan.findClanByPlayer
import dev.slne.surf.stats.api.StatsInstance
import dev.slne.surf.stats.api.SurfStatsApi
import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.api.model.PlayerStatsBatch
import dev.slne.surf.stats.api.model.StatEntry
import dev.slne.surf.stats.api.service.StatisticsManagerService
import dev.slne.surf.stats.core.database.StatsDatabaseService
import dev.slne.surf.stats.core.repository.PlayerStatsRepository
import dev.slne.surf.surfapi.core.api.util.logger
import java.util.*

/**
 * Implementation of the main SurfStats API.
 * Uses diff-based statistics: computes the difference between current and snapshot values
 * and stores each diff as a new database entry with clan assignment and timestamp.
 */
@AutoService(SurfStatsApi::class)
class SurfStatsApiImpl() : SurfStatsApi {
    private val log = logger()

    suspend fun onPlayerJoin(playerUuid: UUID, playerName: String) {
        val clanUuid = playerUuid.findClanByPlayer()?.uuid

        StatisticsManagerService.trackPlayer(playerUuid, playerName)
        log.atInfo().log("Player $playerName ($playerUuid) joined, now tracking stats with clanUuid=$clanUuid")
    }

    suspend fun onPlayerQuit(playerUuid: UUID, playerName: String) {
        // Compute final diffs and save before untracking
        processPlayerStats(playerUuid, playerName)
        StatisticsManagerService.untrackPlayer(playerUuid)
        log.atInfo().log("Player $playerName ($playerUuid) quit, stopped tracking stats")
    }

    override suspend fun processPlayerStats(playerUuid: UUID, playerName: String): Result<PlayerStatsBatch> {
        return runCatching {
            val diffs = StatisticsManagerService.computeDiffs(playerUuid, playerName)
            val clanUuid = playerUuid.findClanByPlayer()?.uuid

            if (diffs.isNotEmpty()) {
                val currentStats = PlayerStatsRepository.loadStats(playerUuid, playerName)
                    ?: PlayerStats.empty(playerUuid, playerName)

                StatsDatabaseService.saveDiffBatch(
                    playerUuid = playerUuid,
                    playerName = playerName,
                    dataVersion = currentStats.dataVersion,
                    diffs = diffs,
                    clanUuid = clanUuid
                )
            }

            PlayerStatsBatch(
                player = PlayerStats(playerUuid, playerName, 0, diffs),
                serverName = StatsInstance.serverName,
                clanUuid = clanUuid
            )
        }.onFailure { error ->
            log.atSevere()
                .withCause(error)
                .log("Failed to process stats for player $playerName ($playerUuid)")
        }
    }

    override suspend fun processAllPlayerStats(players: Map<UUID, String>): List<PlayerStatsBatch> {
        val batches = mutableListOf<PlayerStatsBatch>()
        val diffsByPlayer = mutableMapOf<UUID, List<StatEntry>>()

        for ((uuid, name) in players) {
            val diffs = StatisticsManagerService.computeDiffs(uuid, name)
            val clanUuid = uuid.findClanByPlayer()?.uuid

            if (diffs.isNotEmpty()) {
                diffsByPlayer[uuid] = diffs
                batches.add(
                    PlayerStatsBatch(
                        player = PlayerStats(uuid, name, 0, diffs),
                        serverName = StatsInstance.serverName,
                        clanUuid = clanUuid
                    )
                )
            }
        }

        if (batches.isNotEmpty()) {
            val failedCount = StatsDatabaseService.saveDiffBatches(batches, diffsByPlayer)
            if (failedCount > 0) {
                log.atWarning().log("Failed to save diff stats for $failedCount/${batches.size} players")
            }
        }

        return batches
    }

    override suspend fun getPlayerStats(playerUuid: UUID, playerName: String): PlayerStats? {
        return PlayerStatsRepository.loadStats(playerUuid, playerName)
    }

    override suspend fun saveCustomStat(playerUuid: UUID, playerName: String, key: String, value: Long) {
        saveCustomStats(playerUuid, playerName, mapOf(key to value))
    }

    override suspend fun saveCustomStats(playerUuid: UUID, playerName: String, stats: Map<String, Long>) {
        StatsDatabaseService.saveCustomStats(playerUuid, playerName, stats)
    }

    override fun extractCategories(stats: PlayerStats): Set<String> {
        return stats.categories()
    }

    override fun extractStatKeys(stats: PlayerStats): Set<String> {
        return stats.statKeys()
    }
}

val surfStatsApiImpl get() = SurfStatsApi.INSTANCE as SurfStatsApiImpl
