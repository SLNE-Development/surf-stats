package dev.slne.surf.stats.microservice.db

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.and
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.*
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.stats.api.model.PlayerAdvancements
import dev.slne.surf.stats.microservice.db.tables.AdvancementsTable
import dev.slne.surf.stats.microservice.db.tables.PlayerAdvancementCriteriaTable
import dev.slne.surf.stats.microservice.db.tables.PlayerAdvancementsTable
import java.util.*

/**
 * Persists player advancement snapshots.
 *
 * A snapshot is always complete, so it is written by replacement: everything
 * stored for `(player, server)` is deleted and re-inserted inside one
 * transaction. Advancements can disappear — `/advancement revoke`, a removed
 * datapack, a reset world — and an upsert-only strategy would leave those rows
 * behind forever. Since no history is kept, replacement is both simpler than a
 * `NOT IN` diff and portable across dialects.
 */
object AdvancementsDatabaseService {

    /**
     * Saves advancement snapshots.
     *
     * Snapshots for the same player and server are saved sequentially; unrelated
     * groups may be processed concurrently.
     *
     * @return the UUIDs of players whose snapshot failed to save
     */
    suspend fun saveSnapshots(snapshots: List<PlayerAdvancements>): Set<UUID> = saveGrouped(
        items = snapshots,
        operationName = "advancements",
        playerUuidOf = { it.playerUuid },
        groupKeyOf = { it.playerUuid to it.serverName },
        save = ::saveSnapshot
    )

    private suspend fun saveSnapshot(snapshot: PlayerAdvancements) {
        // An empty snapshot means the file could not be read. Replacing with
        // nothing would delete the player's stored advancements.
        if (snapshot.isEmpty()) {
            return
        }

        suspendTransaction {
            AdvancementsTable.batchInsert(
                data = snapshot.map { it.advancement }.toSet(),
                ignore = true,
                shouldReturnGeneratedValues = false,
            ) { advancement ->
                this[AdvancementsTable.name] = advancement
            }

            PlayerAdvancementCriteriaTable.deleteWhere {
                (PlayerAdvancementCriteriaTable.playerUuid eq snapshot.playerUuid) and
                        (PlayerAdvancementCriteriaTable.serverName eq snapshot.serverName)
            }

            PlayerAdvancementsTable.deleteWhere {
                (PlayerAdvancementsTable.playerUuid eq snapshot.playerUuid) and
                        (PlayerAdvancementsTable.serverName eq snapshot.serverName)
            }

            PlayerAdvancementsTable.batchInsert(
                data = snapshot.advancements,
                shouldReturnGeneratedValues = false,
            ) { entry ->
                this[PlayerAdvancementsTable.playerUuid] = snapshot.playerUuid
                this[PlayerAdvancementsTable.advancementName] = entry.advancement
                this[PlayerAdvancementsTable.serverName] = snapshot.serverName
                this[PlayerAdvancementsTable.done] = entry.done
                this[PlayerAdvancementsTable.completedAt] = entry.completedAt
                this[PlayerAdvancementsTable.criteriaDone] = entry.criteriaDone
            }

            val criteriaRows = snapshot.advancements.flatMap { entry ->
                entry.criteria.map { criterion -> entry.advancement to criterion }
            }

            if (criteriaRows.isNotEmpty()) {
                PlayerAdvancementCriteriaTable.batchInsert(
                    data = criteriaRows,
                    shouldReturnGeneratedValues = false,
                ) { (advancement, criterion) ->
                    this[PlayerAdvancementCriteriaTable.playerUuid] = snapshot.playerUuid
                    this[PlayerAdvancementCriteriaTable.advancementName] = advancement
                    this[PlayerAdvancementCriteriaTable.criterionName] = criterion.name
                    this[PlayerAdvancementCriteriaTable.serverName] = snapshot.serverName
                    this[PlayerAdvancementCriteriaTable.awardedAt] = criterion.awardedAt
                }
            }
        }
    }
}
