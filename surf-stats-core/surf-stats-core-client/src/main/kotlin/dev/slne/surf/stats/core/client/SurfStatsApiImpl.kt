package dev.slne.surf.stats.core.client

import com.google.auto.service.AutoService
import dev.slne.clan.api.clan.findClanByPlayer
import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.stats.api.SurfStatsApi
import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.api.model.PlayerStatsBatch
import dev.slne.surf.stats.core.client.repository.PlayerStatsRepository
import dev.slne.surf.stats.core.client.service.StatisticsManagerService
import dev.slne.surf.stats.core.common.packets.SavePlayerRequestPacket
import dev.slne.surf.stats.core.common.packets.SaveStatsRequestPacket
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
        statsInstance.rabbitApi.sendRequest(
            SavePlayerRequestPacket(
                playerUuid = playerUuid,
                playerName = playerName
            )
        )
    }

    suspend fun onPlayerQuit(playerUuid: UUID) {
        // Compute final diffs and save before untracking
        processPlayerStats(playerUuid)
        StatisticsManagerService.untrackPlayer(playerUuid)
    }

    override suspend fun processPlayerStats(playerUuid: UUID) {
        runCatching {
            // Load & compute
            val diffs = StatisticsManagerService.computeDiffs(playerUuid)
            val currentStats = PlayerStatsRepository.loadStats(playerUuid)

            // Save
            saveStats(
                playerUuid = playerUuid,
                stats = currentStats
            )

            if (diffs.isNotEmpty()) {
                saveDiffStats(
                    playerUuid = playerUuid,
                    diffs = diffs,
                    clanUuid = playerUuid.findClanByPlayer()?.uuid
                )
            }

            // Update snapshot
            if (diffs.isNotEmpty()) {
                StatisticsManagerService.updateSnapshot(playerUuid)
            }
        }.onFailure { error ->
            log.atSevere()
                .withCause(error)
                .log("Failed to process stats for player $playerUuid")
        }
    }

    override suspend fun processAllPlayerStats(
        uuids: Set<UUID>
    ) = coroutineScope {
        val actualStatsJob = async { saveAllCurrentStats(uuids) }
        val diffJob = async { computeAndSaveAllDiffs(uuids) }

        actualStatsJob.await()
        diffJob.await()

        Unit
    }

    override suspend fun getPlayerStats(playerUuid: UUID): PlayerStats {
        return PlayerStatsRepository.loadStats(playerUuid)
    }

    override suspend fun saveStats(playerUuid: UUID, stats: PlayerStats) {
        statsInstance.rabbitApi.sendRequest(
            SaveStatsRequestPacket(
                batches = listOf(
                    PlayerStatsBatch(
                        playerUuid = playerUuid,
                        stats = stats,
                        serverName = SurfCoreApi.getCurrentServerName(),
                        clanUuid = null
                    )
                ),
                type = SaveStatsRequestPacket.Type.CURRENT
            )
        )
    }

    private suspend fun saveDiffStats(
        playerUuid: UUID,
        diffs: PlayerStats,
        clanUuid: UUID?
    ) {
        statsInstance.rabbitApi.sendRequest(
            SaveStatsRequestPacket(
                batches = listOf(
                    PlayerStatsBatch(
                        playerUuid = playerUuid,
                        stats = diffs,
                        serverName = SurfCoreApi.getCurrentServerName(),
                        clanUuid = clanUuid
                    )
                ),
                type = SaveStatsRequestPacket.Type.DIFFERENCE
            )
        )
    }

    private suspend fun saveAllCurrentStats(uuids: Set<UUID>) {
        val allCurrentStats = PlayerStatsRepository.loadAllStats(uuids)
        val statBatches = allCurrentStats.map { entry ->
            PlayerStatsBatch(
                playerUuid = entry.playerUuid,
                stats = entry,
                serverName = SurfCoreApi.getCurrentServerName(),
                clanUuid = entry.playerUuid.findClanByPlayer()?.uuid
            )
        }

        if (statBatches.isNotEmpty()) {
            val failedUuids = statsInstance.rabbitApi.sendRequest(
                SaveStatsRequestPacket(
                    batches = statBatches,
                    SaveStatsRequestPacket.Type.CURRENT
                )
            ).value

            if (failedUuids.isNotEmpty()) {
                log.atWarning().log("Failed to save actual stats for ${failedUuids.size}/${statBatches.size} players")
            }
        }
    }

    private suspend fun computeAndSaveAllDiffs(uuids: Set<UUID>): List<PlayerStatsBatch> {
        val diffBatches = uuids.mapNotNull { uuid ->
            val diffs = StatisticsManagerService.computeDiffs(uuid)
            if (diffs.isEmpty()) {
                return@mapNotNull null
            }

            val clanUuid = uuid.findClanByPlayer()?.uuid
            PlayerStatsBatch(
                playerUuid = uuid,
                stats = diffs,
                serverName = SurfCoreApi.getCurrentServerName(),
                clanUuid = clanUuid
            )
        }

        if (diffBatches.isNotEmpty()) {
            val failedUuids = statsInstance.rabbitApi.sendRequest(
                SaveStatsRequestPacket(
                    batches = diffBatches,
                    SaveStatsRequestPacket.Type.DIFFERENCE
                )
            ).value

            if (failedUuids.isNotEmpty()) {
                log.atWarning().log("Failed to save diff stats for ${failedUuids.size}/${diffBatches.size} players")
            }

            // Only update snapshots for players whose diffs were persisted successfully
            for (uuid in uuids) {
                if (uuid !in failedUuids && diffBatches.any { it.playerUuid == uuid }) {
                    StatisticsManagerService.updateSnapshot(uuid)
                }
            }
        }

        return diffBatches
    }
}

val surfStatsApiImpl get() = SurfStatsApi.INSTANCE as SurfStatsApiImpl
