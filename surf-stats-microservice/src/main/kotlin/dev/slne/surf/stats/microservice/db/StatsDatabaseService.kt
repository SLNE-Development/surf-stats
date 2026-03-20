package dev.slne.surf.stats.microservice.db

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.batchInsert
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.batchUpsert
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.upsert
import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.api.model.PlayerStatsBatch
import dev.slne.surf.stats.microservice.db.tables.*
import dev.slne.surf.surfapi.core.api.util.logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.*

object StatsDatabaseService {
    private val log = logger()

    suspend fun ensurePlayer(playerUuid: UUID, playerName: String) = suspendTransaction {
        PlayersTable.upsert {
            it[uuid] = playerUuid
            it[name] = playerName
            it[dataVersion] = 0
            it[updatedAt] = CurrentTimestamp
        }
    }

    private suspend fun ensureDimensions(
        stats: PlayerStats,
    ) = suspendTransaction {
        val categories = stats.map { it.category }.toSet()
        val keys = stats.map { it.key }.toSet()

        StatCategoriesTable.batchInsert(categories, ignore = true) { category ->
            this[StatCategoriesTable.name] = category
        }

        StatKeysTable.batchInsert(keys, ignore = true) { key ->
            this[StatKeysTable.name] = key
        }
    }

    suspend fun ensureServer(
        serverName: String,
        serverDisplayName: String
    ) = suspendTransaction {
        ServersTable.upsert {
            it[name] = serverName
            it[label] = serverDisplayName
        }
    }

    private suspend fun saveBatch(batch: PlayerStatsBatch) {
        suspendTransaction {
            ensureDimensions(batch.stats)

            PlayerStatsTable.batchUpsert(batch.stats) { entry ->
                this[PlayerStatsTable.playerUuid] = batch.playerUuid
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
    private suspend fun saveDiffBatch(
        batch: PlayerStatsBatch,
    ) {
        val stats = batch.stats
        if (stats.isEmpty()) {
            return
        }

        suspendTransaction {
            ensureDimensions(stats)

            // INSERT (not upsert) - each diff is a new row with its own timestamp
            PlayerStatsHistoryTable.batchInsert(stats) { entry ->
                this[PlayerStatsHistoryTable.playerUuid] = stats.playerUuid
                this[PlayerStatsHistoryTable.categoryName] = entry.category
                this[PlayerStatsHistoryTable.statKeyName] = entry.key
                this[PlayerStatsHistoryTable.value] = entry.value
                this[PlayerStatsHistoryTable.serverName] = stats.serverName
                this[PlayerStatsHistoryTable.clanUuid] = batch.clanUuid
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
                    log.atSevere()
                        .withCause(e)
                        .log("Failed to save stats for player ${batch.playerUuid}")
                }.let { result ->
                    if (result.isFailure) batch.playerUuid else null
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
                runCatching {
                    saveDiffBatch(batch)
                }.onFailure { e ->
                    log.atSevere()
                        .withCause(e)
                        .log("Failed to save diff stats for player ${batch.playerUuid}")
                }.let { result ->
                    if (result.isFailure) {
                        batch.playerUuid
                    } else null
                }
            }
        }.awaitAll().filterNotNull().toSet()
    }
}