package dev.slne.surf.stats.core.client.service

import dev.slne.surf.stats.api.model.PlayerAdvancements
import java.util.*

/**
 * Suppresses advancement sends for players whose snapshot has not changed since
 * the last successful send.
 *
 * Advancement snapshots are shipped in full and most players earn nothing
 * between two world saves, so without this guard the majority of sends would
 * rewrite identical rows.
 *
 * State is in-memory only — after a plugin restart the first snapshot per player
 * always counts as changed.
 */
interface AdvancementSyncStateService {
    /**
     * Returns the snapshots whose content differs from the last state marked
     * synced for that player.
     */
    fun selectChanged(snapshots: List<PlayerAdvancements>): List<PlayerAdvancements>

    /**
     * Records [sent] as the last successfully synced state, skipping players
     * listed in [failed] so the next trigger retries them.
     */
    fun markSynced(sent: List<PlayerAdvancements>, failed: Set<UUID>)

    /**
     * Drops all state for [playerUuid], for example when the player quits.
     */
    fun forget(playerUuid: UUID)

    companion object : AdvancementSyncStateService by AdvancementSyncStateServiceImpl
}
