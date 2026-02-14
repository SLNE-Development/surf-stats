package dev.slne.surf.stats.core.database

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.batchInsert
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.batchUpsert
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.upsert
import dev.slne.surf.stats.api.model.PlayerStatsBatch
import dev.slne.surf.stats.core.database.table.*
import org.slf4j.LoggerFactory

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
        logger.info("Registered server '{}' in database", serverName)
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

        logger.info(
            "Saved {} stat entries for player {} ({}) on server '{}'",
            player.stats.size, player.name, player.uuid, batch.serverName
        )
    }

    /**
     * Saves multiple batches, wrapping all operations in a single transaction.
     * Returns the number of batches that failed to save.
     */
    suspend fun saveBatches(batches: List<PlayerStatsBatch>): Int {
        var failedCount = 0

        suspendTransaction {
            // Collect all unique categories and keys across all batches
            val allCategories = batches.flatMap { it.player.stats.map { s -> s.category } }.toSet()
            val allKeys = batches.flatMap { it.player.stats.map { s -> s.key } }.toSet()

            StatCategoriesTable.batchInsert(allCategories, ignore = true) { category ->
                this[StatCategoriesTable.name] = category
            }

            StatKeysTable.batchInsert(allKeys, ignore = true) { key ->
                this[StatKeysTable.name] = key
            }

            for (batch in batches) {
                try {
                    val player = batch.player

                    PlayersTable.upsert {
                        it[uuid] = player.uuid
                        it[name] = player.name
                        it[dataVersion] = player.dataVersion
                        it[updatedAt] = CurrentTimestamp
                    }

                    PlayerStatsTable.batchUpsert(player.stats) { entry ->
                        this[PlayerStatsTable.playerUuid] = player.uuid
                        this[PlayerStatsTable.categoryName] = entry.category
                        this[PlayerStatsTable.statKeyName] = entry.key
                        this[PlayerStatsTable.value] = entry.value
                        this[PlayerStatsTable.serverName] = batch.serverName
                    }

                    logger.info(
                        "Saved {} stat entries for player {} ({}) on server '{}'",
                        player.stats.size, player.name, player.uuid, batch.serverName
                    )
                } catch (e: Exception) {
                    failedCount++
                    logger.error(
                        "Failed to save stats for player {} ({}): {}",
                        batch.player.name, batch.player.uuid, e.message
                    )
                }
            }
        }

        return failedCount
    }
}
