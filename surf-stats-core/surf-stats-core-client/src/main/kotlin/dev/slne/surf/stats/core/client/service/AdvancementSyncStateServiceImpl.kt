package dev.slne.surf.stats.core.client.service

import dev.slne.surf.stats.api.model.PlayerAdvancements
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object AdvancementSyncStateServiceImpl : AdvancementSyncStateService {
    private val lastSyncedHashes = ConcurrentHashMap<UUID, Int>()

    override fun selectChanged(snapshots: List<PlayerAdvancements>): List<PlayerAdvancements> =
        snapshots.filter { snapshot ->
            lastSyncedHashes[snapshot.playerUuid] != hashOf(snapshot)
        }

    override fun markSynced(sent: List<PlayerAdvancements>, failed: Set<UUID>) {
        sent.forEach { snapshot ->
            if (snapshot.playerUuid in failed) {
                lastSyncedHashes.remove(snapshot.playerUuid)
            } else {
                lastSyncedHashes[snapshot.playerUuid] = hashOf(snapshot)
            }
        }
    }

    override fun forget(playerUuid: UUID) {
        lastSyncedHashes.remove(playerUuid)
    }

    /**
     * Order-independent content hash of [snapshot].
     */
    private fun hashOf(snapshot: PlayerAdvancements): Int {
        var result = snapshot.serverName.hashCode()

        snapshot.advancements
            .sortedBy { it.advancement.asString() }
            .forEach { entry ->
                result = 31 * result + entry.advancement.asString().hashCode()
                result = 31 * result + entry.done.hashCode()

                entry.criteria
                    .sortedBy { it.name }
                    .forEach { criterion ->
                        result = 31 * result + criterion.name.hashCode()
                        result = 31 * result + (criterion.awardedAt?.hashCode() ?: 0)
                    }
            }

        return result
    }
}
