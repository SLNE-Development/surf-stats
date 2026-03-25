@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.slne.surf.stats.core.client.service

import com.github.benmanes.caffeine.cache.Caffeine
import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.api.model.StatEntry
import dev.slne.surf.stats.core.client.repository.PlayerStatsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*

object StatisticsManagerServiceImpl : StatisticsManagerService {
    private val _snapshotMap = Caffeine.newBuilder()
        .maximumSize(10_000)
        .build<UUID, PlayerStats>()

    override val snapshotMap get() = _snapshotMap.asMap().values.toList()

    override suspend fun computeDiffs(uuid: UUID): PlayerStats {
        val currentStats = PlayerStatsRepository.loadStats(uuid)
        val snapshot = _snapshotMap.get(uuid) { _ -> currentStats }
        val diffs = diffEntries(snapshot, currentStats)

        return diffs
    }

    override suspend fun updateSnapshot(uuid: UUID) {
        val currentStats = PlayerStatsRepository.loadStats(uuid)
        _snapshotMap.put(uuid, currentStats)
    }

    internal fun diffEntries(snapshot: PlayerStats, current: PlayerStats): PlayerStats {
        val snapshotLookup = snapshot.stats.associateBy { it.category to it.key }

        val list = current.stats.mapNotNull { entry ->
            val oldValue = snapshotLookup[entry.category to entry.key]?.value ?: 0L
            val diff = entry.value - oldValue

            if (diff > 0) {
                StatEntry(entry.category, entry.key, diff)
            } else null
        }

        return PlayerStats(
            playerUuid = snapshot.playerUuid,
            serverName = snapshot.serverName,
            stats = list
        )
    }

    override suspend fun trackPlayer(uuid: UUID) {
        val currentStats = PlayerStatsRepository.loadStats(uuid)
        _snapshotMap.put(uuid, currentStats)
    }

    override fun untrackPlayer(uuid: UUID) {
        _snapshotMap.invalidate(uuid)
    }

    override fun isTracking(uuid: UUID): Boolean {
        return snapshotMap.any { it.playerUuid == uuid }
    }
}