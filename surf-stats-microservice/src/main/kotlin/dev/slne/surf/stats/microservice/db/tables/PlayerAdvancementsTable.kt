package dev.slne.surf.stats.microservice.db.tables

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.Table
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.timestamp

object PlayerAdvancementsTable : Table("player_advancements") {
    val playerUuid = nativeUuid("player_uuid")
    val advancementName = varchar("advancement_name", 255)
        .transform({ key(it) }, { it.asString() })
        .references(AdvancementsTable.name)
    val serverName = varchar("server_name", 128).references(ServersTable.name)
    val done = bool("done").default(false)

    /** Latest criterion timestamp once the advancement is complete, else `null`. */
    val completedAt = timestamp("completed_at").nullable()

    /** Number of awarded criteria. The total is not available from the source file. */
    val criteriaDone = integer("criteria_done").default(0)

    /**
     * When this player's snapshot last changed. Snapshot-scoped, not row-scoped:
     * the whole snapshot is rewritten as a unit, so every row of a player shares
     * this value. The per-advancement timestamp is [completedAt].
     */
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(playerUuid, advancementName, serverName)

    init {
        index("idx_player_advancements_player_done", false, playerUuid, done)
        index(
            "idx_player_advancements_advancement_done",
            false,
            advancementName,
            done,
            completedAt
        )
    }
}
