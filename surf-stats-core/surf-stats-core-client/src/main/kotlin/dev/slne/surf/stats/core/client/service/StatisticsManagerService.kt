package dev.slne.surf.stats.core.client.service

import org.jetbrains.annotations.UnmodifiableView
import java.util.*

/**
 * Tracks which players are currently being followed for periodic stat saves.
 *
 * Diff computation lives on the microservice — clients no longer maintain
 * per-player snapshots. This service only records the set of players whose
 * stats should be flushed and shipped (as absolute values) on the periodic
 * save cycle, on quit, and on shutdown.
 */
interface StatisticsManagerService {
    val trackedPlayers: @UnmodifiableView Set<UUID>

    suspend fun trackPlayer(uuid: UUID)
    fun untrackPlayer(uuid: UUID)

    fun isTracking(uuid: UUID): Boolean

    companion object : StatisticsManagerService by StatisticsManagerServiceImpl
}
