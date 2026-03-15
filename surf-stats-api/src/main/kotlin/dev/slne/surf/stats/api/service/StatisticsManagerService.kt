package dev.slne.surf.stats.api.service

import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.api.model.StatEntry
import dev.slne.surf.surfapi.core.api.util.requiredService
import org.jetbrains.annotations.UnmodifiableView
import java.util.*

private val service = requiredService<StatisticsManagerService>()

interface StatisticsManagerService {
    /** In-memory snapshots of last-known stat values per player */
    val snapshotMap: @UnmodifiableView Map<UUID, PlayerStats>

    /**
     * Start tracking a player (called on join).
     * Loads the current stats from the JSON file as the initial snapshot
     * and caches the player's current clan UUID.
     */
    suspend fun trackPlayer(uuid: UUID, name: String)

    /**
     * Computes the diff between the current JSON file stats and the stored snapshot.
     * Only returns entries where the diff is > 0.
     * Updates the in-memory snapshot to the current values after computation.
     */
    suspend fun computeDiffs(uuid: UUID, name: String): List<StatEntry>

    /**
     * Stop tracking a player (called on quit).
     * Removes the player from the snapshot and clan maps.
     */
    fun untrackPlayer(uuid: UUID)

    /**
     * Returns whether a player is currently being tracked.
     */
    fun isTracking(uuid: UUID): Boolean

    companion object : StatisticsManagerService by service {
        val INSTANCE get() = service
    }
}
