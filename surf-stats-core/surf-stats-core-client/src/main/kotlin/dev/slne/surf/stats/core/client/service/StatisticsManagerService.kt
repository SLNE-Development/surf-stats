package dev.slne.surf.stats.core.client.service

import dev.slne.surf.stats.api.model.PlayerStats
import org.jetbrains.annotations.UnmodifiableView
import java.util.*

interface StatisticsManagerService {
    val snapshotMap: @UnmodifiableView List<PlayerStats>

    suspend fun computeDiffs(uuid: UUID): PlayerStats

    suspend fun updateSnapshot(uuid: UUID)

    suspend fun trackPlayer(uuid: UUID)
    fun untrackPlayer(uuid: UUID)

    fun isTracking(uuid: UUID): Boolean

    companion object : StatisticsManagerService by StatisticsManagerServiceImpl
}
