package dev.slne.surf.stats.core.service

import com.google.auto.service.AutoService
import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.api.model.StatEntry
import dev.slne.surf.stats.api.service.StatisticsManagerService
import dev.slne.surf.stats.core.repository.PlayerStatsRepository
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@AutoService(StatisticsManagerService::class)
class StatisticsManagerServiceImpl : StatisticsManagerService {
    private val _snapshotMap = ConcurrentHashMap<UUID, PlayerStats>()
    override val snapshotMap get() = _snapshotMap.toMap()

    private val logger = LoggerFactory.getLogger(StatisticsManagerServiceImpl::class.java)

    override suspend fun trackPlayer(uuid: UUID, name: String) {
        val stats = PlayerStatsRepository.loadStats(uuid, name) ?: PlayerStats.empty(uuid, name)
        _snapshotMap[uuid] = stats
        logger.info("Now tracking player {} ({})", name, uuid)
        logger.info("Current stats: {}", stats)
    }

    override suspend fun computeDiffs(uuid: UUID, name: String): List<StatEntry> {
        val currentStats = PlayerStatsRepository.loadStats(uuid, name)
        if (currentStats == null) {
            logger.warn("Could not load current stats for player {} ({})", name, uuid)
            return emptyList()
        }

        val snapshot = _snapshotMap[uuid] ?: PlayerStats.empty(uuid, name)
        val diffs = computeStatDiffs(snapshot, currentStats)

        // Update snapshot to current values
        _snapshotMap[uuid] = currentStats

        if (diffs.isNotEmpty()) {
            logger.debug("Computed {} diff entries for player {} ({})", diffs.size, name, uuid)
        }

        logger.info("Diff: {}", diffs)
        return diffs
    }

    private fun computeStatDiffs(snapshot: PlayerStats, current: PlayerStats): List<StatEntry> {
        val snapshotLookup = snapshot.stats.associateBy { it.category to it.key }

        return current.stats.mapNotNull { entry ->
            val oldValue = snapshotLookup[entry.category to entry.key]?.value ?: 0L
            val diff = entry.value - oldValue

            if (diff > 0) {
                StatEntry(entry.category, entry.key, diff)
            } else null
        }
    }

    override fun untrackPlayer(uuid: UUID) {
        _snapshotMap.remove(uuid)
    }

    override fun isTracking(uuid: UUID): Boolean {
        return _snapshotMap.containsKey(uuid)
    }
}
