package dev.slne.surf.stats.microservice.db

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.serializer.adventure.key.SerializableKey
import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ResultRow
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.and
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.inList
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.*
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.stats.api.model.OptOutInfo
import dev.slne.surf.stats.api.model.OptOutType
import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.api.model.PlayerStatsBatch
import dev.slne.surf.stats.core.common.mapping.optOutStatisticMapping
import dev.slne.surf.stats.microservice.db.StatsDatabaseService.saveBatch
import dev.slne.surf.stats.microservice.db.tables.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import java.util.*

object StatsDatabaseService {
    private val log = logger()

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

        StatCategoriesTable.batchInsert(
            data = categories,
            ignore = true,
            shouldReturnGeneratedValues = false,
        ) { category ->
            this[StatCategoriesTable.name] = category
        }

        StatKeysTable.batchInsert(
            data = keys,
            ignore = true,
            shouldReturnGeneratedValues = false,
        ) { key ->
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

            PlayerStatsTable.batchUpsert(
                data = filtered.stats,
                shouldReturnGeneratedValues = false,
                onUpdateExclude = listOf(PlayerStatsTable.lastDiffValue)
            ) { entry ->
                this[PlayerStatsTable.playerUuid] = filtered.playerUuid
                this[PlayerStatsTable.categoryName] = entry.category
                this[PlayerStatsTable.statKeyName] = entry.key
                this[PlayerStatsTable.value] = entry.value
                this[PlayerStatsTable.serverName] = filtered.serverName
                this[PlayerStatsTable.lastDiffValue] = entry.value
            }
        }
    }

    /**
     * Saves diff entries for a single player.
     *
     * Callers send absolute current values; the microservice computes the
     * delta against the per-tuple `last_diff_value` baseline stored in
     * [PlayerStatsTable]. For each entry:
     *
     * - `delta = entry.value - baseline` (baseline defaults to 0 for first-time tuples).
     * - `delta > 0`: append a row to [PlayerStatsHistoryTable] with `value = delta`.
     * - `delta == 0`: no history row; baseline still upserted (no-op).
     * - `delta < 0`: log a warning, no history row, baseline still upserted so future
     *   calls work against the new lower baseline.
     *
     * The baseline upsert sets `last_diff_value = entry.value`. On a row that
     * already exists, only `last_diff_value` is updated — `value` is never touched
     * here (that path is owned by [saveBatch]). On insert, `value = entry.value`
     * is used as a placeholder so the row is valid; [saveBatch] will overwrite
     * it on the next current-stats save if it runs.
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

            val categories = stats.map { it.category }.toSet().toList()
            val keys = stats.map { it.key }.toSet().toList()

            val baselines: Map<Pair<SerializableKey, SerializableKey>, Long> = PlayerStatsTable
                .selectAll()
                .where {
                    (PlayerStatsTable.playerUuid eq filtered.playerUuid) and
                            (PlayerStatsTable.serverName eq filtered.serverName) and
                            (PlayerStatsTable.categoryName inList categories) and
                            (PlayerStatsTable.statKeyName inList keys)
                }
                .toList()
                .associate { row ->
                    val cat = row[PlayerStatsTable.categoryName]
                    val statKey = row[PlayerStatsTable.statKeyName]
                    (cat to statKey) to (row[PlayerStatsTable.lastDiffValue] ?: 0L)
                }

            val deltas = stats.map { entry ->
                val baseline = baselines[entry.category to entry.key] ?: 0L
                val delta = entry.value - baseline
                if (delta < 0) {
                    log.atInfo().log(
                        "Negative delta for player %s on %s for %s/%s: baseline=%d, current=%d, delta=%d",
                        filtered.playerUuid,
                        filtered.serverName,
                        entry.category.asString(),
                        entry.key.asString(),
                        baseline,
                        entry.value,
                        delta
                    )
                }
                DeltaRow(
                    category = entry.category,
                    statKey = entry.key,
                    absoluteValue = entry.value,
                    delta = delta
                )
            }

            val historyRows = deltas.filter { it.delta > 0 }
            if (historyRows.isNotEmpty()) {
                PlayerStatsHistoryTable.batchInsert(
                    data = historyRows,
                    shouldReturnGeneratedValues = false,
                ) { row ->
                    this[PlayerStatsHistoryTable.playerUuid] = filtered.playerUuid
                    this[PlayerStatsHistoryTable.categoryName] = row.category
                    this[PlayerStatsHistoryTable.statKeyName] = row.statKey
                    this[PlayerStatsHistoryTable.value] = row.delta
                    this[PlayerStatsHistoryTable.serverName] = filtered.serverName
                    this[PlayerStatsHistoryTable.clanUuid] = filtered.clanUuid
                }
            }

            PlayerStatsTable.batchUpsert(
                data = deltas,
                shouldReturnGeneratedValues = false,
                onUpdateExclude = listOf(PlayerStatsTable.value)
            ) { row ->
                this[PlayerStatsTable.playerUuid] = filtered.playerUuid
                this[PlayerStatsTable.categoryName] = row.category
                this[PlayerStatsTable.statKeyName] = row.statKey
                this[PlayerStatsTable.value] = row.absoluteValue
                this[PlayerStatsTable.serverName] = filtered.serverName
                this[PlayerStatsTable.lastDiffValue] = row.absoluteValue
            }
        }
    }

    private data class DeltaRow(
        val category: SerializableKey,
        val statKey: SerializableKey,
        val absoluteValue: Long,
        val delta: Long
    )

    /**
     * Saves current player statistic batches.
     *
     * Batches for the same player and server are saved sequentially, while unrelated groups
     * may be processed concurrently.
     *
     * @param batches the current statistic batches to save
     * @return the UUIDs of players for which at least one batch failed to save
     */
    suspend fun saveBatches(
        batches: List<PlayerStatsBatch>
    ): Set<UUID> = saveGroupedBatches(
        batches = batches,
        operationName = "stats",
        save = ::saveBatch
    )

    /**
     * Saves player statistic difference batches.
     *
     * Batches for the same player and server are saved sequentially to ensure that differences
     * are calculated against baselines in the correct order. Unrelated groups may be processed
     * concurrently.
     *
     * @param batches the statistic difference batches to save
     * @return the UUIDs of players for which at least one batch failed to save
     */
    suspend fun saveDiffBatches(
        batches: List<PlayerStatsBatch>
    ): Set<UUID> = saveGroupedBatches(
        batches = batches,
        operationName = "diff stats",
        save = ::saveDiffBatch
    )

    /**
     * Saves player statistic batches while preserving their order per player and server.
     *
     * Batches belonging to the same player and server are processed sequentially to prevent
     * older values from overwriting newer baselines. Different player-server groups are
     * processed concurrently up to the configured [concurrency] limit.
     *
     * @param batches the statistic batches to save
     * @param operationName the operation name used in failure log messages
     * @param concurrency the maximum number of player-server groups processed concurrently
     * @param save the suspending operation used to persist a single batch
     * @return the UUIDs of players for which at least one batch failed to save
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private suspend fun saveGroupedBatches(
        batches: List<PlayerStatsBatch>,
        operationName: String,
        concurrency: Int = DEFAULT_CONCURRENCY,
        save: suspend (PlayerStatsBatch) -> Unit
    ): Set<UUID> {
        return batches
            .groupBy { it.playerUuid to it.stats.serverName }
            .values
            .asFlow()
            .flatMapMerge(concurrency = concurrency) { playerBatches ->
                flow {
                    for (batch in playerBatches) {
                        val failed = runCatching {
                            save(batch)
                        }.onFailure { exception ->
                            log.atSevere()
                                .withCause(exception)
                                .log(
                                    "Failed to save $operationName for player ${batch.playerUuid}"
                                )
                        }.isFailure

                        if (failed) {
                            emit(batch.playerUuid)
                        }
                    }
                }
            }
            .toSet()
    }
}