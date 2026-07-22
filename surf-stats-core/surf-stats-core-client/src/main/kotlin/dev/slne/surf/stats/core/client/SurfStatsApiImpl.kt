package dev.slne.surf.stats.core.client

import com.google.auto.service.AutoService
import dev.slne.clan.api.clan.findClanByPlayer
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.stats.api.SurfStatsApi
import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.api.model.PlayerStatsBatch
import dev.slne.surf.stats.core.client.repository.PlayerStatsRepository
import dev.slne.surf.stats.core.client.service.StatisticsManagerService
import dev.slne.surf.stats.core.common.packets.SaveStatsRequestPacket
import java.util.*

/**
 * Implementation of the main SurfStats API.
 *
 * Sends absolute current values to the microservice for both the current-stats
 * (`saveStats`) and event-log (`saveDiffStats`) paths. Delta computation is
 * owned by the microservice, which stores the per-tuple baseline in
 * `player_stats.last_diff_value`.
 */
@AutoService(SurfStatsApi::class)
class SurfStatsApiImpl : SurfStatsApi {
    private val log = logger()

    suspend fun onPlayerJoin(playerUuid: UUID) {
        StatisticsManagerService.trackPlayer(playerUuid)
    }

    suspend fun onPlayerQuit(playerUuid: UUID) {
        processPlayerStats(playerUuid)
        StatisticsManagerService.untrackPlayer(playerUuid)
    }

    override suspend fun processPlayerStats(playerUuid: UUID) {
        runCatching {
            val currentStats = PlayerStatsRepository.loadStats(playerUuid)

            if (currentStats.isEmpty()) {
                return@runCatching
            }

            saveStats(
                playerUuid = playerUuid,
                stats = currentStats
            )

            saveDiffStats(
                playerUuid = playerUuid,
                stats = currentStats
            )
        }.onFailure { error ->
            log.atSevere()
                .withCause(error)
                .log("Failed to process stats for player $playerUuid")
        }
    }

    override suspend fun processAllPlayerStats(uuids: Set<UUID>) {
        if (uuids.isEmpty()) {
            return
        }

        val allCurrentStats = PlayerStatsRepository.loadAllStats(uuids)
            .filter { it.isNotEmpty() }

        if (allCurrentStats.isEmpty()) {
            return
        }

        val serverName = SurfCoreApi.getCurrentServerName()

        val currentBatches = allCurrentStats.map { stats ->
            PlayerStatsBatch(
                playerUuid = stats.playerUuid,
                stats = stats,
                serverName = serverName,
                clanUuid = stats.playerUuid.findClanByPlayer()?.uuid
            )
        }

        val currentResponse = statsInstance.rabbitApi.sendRequest(
            SaveStatsRequestPacket(
                batches = currentBatches,
                type = SaveStatsRequestPacket.Type.CURRENT
            )
        )
        val currentFailed = currentResponse.value
        if (currentFailed.isNotEmpty()) {
            log.atWarning()
                .log("Failed to save current stats for ${currentFailed.size}/${currentBatches.size} players")
        }

        val diffResponse = statsInstance.rabbitApi.sendRequest(
            SaveStatsRequestPacket(
                batches = currentBatches,
                type = SaveStatsRequestPacket.Type.DIFFERENCE
            )
        )
        val diffFailed = diffResponse.value
        if (diffFailed.isNotEmpty()) {
            log.atWarning()
                .log("Failed to save diff stats for ${diffFailed.size}/${currentBatches.size} players")
        }
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

    override suspend fun saveDiffStats(
        playerUuid: UUID,
        stats: PlayerStats,
    ) {
        statsInstance.rabbitApi.sendRequest(
            SaveStatsRequestPacket(
                batches = listOf(
                    PlayerStatsBatch(
                        playerUuid = playerUuid,
                        stats = stats,
                        serverName = SurfCoreApi.getCurrentServerName(),
                        clanUuid = playerUuid.findClanByPlayer()?.uuid
                    )
                ),
                type = SaveStatsRequestPacket.Type.DIFFERENCE
            )
        )
    }
}

val surfStatsApiImpl get() = SurfStatsApi.INSTANCE as SurfStatsApiImpl
