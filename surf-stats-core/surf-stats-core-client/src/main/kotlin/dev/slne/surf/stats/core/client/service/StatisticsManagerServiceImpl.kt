package dev.slne.surf.stats.core.client.service

import java.util.*
import java.util.concurrent.ConcurrentHashMap

object StatisticsManagerServiceImpl : StatisticsManagerService {
    private val _trackedPlayers: MutableSet<UUID> =
        Collections.newSetFromMap(ConcurrentHashMap())

    override val trackedPlayers: Set<UUID>
        get() = Collections.unmodifiableSet(_trackedPlayers)

    override suspend fun trackPlayer(uuid: UUID) {
        _trackedPlayers.add(uuid)
    }

    override fun untrackPlayer(uuid: UUID) {
        _trackedPlayers.remove(uuid)
    }

    override fun isTracking(uuid: UUID): Boolean {
        return uuid in _trackedPlayers
    }
}
