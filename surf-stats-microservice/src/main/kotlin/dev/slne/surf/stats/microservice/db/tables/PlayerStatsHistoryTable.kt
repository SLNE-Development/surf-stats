package dev.slne.surf.stats.microservice.db.tables

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.database.columns.charUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.Table
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.timestamp

object PlayerStatsHistoryTable : Table("player_stats_history") {
    val id = long("id").autoIncrement()
    val playerUuid = charUuid("player_uuid").references(PlayersTable.uuid)
    val categoryName = varchar("category_name", 128)
        .transform({ key(it) }, { it.asString() })
        .references(StatCategoriesTable.name)
    val statKeyName = varchar("stat_key_name", 128)
        .transform({ key(it) }, { it.asString() })
        .references(StatKeysTable.name)
    val value = long("value")
    val serverName = varchar("server_name", 128).references(ServersTable.name)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val clanUuid = charUuid("clan_uuid").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(
            "uq_player_stat_snapshot",
            playerUuid,
            categoryName,
            statKeyName,
            serverName,
            createdAt
        )
        index(
            "idx_stats_player_category_stat",
            false,
            playerUuid,
            categoryName,
            statKeyName,
            createdAt
        )
        index("idx_stats_clan_created_at", false, clanUuid, createdAt)
        index("idx_stats_stat_key_created_at", false, statKeyName, createdAt)
    }
}
