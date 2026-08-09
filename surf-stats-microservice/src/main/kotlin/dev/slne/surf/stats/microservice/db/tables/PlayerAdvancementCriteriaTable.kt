package dev.slne.surf.stats.microservice.db.tables

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.Table
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.timestamp

object PlayerAdvancementCriteriaTable : Table("player_advancement_criteria") {
    val playerUuid = nativeUuid("player_uuid")
    val advancementName = varchar("advancement_name", 255)
        .transform({ key(it) }, { it.asString() })
        .references(AdvancementsTable.name)

    /**
     * Criterion names are free-form strings, so this stays a plain varchar —
     * `Key.key()` throws on names datapacks are allowed to use.
     */
    val criterionName = varchar("criterion_name", 128)
    val serverName = varchar("server_name", 128).references(ServersTable.name)

    /** `null` when the timestamp in the source file could not be parsed. */
    val awardedAt = timestamp("awarded_at").nullable()

    override val primaryKey = PrimaryKey(playerUuid, advancementName, criterionName, serverName)
}
