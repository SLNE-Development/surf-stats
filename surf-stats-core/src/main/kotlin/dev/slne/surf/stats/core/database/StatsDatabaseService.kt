package dev.slne.surf.stats.core.database

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.batchInsert
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.batchUpsert
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.upsert
import dev.slne.surf.stats.api.StatsInstance
import dev.slne.surf.stats.api.model.PlayerStatsBatch
import dev.slne.surf.stats.api.model.StatEntry
import dev.slne.surf.stats.core.database.table.*
import dev.slne.surf.surfapi.core.api.util.logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.*

object StatsDatabaseService {
    private val log = logger()

    private suspend fun ensurePlayerAndDimensions(
        playerUuid: UUID,
        playerName: String,
        dataVersion: Int,
        stats: Collection<StatEntry>,
        excludeDataVersionOnUpdate: Boolean = false
    ) {
        PlayersTable.upsert(
            onUpdateExclude = if (excludeDataVersionOnUpdate) listOf(PlayersTable.dataVersion) else emptyList()
        ) {
            it[uuid] = playerUuid
            it[name] = playerName
            it[PlayersTable.dataVersion] = dataVersion
            it[updatedAt] = CurrentTimestamp
        }

        val categories = stats.map { it.category }.toSet()
        val keys = stats.map { it.key }.toSet()

        StatCategoriesTable.batchInsert(categories, ignore = true) { category ->
            this[StatCategoriesTable.name] = category
        }

        StatKeysTable.batchInsert(keys, ignore = true) { key ->
            this[StatKeysTable.name] = key
        }
    }

    suspend fun registerServer() = suspendTransaction {
        ServersTable.upsert {
            it[name] = StatsInstance.serverName
            it[label] = StatsInstance.serverDisplayName
        }
    }

    suspend fun saveBatch(batch: PlayerStatsBatch) {
        val player = batch.player

        suspendTransaction {
            ensurePlayerAndDimensions(player.uuid, player.name, player.dataVersion, player.stats)

            PlayerStatsTable.batchUpsert(player.stats) { entry ->
                this[PlayerStatsTable.playerUuid] = player.uuid
                this[PlayerStatsTable.categoryName] = entry.category
                this[PlayerStatsTable.statKeyName] = entry.key
                this[PlayerStatsTable.value] = entry.value
                this[PlayerStatsTable.serverName] = batch.serverName
            }
        }
    }

    /**
     * Saves diff entries for a single player to the database.
     * Each entry is INSERTed as a new row (not upserted), since diff entries
     * are append-only with a timestamp.
     */
    suspend fun saveDiffBatch(
        playerUuid: UUID,
        playerName: String,
        dataVersion: Int,
        diffs: List<StatEntry>,
        clanUuid: UUID?
    ) {
        if (diffs.isEmpty()) {
            return
        }

        suspendTransaction {
            ensurePlayerAndDimensions(playerUuid, playerName, dataVersion, diffs)

            // INSERT (not upsert) - each diff is a new row with its own timestamp
            PlayerStatsHistoryTable.batchInsert(diffs) { entry ->
                this[PlayerStatsHistoryTable.playerUuid] = playerUuid
                this[PlayerStatsHistoryTable.categoryName] = entry.category
                this[PlayerStatsHistoryTable.statKeyName] = entry.key
                this[PlayerStatsHistoryTable.value] = entry.value
                this[PlayerStatsHistoryTable.serverName] = StatsInstance.serverName
                this[PlayerStatsHistoryTable.clanUuid] = clanUuid
            }
        }
    }

    /**
     * Saves multiple actual stat batches in parallel.
     * Returns the set of player UUIDs that failed to save.
     */
    suspend fun saveBatches(batches: List<PlayerStatsBatch>): Set<UUID> = coroutineScope {
        batches.map { batch ->
            async {
                runCatching {
                    saveBatch(batch)
                }.onFailure { e ->
                    log.atSevere().log(
                        "Failed to save stats for player {} ({}): {}",
                        batch.player.name, batch.player.uuid, e.message
                    )
                }.let { result ->
                    if (result.isFailure) batch.player.uuid else null
                }
            }
        }.awaitAll().filterNotNull().toSet()
    }

    /**
     * Saves multiple diff batches in parallel.
     * Returns the set of player UUIDs that failed to save.
     */
    suspend fun saveDiffBatches(
        batches: List<PlayerStatsBatch>
    ): Set<UUID> = coroutineScope {
        batches.map { batch ->
            async {
                val diffs = batch.player.stats
                runCatching {
                    saveDiffBatch(
                        playerUuid = batch.player.uuid,
                        playerName = batch.player.name,
                        dataVersion = batch.player.dataVersion,
                        diffs = diffs,
                        clanUuid = batch.clanUuid
                    )
                }.onFailure { e ->
                    log.atSevere().log(
                        "Failed to save diff stats for player {} ({}): {}",
                        batch.player.name, batch.player.uuid, e.message
                    )
                }.let { result ->
                    if (result.isFailure) batch.player.uuid else null
                }
            }
        }.awaitAll().filterNotNull().toSet()
    }

    suspend fun saveCustomStats(
        playerUuid: UUID,
        playerName: String,
        stats: Map<String, Long>
    ) {
        val category = "minecraft:custom"
        val statEntries = stats.map { (key, value) -> StatEntry(category, key, value) }

        suspendTransaction {
            ensurePlayerAndDimensions(playerUuid, playerName, 0, statEntries, excludeDataVersionOnUpdate = true)

            // Custom stats are also inserted as diff entries
            PlayerStatsTable.batchInsert(statEntries) { entry ->
                this[PlayerStatsTable.playerUuid] = playerUuid
                this[PlayerStatsTable.categoryName] = entry.category
                this[PlayerStatsTable.statKeyName] = entry.key
                this[PlayerStatsTable.value] = entry.value
                this[PlayerStatsTable.serverName] = StatsInstance.serverName
            }
        }
    }
}
