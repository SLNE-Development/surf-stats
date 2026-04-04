package dev.slne.surf.stats.microservice.db

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.serializer.adventure.key.SerializableKey
import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ResultRow
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.and
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.*
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.stats.api.model.OptOutInfo
import dev.slne.surf.stats.api.model.OptOutType
import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.api.model.PlayerStatsBatch
import dev.slne.surf.stats.core.common.mapping.optOutStatisticMapping
import dev.slne.surf.stats.microservice.db.tables.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
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

    suspend fun toggleOptOutStatistic(
        playerUuid: SerializableUUID,
        categoryName: String,
        statisticName: String,
        type: OptOutType
    ) = suspendTransaction {
        val mapping =
            optOutStatisticMapping.find { it.categoryName == categoryName && it.statisticName == statisticName }
        val items = mapping?.items ?: listOf(statisticName)

        val categoryNameKey = key(categoryName)
        items.forEach { item ->
            val statisticNameKey = key(item)

            if (type == OptOutType.OUT) {
                PlayerStatOptOuts.insertIgnore {
                    it[this.playerUuid] = playerUuid
                    it[this.categoryName] = categoryNameKey
                    it[this.statKeyName] = statisticNameKey
                }
            } else if (type == OptOutType.IN) {
                PlayerStatOptOuts.deleteWhere {
                    (PlayerStatOptOuts.playerUuid eq playerUuid) and
                            (PlayerStatOptOuts.categoryName eq categoryNameKey) and
                            (PlayerStatOptOuts.statKeyName eq statisticNameKey)
                }
            }
        }
    }

    suspend fun getOptOut(
        playerUuid: SerializableUUID
    ) = suspendTransaction {
        PlayerStatOptOuts.selectAll().where { PlayerStatOptOuts.playerUuid eq playerUuid }
            .map { it.toOptOutInfo() }
            .toList()
    }

    private fun ResultRow.toOptOutInfo(): OptOutInfo {
        return OptOutInfo(
            playerUuid = this[PlayerStatOptOuts.playerUuid],
            categoryName = this[PlayerStatOptOuts.categoryName],
            statisticName = this[PlayerStatOptOuts.statKeyName],
            type = OptOutType.OUT
        )
    }

    private suspend fun getOptOutKeys(
        playerUuid: SerializableUUID
    ): Set<Pair<SerializableKey, SerializableKey>> = suspendTransaction {
        PlayerStatOptOuts.selectAll().where { PlayerStatOptOuts.playerUuid eq playerUuid }
            .map { row -> row[PlayerStatOptOuts.categoryName] to row[PlayerStatOptOuts.statKeyName] }
            .toSet()
    }

    private fun PlayerStatsBatch.filterOptedOut(
        optOutKeys: Set<Pair<SerializableKey, SerializableKey>>
    ): PlayerStatsBatch {
        if (optOutKeys.isEmpty()) return this
        val filteredStats = stats.stats.filter { entry ->
            (entry.category to entry.key) !in optOutKeys
        }
        return copy(stats = PlayerStats(stats.playerUuid, stats.serverName, filteredStats))
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
        val optOutKeys = getOptOutKeys(batch.playerUuid)
        val filtered = batch.filterOptedOut(optOutKeys)

        if (filtered.stats.isEmpty()) return

        suspendTransaction {
            ensureDimensions(filtered.stats)

            PlayerStatsTable.batchUpsert(filtered.stats) { entry ->
                this[PlayerStatsTable.playerUuid] = filtered.playerUuid
                this[PlayerStatsTable.categoryName] = entry.category
                this[PlayerStatsTable.statKeyName] = entry.key
                this[PlayerStatsTable.value] = entry.value
                this[PlayerStatsTable.serverName] = filtered.serverName
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
        val optOutKeys = getOptOutKeys(batch.playerUuid)
        val filtered = batch.filterOptedOut(optOutKeys)
        val stats = filtered.stats

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
                this[PlayerStatsHistoryTable.clanUuid] = filtered.clanUuid
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