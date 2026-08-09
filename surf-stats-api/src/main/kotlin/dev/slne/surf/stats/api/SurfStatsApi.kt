package dev.slne.surf.stats.api

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.stats.api.model.PlayerAdvancements
import dev.slne.surf.stats.api.model.PlayerStats
import java.util.*

private val api = requiredService<SurfStatsApi>()

interface SurfStatsApi {

    suspend fun processPlayerStats(playerUuid: UUID)

    suspend fun processAllPlayerStats(uuids: Set<UUID>)

    suspend fun getPlayerStats(playerUuid: UUID): PlayerStats

    /**
     * UPSERTs the player's current absolute stats into `player_stats`.
     *
     * Each entry's `value` overwrites the row keyed by
     * `(player_uuid, category_name, stat_key_name, server_name)`.
     */
    suspend fun saveStats(playerUuid: UUID, stats: PlayerStats)

    /**
     * Records an event-log delta for each entry.
     *
     * **Breaking change** (vs. the previous version of this API): callers must
     * now pass the **absolute current values** — the same shape as
     * [saveStats]. The microservice computes
     * `delta = entry.value - last_diff_value` against the per-tuple baseline
     * stored in `player_stats.last_diff_value` and appends a row to
     * `player_stats_history` with `value = delta` whenever `delta > 0`.
     *
     * The first call per `(player, category, key, server)` tuple sees a
     * baseline of 0, so the initial history row carries `entry.value` as
     * its delta. Negative deltas are dropped (no history row) and logged
     * server-side; the baseline is always advanced to the new
     * `entry.value` so subsequent calls operate against the latest known
     * value.
     *
     * Do not pre-compute deltas client-side — sending deltas under this
     * contract will make the baseline drift and corrupt history.
     */
    suspend fun saveDiffStats(playerUuid: UUID, stats: PlayerStats)

    /**
     * Loads the player's current advancement snapshot from disk.
     *
     * Recipe advancements are excluded.
     */
    suspend fun getPlayerAdvancements(playerUuid: UUID): PlayerAdvancements

    /**
     * Replaces everything stored for `(playerUuid, serverName)` with [advancements].
     *
     * The snapshot must be complete — anything missing from it is deleted. An
     * empty snapshot is ignored rather than treated as "the player has nothing",
     * so that a failed read can never wipe stored data.
     *
     * This bypasses the change-detection used by [processPlayerAdvancements] and
     * always sends.
     */
    suspend fun saveAdvancements(playerUuid: UUID, advancements: PlayerAdvancements)

    /**
     * Loads one player's advancements from disk and sends them if they changed
     * since the last successful send.
     */
    suspend fun processPlayerAdvancements(playerUuid: UUID)

    /**
     * Same as [processPlayerAdvancements], batched for many players. Players whose
     * snapshot is unchanged are not sent at all.
     */
    suspend fun processAllPlayerAdvancements(uuids: Set<UUID>)

    companion object : SurfStatsApi by api {
        val INSTANCE get() = api
    }
}
