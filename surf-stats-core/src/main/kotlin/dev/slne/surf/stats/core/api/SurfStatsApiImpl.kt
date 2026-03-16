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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
        StatisticsManagerService.trackPlayer(playerUuid, playerName)
    }

    suspend fun onPlayerQuit(playerUuid: UUID, playerName: String) {
        // Compute final diffs and save before untracking
        processPlayerStats(playerUuid, playerName)
        StatisticsManagerService.untrackPlayer(playerUuid)
    }

    override suspend fun processPlayerStats(playerUuid: UUID, playerName: String): Result<PlayerStatsBatch> {
        return runCatching {
            // Load & compute
            val diffs = StatisticsManagerService.computeDiffs(playerUuid, playerName)
            val clanUuid = playerUuid.findClanByPlayer()?.uuid
            val currentStats = PlayerStatsRepository.loadStats(playerUuid, playerName)
                ?: PlayerStats.empty(playerUuid, playerName)

            // Save
            saveCurrentStats(currentStats)
            if (diffs.isNotEmpty()) {
                saveDiffStats(playerUuid, playerName, currentStats.dataVersion, diffs, clanUuid)
            }

            // Update snapshot
            if (diffs.isNotEmpty()) {
                StatisticsManagerService.updateSnapshot(playerUuid, playerName)
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

    override suspend fun processAllPlayerStats(players: Map<UUID, String>): List<PlayerStatsBatch> = coroutineScope {
        val actualStatsJob = async { saveAllCurrentStats(players) }
        val diffJob = async { computeAndSaveAllDiffs(players) }

        actualStatsJob.await()
        diffJob.await()
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

    @Deprecated("Use PlayerStats.categories() directly", ReplaceWith("stats.categories()"))
    override fun extractCategories(stats: PlayerStats): Set<String> {
        return stats.categories()
    }

    @Deprecated("Use PlayerStats.statKeys() directly", ReplaceWith("stats.statKeys()"))
    override fun extractStatKeys(stats: PlayerStats): Set<String> {
        return stats.statKeys()
    }

    private suspend fun saveCurrentStats(stats: PlayerStats) {
        // I/O
        StatsDatabaseService.saveBatch(
            PlayerStatsBatch(
                player = stats,
                serverName = StatsInstance.serverName,
            )
        )
    }

    private suspend fun saveDiffStats(
        playerUuid: UUID,
        playerName: String,
        dataVersion: Int,
        diffs: List<StatEntry>,
        clanUuid: UUID?
    ) {
        // I/O
        StatsDatabaseService.saveDiffBatch(
            playerUuid = playerUuid,
            playerName = playerName,
            dataVersion = dataVersion,
            diffs = diffs,
            clanUuid = clanUuid
        )
    }

    private suspend fun saveAllCurrentStats(players: Map<UUID, String>) {
        // I/O
        val allCurrentStats = PlayerStatsRepository.loadAllStats(players)
        val statBatches = allCurrentStats.map { stats ->
            PlayerStatsBatch(
                player = stats,
                serverName = StatsInstance.serverName,
            )
        }

        if (statBatches.isNotEmpty()) {
            val failedUuids = StatsDatabaseService.saveBatches(statBatches)
            if (failedUuids.isNotEmpty()) {
                log.atWarning().log("Failed to save actual stats for ${failedUuids.size}/${statBatches.size} players")
            }
        }
    }

    private suspend fun computeAndSaveAllDiffs(players: Map<UUID, String>): List<PlayerStatsBatch> {
        val diffBatches = players.mapNotNull { (uuid, name) ->
            val diffs = StatisticsManagerService.computeDiffs(uuid, name)
            if (diffs.isEmpty()) return@mapNotNull null

            val clanUuid = uuid.findClanByPlayer()?.uuid
            PlayerStatsBatch(
                player = PlayerStats(uuid, name, 0, diffs),
                serverName = StatsInstance.serverName,
                clanUuid = clanUuid
            )
        }

        if (diffBatches.isNotEmpty()) {
            val failedUuids = StatsDatabaseService.saveDiffBatches(diffBatches)
            if (failedUuids.isNotEmpty()) {
                log.atWarning().log("Failed to save diff stats for ${failedUuids.size}/${diffBatches.size} players")
            }

            // Only update snapshots for players whose diffs were persisted successfully
            for ((uuid, name) in players) {
                if (uuid !in failedUuids && diffBatches.any { it.player.uuid == uuid }) {
                    StatisticsManagerService.updateSnapshot(uuid, name)
                }
            }
        }

        return diffBatches
    }
}

val surfStatsApiImpl get() = SurfStatsApi.INSTANCE as SurfStatsApiImpl
