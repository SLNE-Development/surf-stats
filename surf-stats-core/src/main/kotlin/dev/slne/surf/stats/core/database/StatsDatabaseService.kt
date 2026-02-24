package dev.slne.surf.stats.core.database

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.batchInsert
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.batchUpsert
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.upsert
import dev.slne.surf.stats.api.model.PlayerStatsBatch
import dev.slne.surf.stats.core.database.table.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.util.UUID

class StatsDatabaseService(
    private val serverName: String,
    private val serverLabel: String
) {

    private val logger = LoggerFactory.getLogger(StatsDatabaseService::class.java)

    suspend fun registerServer() {
        suspendTransaction {
            ServersTable.upsert {
                it[name] = serverName
                it[label] = serverLabel
            }
        }
    }

    suspend fun saveBatch(batch: PlayerStatsBatch) {
        val player = batch.player

        suspendTransaction {
            // Upsert player
            PlayersTable.upsert {
                it[uuid] = player.uuid
                it[name] = player.name
                it[dataVersion] = player.dataVersion
                it[updatedAt] = CurrentTimestamp
            }

            // Use batch operations instead of individual inserts/upserts for performance.
            // With 20 players and ~200 entries each, this reduces ~4000 individual queries
            // to ~20 batch statements.
            val categories = player.stats.map { it.category }.toSet()
            val keys = player.stats.map { it.key }.toSet()

            StatCategoriesTable.batchInsert(categories, ignore = true) { category ->
                this[StatCategoriesTable.name] = category
            }

            StatKeysTable.batchInsert(keys, ignore = true) { key ->
                this[StatKeysTable.name] = key
            }

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
     * Saves multiple batches in parallel, each in its own transaction.
     * A failure for one player does not affect the others.
     * Returns the number of batches that failed to save.
     */
    suspend fun saveBatches(batches: List<PlayerStatsBatch>): Int = coroutineScope {
        batches.map { batch ->
            async {
                runCatching { saveBatch(batch) }
                    .onFailure { e ->
                        logger.error(
                            "Failed to save stats for player {} ({}): {}",
                            batch.player.name, batch.player.uuid, e.message
                        )
                    }
                    .isFailure
            }
        }.awaitAll().count { it }
    }

    suspend fun saveCustomStats(
        playerUuid: UUID,
        playerName: String,
        stats: Map<String, Long>
    ) {
        val category = "minecraft:custom"

        suspendTransaction {
            PlayersTable.upsert(onUpdateExclude = listOf(PlayersTable.dataVersion)) {
                it[uuid] = playerUuid
                it[name] = playerName
                it[dataVersion] = 0
                it[updatedAt] = CurrentTimestamp
            }

            StatCategoriesTable.batchInsert(listOf(category), ignore = true) { cat ->
                this[StatCategoriesTable.name] = cat
            }

            StatKeysTable.batchInsert(stats.keys, ignore = true) { key ->
                this[StatKeysTable.name] = key
            }

            PlayerStatsTable.batchUpsert(stats.entries.toList()) { (key, value) ->
                this[PlayerStatsTable.playerUuid] = playerUuid
                this[PlayerStatsTable.categoryName] = category
                this[PlayerStatsTable.statKeyName] = key
                this[PlayerStatsTable.value] = value
                this[PlayerStatsTable.serverName] = serverName
            }
        }
    }
}
