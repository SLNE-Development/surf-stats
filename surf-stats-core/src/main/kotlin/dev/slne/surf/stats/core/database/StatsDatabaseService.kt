package dev.slne.surf.stats.core.database

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insertIgnore
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
            }

            // Collect and insert unique categories and keys
            val categories = player.stats.map { it.category }.toSet()
            val keys = player.stats.map { it.key }.toSet()

            for (category in categories) {
                StatCategoriesTable.insertIgnore {
                    it[name] = category
                }
            }

            for (key in keys) {
                StatKeysTable.insertIgnore {
                    it[name] = key
                }
            }

            // Upsert all stat entries
            for (entry in player.stats) {
                PlayerStatsTable.upsert {
                    it[playerUuid] = player.uuid
                    it[categoryName] = entry.category
                    it[statKeyName] = entry.key
                    it[value] = entry.value
                    it[serverName] = batch.serverName
                }
            }
        }

        logger.info(
            "Saved {} stat entries for player {} ({}) on server '{}'",
            player.stats.size, player.name, player.uuid, batch.serverName
        )
    }

    suspend fun saveBatches(batches: List<PlayerStatsBatch>) {
        for (batch in batches) {
            try {
                saveBatch(batch)
            } catch (e: Exception) {
                logger.error(
                    "Failed to save stats for player {} ({}): {}",
                    batch.player.name, batch.player.uuid, e.message
                )
            }
        }
    }
}
